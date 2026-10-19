$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

if ($env:OS -ne 'Windows_NT' -or -not [Environment]::Is64BitOperatingSystem) {
    throw 'This helper downloads the Windows x64 JDK. On other systems, install JDK 17 and use ./gradlew.'
}

$projectRoot = Split-Path $PSScriptRoot -Parent
$toolsDirectory = Join-Path $projectRoot '.tools'
$jdkDirectory = Join-Path $toolsDirectory 'jdk-17.0.20.1+1'
if (Test-Path (Join-Path $jdkDirectory 'bin/javac.exe')) {
    Write-Output 'Project JDK is already available. Run .\opacha.ps1 to play.'
    exit 0
}

New-Item -ItemType Directory -Path $toolsDirectory -Force | Out-Null
$archivePath = Join-Path $toolsDirectory 'temurin17.zip'
$downloadUrl = 'https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.20.1%2B1/OpenJDK17U-jdk_x64_windows_hotspot_17.0.20.1_1.zip'
$expectedChecksum = 'e53a79c3c3d86865bd7e787903884331068e71321714ffd44f145785affc7cb0'
Invoke-WebRequest -Uri $downloadUrl -OutFile $archivePath -UseBasicParsing
if ((Get-FileHash -LiteralPath $archivePath -Algorithm SHA256).Hash.ToLowerInvariant() -ne $expectedChecksum) {
    throw 'The JDK download failed its SHA-256 check. It has not been unpacked.'
}

Expand-Archive -LiteralPath $archivePath -DestinationPath $toolsDirectory -Force
if (-not (Test-Path (Join-Path $jdkDirectory 'bin/javac.exe'))) {
    throw 'The downloaded archive did not contain the expected JDK.'
}
Remove-Item -LiteralPath $archivePath
Write-Output 'Project JDK is ready. Run .\opacha.ps1 to play.'
