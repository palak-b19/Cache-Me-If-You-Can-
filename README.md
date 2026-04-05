# Permission Manager

# Cache-Me-If-You-Can

Privacy helper that surfaces each installed app, drills into its declared permissions, and now layers an on-device risk insight so users can judge exposure without leaving the device.

## AI risk insights
- `PermissionInsightEngine` (under `app/src/main/java/com/example/ussdemoproject/ai`) keeps everything offline, caching summaries for six hours to avoid repeated work.
- Risk scores (0-100) come with explicit rationales so people see which permissions triggered the assessment.
- If the bundled LLM runtime is unavailable (for example offline or stripped builds), the UI shows an "LLM insights unavailable" banner while keeping heuristic summaries alive.

### Bringing TinyLlama online
1. Fetch the lighter TinyLlama ONNX drop from [webnn/TinyLlama-1.1B-Chat-v1.0-onnx](https://huggingface.co/webnn/TinyLlama-1.1B-Chat-v1.0-onnx). This repo is public, so no Hugging Face token is required.
2. From the repo root run the helper script (Windows example):
	```powershell
	Set-Location -Path "C:\Users\Sidhartha Garg\Documents\GitHub\Cache-Me-If-You-Can-"
	.\scripts\download_tinyllama_assets.ps1
	```
	The script pulls:
	- `model.onnx` + `model.onnx.data`, `tokenizer.json`, `tokenizer_config.json`, and `special_tokens_map.json` into `app/src/main/assets/models/tinyllama/` so ONNX Runtime GenAI can mirror the Hugging Face layout. A handcrafted `genai_config.json` already lives in that folder to describe the TinyLlama geometry.
	- `onnxruntime-genai-android-0.11.0.aar` into `app/libs/`, which Gradle consumes via `implementation(files("libs/onnxruntime-genai-android-0.11.0.aar"))`.
3. Rebuild; the first app launch copies the ONNX file into internal storage and boots ONNX Runtime.
4. If the download script fails (offline, rate limited, etc.), manually fetch the same files and place them in the directories above. Until then, the engine falls back to the heuristic scorer and shows the reason in the UI banner.


