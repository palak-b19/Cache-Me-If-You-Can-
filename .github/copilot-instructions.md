# Cache-Me-If-You-Can AI Instructions

## Project Overview
Android privacy helper app that analyzes installed applications to provide on-device risk insights using a local LLM (TinyLlama) via ONNX Runtime GenAI, with a heuristic fallback.

## Architecture & Core Components
- **Insight Engine**: `PermissionInsightEngine` (`app/src/main/java/com/example/ussdemoproject/ai/`) is the central orchestrator. It manages:
  - **Caching**: In-memory `LruCache` (6-hour TTL) to avoid redundant processing.
  - **Strategy**: Tries LLM first, falls back to heuristics if unavailable/fails.
- **LLM Client**: `TinyLlamaInsightClient` wraps `onnxruntime-genai`.
  - **Assets**: Models live in `app/src/main/assets/models/tinyllama/`.
  - **Prompting**: `TinyLlamaPromptBuilder` constructs strict JSON-output prompts.
- **UI Layer**: `AppDetailActivity` consumes `PermissionInsightResult` via Kotlin Coroutines (`lifecycleScope`).

## Critical Developer Workflows
- **Initial Setup**: You MUST run `scripts/download_tinyllama_assets.ps1` (PowerShell) to fetch the TinyLlama ONNX model and the GenAI AAR.
  - Without this, the app builds but runs in "Heuristic Only" mode.
- **Build**: Standard Gradle: `./gradlew.bat assembleDebug`.
- **Debugging AI**:
  - Check `TinyLlamaInsightClient.lastKnownIssue()` for runtime errors.
  - `PermissionInsightResult.Unavailable` or `llmUnavailableReason` in the UI indicates fallback triggers.

## Coding Conventions & Patterns
- **Async/Concurrency**: Use `Dispatchers.Default` for AI inference. Never block the main thread with `PermissionInsightEngine.analyze`.
- **LLM Output Handling**:
  - The LLM is prompted to return JSON.
  - Use robust manual parsing (`JSONObject`) in `TinyLlamaInsightClient.parseInsight` as the model may output preamble/postscript text.
  - Always validate JSON fields (`optString`, `optInt`) with safe defaults.
- **Heuristics**: When modifying `PermissionInsightEngine`, ensure the heuristic path remains functional as a reliable fallback.
- **ViewBinding**: Use `ActivityAppDetailBinding` patterns for UI updates.

## External Dependencies
- **ONNX Runtime GenAI**: Provided as a local AAR (`libs/onnxruntime-genai-android-0.11.0.aar`).
- **TinyLlama**: 1.1B Chat model (quantized int4) loaded from app assets.
