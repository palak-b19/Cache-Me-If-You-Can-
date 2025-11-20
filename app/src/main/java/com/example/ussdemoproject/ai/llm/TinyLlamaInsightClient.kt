package com.example.ussdemoproject.ai.llm

import ai.onnxruntime.genai.GenAIException
import ai.onnxruntime.genai.Generator
import ai.onnxruntime.genai.GeneratorParams
import ai.onnxruntime.genai.Model
import ai.onnxruntime.genai.Sequences
import ai.onnxruntime.genai.Tokenizer
import android.content.Context
import android.content.res.AssetManager
import android.os.Build
import com.example.ussdemoproject.R
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.io.use
import org.json.JSONArray
import org.json.JSONObject

private const val MODEL_ASSET_DIRECTORY = "models/tinyllama"
private const val MODEL_CACHE_DIRECTORY = "tinyllama-int4"
private val REQUIRED_MODEL_ASSETS = listOf(
    "genai_config.json",
    "model.onnx",
    "model.onnx.data",
    "tokenizer.json",
    "tokenizer_config.json",
    "special_tokens_map.json"
)
private const val MAX_GENERATED_CHAR_COUNT = 1500

/**
 * Hosts TinyLlama via ONNX Runtime GenAI by copying the bundled assets onto disk
 * and running the lightweight Java bindings for prompt/response generation.
 */
class TinyLlamaInsightClient(private val context: Context) {

    private val appContext = context.applicationContext
    private val assetManager: AssetManager = appContext.assets
    private val promptBuilder = TinyLlamaPromptBuilder()

    @Volatile
    private var model: Model? = null
    @Volatile
    private var tokenizer: Tokenizer? = null

    @Volatile
    private var lastIssue: String? = appContext.getString(R.string.tinyllama_runtime_unimplemented)

    fun lastKnownIssue(): String? = lastIssue

    fun isRuntimeReady(): Boolean = ensureRuntimeReady()

    fun generateInsight(
        appName: String,
        packageName: String,
        permissions: List<String>
    ): LlmInsightPayload? {
        if (!isCompatibleDevice()) {
            lastIssue = appContext.getString(R.string.tinyllama_unsupported_device)
            return null
        }
        if (!ensureRuntimeReady()) {
            return null
        }

        val prompt = promptBuilder.buildPrompt(appName, packageName, permissions)
        return try {
            runModel(prompt)
        } catch (t: Throwable) {
            lastIssue = t.message ?: appContext.getString(R.string.tinyllama_runtime_error)
            null
        }
    }

    private fun ensureRuntimeReady(): Boolean {
        if (!isCompatibleDevice()) {
            lastIssue = appContext.getString(R.string.tinyllama_unsupported_device)
            return false
        }
        if (model != null && tokenizer != null) {
            return true
        }

        return synchronized(this) {
            if (model != null && tokenizer != null) {
                return@synchronized true
            }

            val modelDir = materializeAssets() ?: return@synchronized false

            return try {
                val loadedModel = Model(modelDir.absolutePath)
                val loadedTokenizer = Tokenizer(loadedModel)
                model = loadedModel
                tokenizer = loadedTokenizer
                lastIssue = null
                true
            } catch (ex: Exception) {
                lastIssue = ex.message ?: appContext.getString(R.string.tinyllama_runtime_error)
                closeRuntime()
                false
            }
        }
    }

    private fun isCompatibleDevice(): Boolean {
        val isEmulator = Build.FINGERPRINT.lowercase(Locale.US).contains("generic") ||
            Build.FINGERPRINT.lowercase(Locale.US).contains("emulator") ||
            Build.HARDWARE.contains("goldfish", ignoreCase = true) ||
            Build.HARDWARE.contains("ranchu", ignoreCase = true)
        val hasArm64Abi = Build.SUPPORTED_ABIS.any { abi ->
            abi.equals("arm64-v8a", ignoreCase = true)
        }
        return hasArm64Abi && !isEmulator
    }

    private fun materializeAssets(): File? {
        val modelDir = File(appContext.filesDir, MODEL_CACHE_DIRECTORY)
        if (!modelDir.exists() && !modelDir.mkdirs()) {
            lastIssue = appContext.getString(R.string.tinyllama_model_missing)
            return null
        }

        REQUIRED_MODEL_ASSETS.forEach { assetName ->
            val assetPath = locateAsset(assetName) ?: run {
                lastIssue = when {
                    assetName.endsWith(".onnx") -> appContext.getString(R.string.tinyllama_model_missing)
                    assetName.contains("tokenizer") -> appContext.getString(R.string.tinyllama_tokenizer_missing)
                    assetName.contains("genai_config") -> appContext.getString(R.string.tinyllama_config_missing)
                    else -> appContext.getString(R.string.tinyllama_asset_missing, assetName)
                }
                return null
            }

            val destination = File(modelDir, assetName)
            if (destination.exists() && destination.length() > 0L) {
                return@forEach
            }

            if (!copyAsset(assetPath, destination)) {
                lastIssue = appContext.getString(R.string.tinyllama_asset_copy_failed, assetName)
                return null
            }
        }

        return modelDir
    }

    private fun copyAsset(assetPath: String, destination: File): Boolean {
        return try {
            assetManager.open(assetPath).use { input ->
                FileOutputStream(destination).use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun locateAsset(assetName: String): String? {
        val preferred = "$MODEL_ASSET_DIRECTORY/$assetName"
        if (assetExists(preferred)) return preferred
        val legacy = "models/$assetName"
        if (assetExists(legacy)) return legacy
        return null
    }

    private fun assetExists(path: String): Boolean {
        return try {
            assetManager.open(path).close()
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun closeRuntime() {
        try {
            tokenizer?.close()
        } catch (_: Exception) {
        } finally {
            tokenizer = null
        }

        try {
            model?.close()
        } catch (_: Exception) {
        } finally {
            model = null
        }
    }

    private fun runModel(prompt: TinyLlamaPrompt): LlmInsightPayload? {
        val activeModel = model ?: return null
        val activeTokenizer = tokenizer ?: return null

        return try {
            GeneratorParams(activeModel).use { params ->
                configureSearchOptions(params)
                val promptText = prompt.asPlainText()
                activeTokenizer.encode(promptText).use { sequences ->
                    val completion = generateCompletion(activeTokenizer, sequences, activeModel, params)
                    parseInsight(completion)
                }
            }
        } catch (ex: GenAIException) {
            lastIssue = ex.message ?: appContext.getString(R.string.tinyllama_runtime_error)
            null
        }
    }

    private fun configureSearchOptions(params: GeneratorParams) {
        params.setSearchOption("max_length", 1024.0)
        params.setSearchOption("min_length", 1.0)
        params.setSearchOption("temperature", 0.4)
        params.setSearchOption("top_p", 0.9)
        params.setSearchOption("top_k", 40.0)
        params.setSearchOption("repetition_penalty", 1.02)
        params.setSearchOption("do_sample", false)
    }

    private fun generateCompletion(
        tokenizer: Tokenizer,
        sequences: Sequences,
        model: Model,
        params: GeneratorParams
    ): String {
        val builder = StringBuilder()
        tokenizer.createStream().use { stream ->
            Generator(model, params).use { generator ->
                generator.appendTokenSequences(sequences)
                for (token in generator) {
                    val chunk = stream.decode(token)
                    builder.append(chunk)
                    if (builder.length >= MAX_GENERATED_CHAR_COUNT) {
                        break
                    }
                }
            }
        }
        return builder.toString()
    }

    private fun parseInsight(rawOutput: String): LlmInsightPayload? {
        val jsonPayload = extractJsonBlock(rawOutput) ?: run {
            lastIssue = appContext.getString(R.string.tinyllama_invalid_response)
            return null
        }

        return try {
            val json = JSONObject(jsonPayload)
            val summary = json.optString("summary").ifBlank {
                appContext.getString(R.string.insight_summary_generic, 0, "TinyLlama")
            }
            val riskScore = json.optInt("riskScore", -1).takeIf { it >= 0 } ?: 50
            val rationale = json.optJSONArray("rationale")?.toStringList().takeUnless { it.isNullOrEmpty() }
                ?: listOf(appContext.getString(R.string.insight_default_rationale))
            val confidence = json.optInt("confidencePercent", 55)

            LlmInsightPayload(
                summary = summary,
                riskScore = riskScore,
                rationale = rationale,
                confidencePercent = confidence
            )
        } catch (_: Exception) {
            lastIssue = appContext.getString(R.string.tinyllama_invalid_response)
            null
        }
    }

    private fun extractJsonBlock(text: String): String? {
        // Attempt to find JSON within markdown code blocks first
        val jsonBlockStart = text.indexOf("```json")
        if (jsonBlockStart != -1) {
            val start = text.indexOf('{', jsonBlockStart)
            val end = text.indexOf("```", start)
            if (start != -1 && end != -1) {
                return text.substring(start, end).trim()
            }
        }

        val start = text.indexOf('{')
        if (start == -1) return null

        var depth = 0
        for (index in start until text.length) {
            when (text[index]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) {
                        return text.substring(start, index + 1)
                    }
                }
            }
        }
        return null
    }

    private fun JSONArray.toStringList(): List<String> {
        val items = mutableListOf<String>()
        for (i in 0 until length()) {
            val value = optString(i)
            if (value.isNotBlank()) {
                items.add(value.trim())
            }
        }
        return items
    }
}

private data class TinyLlamaPrompt(
    val systemPrompt: String,
    val userPrompt: String
) {
    fun asPlainText(): String =
        "<|system|>\n${systemPrompt.trim()}</s>\n<|user|>\n${userPrompt.trim()}</s>\n<|assistant|>\n"
}

data class LlmInsightPayload(
    val summary: String,
    val riskScore: Int,
    val rationale: List<String>,
    val confidencePercent: Int,
    val generatedAt: Long = System.currentTimeMillis()
)

private class TinyLlamaPromptBuilder {

    fun buildPrompt(appName: String, packageName: String, permissions: List<String>): TinyLlamaPrompt {
        // Truncate permissions if there are too many to prevent context overflow
        val maxPermissions = 50
        val permissionsToProcess = if (permissions.size > maxPermissions) {
            permissions.take(maxPermissions)
        } else {
            permissions
        }

        val groupedPermissions = permissionsToProcess.groupBy { permission ->
            permission.substringAfterLast('.')
                .uppercase(Locale.US)
                .substringBefore('_')
        }

        val permissionBulletList = groupedPermissions.entries.joinToString(separator = "\n") { (group, items) ->
            val joined = items.joinToString(transform = ::formatPermissionName)
            "- ${group.replaceFirstChar { it.uppercase(Locale.getDefault()) }}: $joined"
        }

        val truncationNote = if (permissions.size > maxPermissions) {
            "\n(Note: ${permissions.size - maxPermissions} additional permissions omitted for brevity)"
        } else ""

        val systemPrompt = """
            You are a privacy expert. Analyze app permissions for risks. Respond ONLY with a JSON object. Keep the summary and rationale extremely concise and short.
        """.trimIndent()

        val userPrompt = """
            App: $appName ($packageName)
            Permissions:
            $permissionBulletList
            $truncationNote

            Return strictly JSON:
            {
              "summary": "One sentence summary.",
              "riskScore": 0-100,
              "rationale": ["Short reason 1", "Short reason 2"],
              "confidencePercent": 0-100
            }
        """.trimIndent()

        return TinyLlamaPrompt(systemPrompt, userPrompt)
    }

    private fun formatPermissionName(permission: String): String {
        return permission.substringAfterLast('.')
            .lowercase(Locale.getDefault())
            .replace('_', ' ')
            .replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
            }
    }
}
