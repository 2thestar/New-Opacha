param(
    [Parameter(Mandatory=$true)][string]$Name,
    [string]$CoreJar,
    [string]$LauncherJar,
    [ValidateRange(0,240)][int]$Fps=0,
    [switch]$CheckBudgets
)
$ErrorActionPreference='Stop'
if($Name -notmatch '^[a-zA-Z0-9._-]+$'){throw 'Use a simple run name'}
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$lib=Join-Path $root 'lwjgl3/build/install/new-opacha/lib'
$classes=Join-Path $root 'lwjgl3/build/classes/java/smokeTest'
$output=Join-Path $root "lwjgl3/build/performance/$Name"
if(Test-Path -LiteralPath $output){throw 'Run already exists; use a fresh name'}
if(-not $CoreJar){
    $candidates=@(Get-ChildItem -LiteralPath $lib -Filter 'core-*.jar')
    if($candidates.Count -ne 1){throw 'Expected one installed core JAR'}
    $CoreJar=$candidates[0].FullName
}
if(-not $LauncherJar){
    $candidates=@(Get-ChildItem -LiteralPath $lib -Filter 'lwjgl3-*.jar')
    if($candidates.Count -ne 1){throw 'Expected one installed launcher JAR'}
    $LauncherJar=$candidates[0].FullName
}
if(-not(Test-Path -LiteralPath (Join-Path $classes 'yio/tro/opacha/lwjgl3/FramePerformanceTest.class'))){throw 'Build :lwjgl3:installDist and :lwjgl3:smokeTestClasses first'}
$core=(Resolve-Path -LiteralPath $CoreJar).Path
$launcher=(Resolve-Path -LiteralPath $LauncherJar).Path
$dependencies=Get-ChildItem -LiteralPath $lib -Filter '*.jar' | Where-Object {$_.Name -notmatch '^(core|lwjgl3)-'}
$classpath=(@($classes,$core,$launcher)+@($dependencies.FullName))-join [IO.Path]::PathSeparator
[IO.Directory]::CreateDirectory($output) | Out-Null
$manifest=@{
    core_sha256=(Get-FileHash -LiteralPath $core).Hash
    launcher_sha256=(Get-FileHash -LiteralPath $launcher).Hash
    benchmark_class_sha256=(Get-FileHash -LiteralPath (Join-Path $classes 'yio/tro/opacha/lwjgl3/FramePerformanceTest.class')).Hash
    art_provenance_sha256=(Get-FileHash -LiteralPath (Join-Path $root 'assets/game/battlefield/provenance.json')).Hash
    viewport='390x844';foreground_fps=$Fps;verify_budgets=[bool]$CheckBudgets
    started_utc=[DateTime]::UtcNow.ToString('o')
}
$manifest | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $output 'manifest.json') -Encoding utf8
$java=Join-Path $root '.tools/jdk-17.0.20.1+1/bin/java.exe'
if(-not(Test-Path -LiteralPath $java)){$java=(Get-Command java -ErrorAction Stop).Source}
Push-Location $root
try{
    & $java "-Djava.io.tmpdir=$output" "-Dorg.lwjgl.system.SharedLibraryExtractPath=$output/native" "-DprofileFps=$Fps" "-DverifyProfile=$($CheckBudgets.IsPresent.ToString().ToLowerInvariant())" -cp $classpath yio.tro.opacha.lwjgl3.FramePerformanceTest $output
    if($LASTEXITCODE -ne 0){throw 'Frame benchmark failed'}
}finally{Pop-Location}
