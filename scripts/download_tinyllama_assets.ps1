[CmdletBinding()]
param(
    [string]$OrtGenAiVersion = "0.11.0",
    [string]$AssetSubDirectory = "tinyllama",
    [string]$HfToken = $env:HF_TOKEN
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
}

$baseUrl = "https://huggingface.co/webnn/TinyLlama-1.1B-Chat-v1.0-onnx/resolve/main"
$remoteFiles = @(
    @{ Remote = "model.onnx"; Local = "model.onnx"; Required = $true },
    @{ Remote = "model.onnx.data"; Local = "model.onnx.data"; Required = $true },
    @{ Remote = "tokenizer.json"; Local = "tokenizer.json"; Required = $true },
    @{ Remote = "tokenizer_config.json"; Local = "tokenizer_config.json"; Required = $true },
    @{ Remote = "special_tokens_map.json"; Local = "special_tokens_map.json"; Required = $false }
)

Write-Section "Downloading TinyLlama model + tokenizer"
foreach ($file in $remoteFiles) {
    $uri = "$baseUrl/$($file.Remote)?download=1"
    $destination = Join-Path $modelAssetsDir $file.Local
    Download-File -Uri $uri -Destination $destination -Headers $headers -Required $file.Required
}

Write-Section "Downloading onnxruntime-genai Android AAR"
$ortFile = "onnxruntime-genai-android-$OrtGenAiVersion.aar"
$ortUri = "https://github.com/microsoft/onnxruntime-genai/releases/download/v$OrtGenAiVersion/$ortFile"
$ortDestination = Join-Path $libsDir $ortFile
Download-File -Uri $ortUri -Destination $ortDestination -Headers @{}

Write-Host "`nAll done. Assets live under:`n - $modelAssetsDir`n - $libsDir" -ForegroundColor Cyan
