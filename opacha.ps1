[CmdletBinding()]
param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]] $GradleArguments
)

$ErrorActionPreference = 'Stop'
$previousJavaHome = $env:JAVA_HOME
$previousGradleHome = $env:GRADLE_USER_HOME
$previousAndroidHome = $env:ANDROID_HOME
$projectJdk = Join-Path $PSScriptRoot '.tools/jdk-17.0.20.1+1'
$projectAndroidSdk = Join-Path $PSScriptRoot '.tools/android-sdk'

if (-not $env:JAVA_HOME -and (Test-Path (Join-Path $projectJdk 'bin/javac.exe'))) {
    $env:JAVA_HOME = $projectJdk
}
if (-not $env:JAVA_HOME -and -not (Get-Command javac -ErrorAction SilentlyContinue)) {
    throw 'JDK 17 is required. Run .\scripts\setup-java.ps1 first, or set JAVA_HOME to an installed JDK.'
}
if (-not $env:GRADLE_USER_HOME) {
    $env:GRADLE_USER_HOME = Join-Path $PSScriptRoot '.gradle-user-home'
}
if (-not $env:ANDROID_HOME -and (Test-Path (Join-Path $projectAndroidSdk 'platform-tools/adb.exe'))) {
    $env:ANDROID_HOME = $projectAndroidSdk
}
if (-not $GradleArguments) {
    $GradleArguments = @(':lwjgl3:run')
}

try {
    Push-Location $PSScriptRoot
    & (Join-Path $PSScriptRoot 'gradlew.bat') @GradleArguments
    $result = $LASTEXITCODE
} finally {
    Pop-Location
    $env:JAVA_HOME = $previousJavaHome
    $env:GRADLE_USER_HOME = $previousGradleHome
    $env:ANDROID_HOME = $previousAndroidHome
}
exit $result
