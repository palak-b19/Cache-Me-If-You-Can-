[CmdletBinding()]
param(
    [string]$HfToken = $env:HF_TOKEN,
    [string]$ModelVariant = "cpu_and_mobile/cpu-int4-rtn-block-32-acc-level-4",
    [string]$OrtGenAiVersion = "0.11.0",
    [string]$AssetSubDirectory = "phi3"
)

$ErrorActionPreference = "Stop"

function Write-Section {
    param([string]$Message)
    Write-Host "`n=== $Message ===" -ForegroundColor Cyan
}

function Ensure-Directory {
    param([string]$Path)
    if (-not (Test-Path -LiteralPath $Path)) {
        New-Item -ItemType Directory -Path $Path | Out-Null
    }
}

function Download-File {
    param(
        [Parameter(Mandatory = $true)][string]$Uri,
        [Parameter(Mandatory = $true)][string]$Destination,
        [hashtable]$Headers,
        [bool]$Required = $true
    )

    if (Test-Path -LiteralPath $Destination) {
        Write-Host "Skipping existing file: $Destination" -ForegroundColor DarkGray
        return
    }

    try {
        $null = Invoke-WebRequest -Uri $Uri -Headers $Headers -OutFile $Destination -UseBasicParsing
        Write-Host "Fetched $Uri" -ForegroundColor Green
    }
    catch {
        if ($Required) {
            throw
        } else {
            Write-Warning "Optional download failed ($Uri). Continue after verifying manually."
        }
    }
}

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$assetsDir = Join-Path $repoRoot "app/src/main/assets/models"
$modelAssetsDir = Join-Path $assetsDir $AssetSubDirectory
$libsDir = Join-Path $repoRoot "app/libs"

Ensure-Directory -Path $assetsDir
Ensure-Directory -Path $modelAssetsDir
Ensure-Directory -Path $libsDir

$headers = @{}
if ($HfToken) {
    $headers["Authorization"] = "Bearer $HfToken"
} else {
    Write-Warning "No Hugging Face token detected. Phi-3 Mini requires an approved token; the download will likely fail until you set HF_TOKEN."
}

Write-Section "Downloading Phi-3 Mini model + tokenizer"
$variantLeaf = Split-Path $ModelVariant -Leaf
$modelBaseName = "phi3-mini-4k-instruct-$variantLeaf"
$phi3Base = "https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-onnx/resolve/main/$ModelVariant"
$phi3Files = @(
    @{ Remote = "genai_config.json"; Local = "genai_config.json"; Required = $true },
    @{ Remote = "config.json"; Local = "config.json"; Required = $true },
    @{ Remote = "tokenizer.json"; Local = "tokenizer.json"; Required = $true },
    @{ Remote = "tokenizer.model"; Local = "tokenizer.model"; Required = $true },
    @{ Remote = "tokenizer_config.json"; Local = "tokenizer_config.json"; Required = $false },
    @{ Remote = "special_tokens_map.json"; Local = "special_tokens_map.json"; Required = $false },
    @{ Remote = "generation_config.json"; Local = "generation_config.json"; Required = $false },
    @{ Remote = "added_tokens.json"; Local = "added_tokens.json"; Required = $false },
    @{ Remote = "$modelBaseName.onnx"; Local = "$modelBaseName.onnx"; Required = $true },
    @{ Remote = "$modelBaseName.onnx.data"; Local = "$modelBaseName.onnx.data"; Required = $true }
)

foreach ($file in $phi3Files) {
    $uri = "$phi3Base/$($file.Remote)?download=1"
    $destination = Join-Path $modelAssetsDir $file.Local
    Download-File -Uri $uri -Destination $destination -Headers $headers -Required $file.Required
}

Write-Section "Downloading onnxruntime-genai Android AAR"
$ortFile = "onnxruntime-genai-android-$OrtGenAiVersion.aar"
$ortUri = "https://github.com/microsoft/onnxruntime-genai/releases/download/v$OrtGenAiVersion/$ortFile"
$ortDestination = Join-Path $libsDir $ortFile
Download-File -Uri $ortUri -Destination $ortDestination -Headers @{}

Write-Host "`nAll done. Placeholders live under:`n - $modelAssetsDir`n - $libsDir" -ForegroundColor Cyan
