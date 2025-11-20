# Cache-Me-If-You-Can AI Instructions

## Project Overview
This is an Android application that analyzes installed apps and their permissions to provide on-device risk insights. It uses a hybrid approach combining heuristic rules and an on-device LLM (TinyLlama via ONNX Runtime GenAI).

## Tech Stack
- **Language**: Kotlin (JVM 17)
- **UI Framework**: Android Views (XML) with ViewBinding and DataBinding
- **Build System**: Gradle (Kotlin DSL)
- **AI/ML**: ONNX Runtime GenAI (local AAR), TinyLlama-1.1B-Chat
- **Architecture**: MVVM-like structure with Activities, Adapters, and a dedicated AI engine

## Critical Workflows

### 1. Initial Setup (Required for AI)
The project requires external assets (model files and AAR) that are not in git.
- **Command**: `.\scripts\download_tinyllama_assets.ps1` (PowerShell)
- **Action**: Downloads `onnxruntime-genai-android-0.11.0.aar` to `app/libs/` and model files to `app/src/main/assets/models/tinyllama/`.
- **Note**: If these assets are missing, the app builds but falls back to heuristic-only mode.

### 2. Build & Run
- **Debug Build**: `.\gradlew.bat assembleDebug`
- **Run Tests**: `.\gradlew.bat test`

## Architecture & Patterns

### AI Engine (`com.example.ussdemoproject.ai`)
- **Entry Point**: `PermissionInsightEngine.analyze(appName, packageName, permissions)`
- **Hybrid Logic**:
  1. Checks in-memory `LruCache` (6-hour TTL).
  2. Attempts to run **TinyLlama** via `TinyLlamaInsightClient`.
  3. Falls back to **Heuristic Model** (`runHeuristicModel`) if LLM is unavailable or fails.
- **Heuristics**: Defined in `PERMISSION_SIGNALS` (weighted keywords like "READ_SMS", "CAMERA").

### UI Layer (`com.example.ussdemoproject.ui`)
- Uses standard `AppCompatActivity` and `RecyclerView`.
- **ViewBinding**: Enabled in `build.gradle.kts`. Use `binding.root` for `setContentView`.
- **DataBinding**: Enabled.

### Dependency Management
- **Local AAR**: The ONNX Runtime GenAI library is loaded from `app/libs/`.
- **Gradle Logic**: `app/build.gradle.kts` conditionally includes the AAR and logs a warning if missing.

## Key File Locations
- **AI Logic**: `app/src/main/java/com/example/ussdemoproject/ai/PermissionInsightEngine.kt`
- **LLM Client**: `app/src/main/java/com/example/ussdemoproject/ai/llm/`
- **UI Components**: `app/src/main/java/com/example/ussdemoproject/ui/`
- **Asset Script**: `scripts/download_tinyllama_assets.ps1`
- **Model Config**: `app/src/main/assets/models/tinyllama/genai_config.json`

## Coding Conventions
- **Permissions**: Handle permissions as strings (e.g., "android.permission.CAMERA").
- **Error Handling**: The AI engine must never crash the app; always return a `PermissionInsightResult` (Success or Unavailable).
- **Offline First**: Assume no internet connection. All analysis happens on-device.
