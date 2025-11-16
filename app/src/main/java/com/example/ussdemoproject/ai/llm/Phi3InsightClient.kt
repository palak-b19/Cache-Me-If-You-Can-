package com.example.ussdemoproject.ai.llm

import ai.onnxruntime.genai.GenAIException
import ai.onnxruntime.genai.Generator
import ai.onnxruntime.genai.GeneratorParams
import ai.onnxruntime.genai.Model
import ai.onnxruntime.genai.Sequences
import ai.onnxruntime.genai.Tokenizer
import android.content.Context
import android.content.res.AssetManager
import com.example.ussdemoproject.R
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.io.use
import org.json.JSONArray
import org.json.JSONObject

private const val MODEL_VARIANT = "phi3-mini-4k-instruct-cpu-int4-rtn-block-32-acc-level-4"
private const val MODEL_ASSET_DIRECTORY = "models/phi3"
private const val MODEL_CACHE_DIRECTORY = "phi3-mini-4k"
private val REQUIRED_MODEL_ASSETS = listOf(
    "genai_config.json",
    "config.json",
    "tokenizer.json",
    "tokenizer.model",
    "tokenizer_config.json",
    "special_tokens_map.json",
    "added_tokens.json",
    "$MODEL_VARIANT.onnx",
    "$MODEL_VARIANT.onnx.data"
)
private const val MAX_GENERATED_CHAR_COUNT = 1200

/**
 * Hosts Phi-3 Mini via ONNX Runtime GenAI by copying the bundled assets onto disk
 * and running the lightweight Java bindings for prompt/response generation.
 */
class Phi3InsightClient(private val context: Context) {

    private val appContext = context.applicationContext
    private val assetManager: AssetManager = appContext.assets
    private val promptBuilder = Phi3PromptBuilder()

    @Volatile
    private var model: Model? = null
    @Volatile
    private var tokenizer: Tokenizer? = null

    @Volatile
    private var lastIssue: String? = appContext.getString(R.string.phi3_runtime_unimplemented)

    fun lastKnownIssue(): String? = lastIssue

    fun isRuntimeReady(): Boolean = ensureRuntimeReady()

    fun generateInsight(
        appName: String,
        packageName: String,
        permissions: List<String>
    ): LlmInsightPayload? {
        if (!ensureRuntimeReady()) {
            return null
        }

        val prompt = promptBuilder.buildPrompt(appName, packageName, permissions)
        return try {
            runModel(prompt)
        } catch (t: Throwable) {
            lastIssue = t.message ?: appContext.getString(R.string.phi3_runtime_error)
            null
        }
    }

    private fun ensureRuntimeReady(): Boolean {
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
                lastIssue = ex.message ?: appContext.getString(R.string.phi3_runtime_error)
                closeRuntime()
                false
            }
        }
    }

    private fun materializeAssets(): File? {
        val modelDir = File(appContext.filesDir, MODEL_CACHE_DIRECTORY)
        if (!modelDir.exists() && !modelDir.mkdirs()) {
            lastIssue = appContext.getString(R.string.phi3_model_missing)
            return null
        }

        REQUIRED_MODEL_ASSETS.forEach { assetName ->
            val assetPath = locateAsset(assetName) ?: run {
                lastIssue = when {
                    assetName.endsWith(".onnx") -> appContext.getString(R.string.phi3_model_missing)
                    assetName.contains("tokenizer") -> appContext.getString(R.string.phi3_tokenizer_missing)
                    assetName.contains("genai_config") -> appContext.getString(R.string.phi3_config_missing)
                    else -> appContext.getString(R.string.phi3_asset_missing, assetName)
                }
                return null
            }

            val destination = File(modelDir, assetName)
            if (destination.exists() && destination.length() > 0L) {
                return@forEach
            }

            if (!copyAsset(assetPath, destination)) {
                lastIssue = appContext.getString(R.string.phi3_asset_copy_failed, assetName)
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

    private fun runModel(prompt: Phi3Prompt): LlmInsightPayload? {
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
            lastIssue = ex.message ?: appContext.getString(R.string.phi3_runtime_error)
            null
        }
    }

    private fun configureSearchOptions(params: GeneratorParams) {
        params.setSearchOption("max_length", 640.0)
        params.setSearchOption("min_length", 64.0)
        params.setSearchOption("temperature", 0.2)
        params.setSearchOption("top_p", 0.9)
        params.setSearchOption("top_k", 40.0)
        params.setSearchOption("repetition_penalty", 1.05)
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
            lastIssue = appContext.getString(R.string.phi3_invalid_response)
            return null
        }

        return try {
            val json = JSONObject(jsonPayload)
            val summary = json.optString("summary").ifBlank {
                appContext.getString(R.string.insight_summary_generic, 0, "Phi-3")
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
            lastIssue = appContext.getString(R.string.phi3_invalid_response)
            null
        }
    }

    private fun extractJsonBlock(text: String): String? {
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

private data class Phi3Prompt(
    val systemPrompt: String,
    val userPrompt: String
) {
    fun asPlainText(): String = "${systemPrompt.trim()}\n\n${userPrompt.trim()}"
}

data class LlmInsightPayload(
    val summary: String,
    val riskScore: Int,
    val rationale: List<String>,
    val confidencePercent: Int,
    val generatedAt: Long = System.currentTimeMillis()
)

private class Phi3PromptBuilder {

    fun buildPrompt(appName: String, packageName: String, permissions: List<String>): Phi3Prompt {
        val groupedPermissions = permissions.groupBy { permission ->
            permission.substringAfterLast('.')
                .uppercase(Locale.US)
                .substringBefore('_')
        }

        val permissionBulletList = groupedPermissions.entries.joinToString(separator = "\n") { (group, items) ->
            val joined = items.joinToString(transform = ::formatPermissionName)
            "- ${group.replaceFirstChar { it.uppercase(Locale.getDefault()) }}: $joined"
        }

        val systemPrompt = """
            You are an on-device privacy analyst running entirely offline. When asked about an app's permissions, you respond with a concise JSON object that contains a numeric risk score (0-100), a short plain-language summary, an array named "rationale" with 1-4 bullet explanations, and a confidencePercent (0-100). The JSON must not contain any additional keys.
        """.trimIndent()

        val userPrompt = """
            App name: $appName
            Package: $packageName
            Permissions:
            $permissionBulletList

            Respond strictly in JSON with the shape:
            {
              "summary": "string",
              "riskScore": number,
              "rationale": ["reason1", "reason2"],
              "confidencePercent": number
            }
        """.trimIndent()

        return Phi3Prompt(systemPrompt, userPrompt)
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
