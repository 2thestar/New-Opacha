$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$projectRoot = Split-Path $PSScriptRoot -Parent
$sdkDirectory = Join-Path $projectRoot '.tools/android-sdk'
$sdkToolsDirectory = Join-Path $sdkDirectory 'cmdline-tools/19.0'
$projectJdk = Join-Path $projectRoot '.tools/jdk-17.0.20.1+1'
$previousJavaHome = $env:JAVA_HOME
$previousAndroidUserHome = $env:ANDROID_USER_HOME

if ($env:OS -ne 'Windows_NT') {
    throw 'This setup helper supports Windows. On other systems, install Android SDK platform 36 and build-tools 35.0.0, then set ANDROID_HOME.'
}
if (-not $env:JAVA_HOME -and (Test-Path (Join-Path $projectJdk 'bin/java.exe'))) {
    $env:JAVA_HOME = $projectJdk
}
if (-not $env:JAVA_HOME) {
    throw 'Set JAVA_HOME to JDK 17, or run .\scripts\setup-java.ps1 first.'
}
$env:ANDROID_USER_HOME = Join-Path $projectRoot '.tools/android-user'

try {
    if (-not (Test-Path (Join-Path $sdkToolsDirectory 'bin/sdkmanager.bat'))) {
        New-Item -ItemType Directory -Path (Join-Path $sdkDirectory 'cmdline-tools') -Force | Out-Null
        $archivePath = Join-Path $projectRoot '.tools/android-commandline-19.zip'
        Invoke-WebRequest 'https://dl.google.com/android/repository/commandlinetools-win-13114758_latest.zip' -OutFile $archivePath -UseBasicParsing
        if ((Get-FileHash -LiteralPath $archivePath -Algorithm SHA256).Hash.ToLowerInvariant() -ne '98b565cb657b012dae6794cefc0f66ae1efb4690c699b78a614b4a6a3505b003') {
            throw 'The Android tools download failed its SHA-256 check. It has not been unpacked.'
        }
        $stagingDirectory = Join-Path $sdkDirectory 'cmdline-tools/bootstrap'
        Expand-Archive -LiteralPath $archivePath -DestinationPath $stagingDirectory -Force
        $extractedTools = (Resolve-Path (Join-Path $stagingDirectory 'cmdline-tools')).Path
        $workspacePrefix = $projectRoot + [IO.Path]::DirectorySeparatorChar
        if (-not $extractedTools.StartsWith($workspacePrefix, [StringComparison]::OrdinalIgnoreCase) -or -not $sdkToolsDirectory.StartsWith($workspacePrefix, [StringComparison]::OrdinalIgnoreCase)) {
            throw 'The Android SDK paths are outside this project.'
        }
        Move-Item -LiteralPath $extractedTools -Destination $sdkToolsDirectory
        Remove-Item -LiteralPath $archivePath
    }

    & (Join-Path $sdkToolsDirectory 'bin/sdkmanager.bat') "--sdk_root=$sdkDirectory" 'platform-tools' 'platforms;android-36' 'build-tools;35.0.0'
    if ($LASTEXITCODE -ne 0) { throw 'Android SDK installation failed.' }
    foreach ($requiredFile in @('platforms/android-36/android.jar', 'build-tools/35.0.0/aapt2.exe', 'platform-tools/adb.exe')) {
        if (-not (Test-Path (Join-Path $sdkDirectory $requiredFile))) { throw "Android SDK file is missing: $requiredFile" }
    }

    $signingDirectory = Join-Path $projectRoot '.tools/signing'
    $keystorePath = Join-Path $signingDirectory 'new-opacha-debug.keystore'
    if (-not (Test-Path -LiteralPath $keystorePath)) {
        New-Item -ItemType Directory -Path $signingDirectory -Force | Out-Null
        & (Join-Path $env:JAVA_HOME 'bin/keytool.exe') -genkeypair -keystore $keystorePath -storetype JKS -storepass android -keypass android -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=New Opacha Development,O=New Opacha,C=GB'
        if ($LASTEXITCODE -ne 0) { throw 'Could not create the local development signing key.' }
    }

    Set-Content -LiteralPath (Join-Path $projectRoot 'local.properties') -Value ('sdk.dir=' + $sdkDirectory.Replace('\', '/').Replace(':', '\:')) -Encoding ascii
    Write-Output 'Android SDK and signing key are ready. Build with .\opacha.ps1 :android:exportApk'
} finally {
    $env:JAVA_HOME = $previousJavaHome
    $env:ANDROID_USER_HOME = $previousAndroidUserHome
}
