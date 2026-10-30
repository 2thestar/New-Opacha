param([string]$OutputRoot = 'lwjgl3/build/organic')
$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$captureRoot = [IO.Path]::GetFullPath((Join-Path $projectRoot $OutputRoot))
$libraryRoot = Join-Path $projectRoot 'lwjgl3/build/install/new-opacha/lib'
$classesRoot = Join-Path $projectRoot 'lwjgl3/build/classes/java/smokeTest'
$testClass = Join-Path $classesRoot 'yio/tro/opacha/lwjgl3/OrganicVisualTest.class'
if (-not (Test-Path -LiteralPath $testClass)) { throw 'Build :lwjgl3:installDist and :lwjgl3:smokeTestClasses first.' }
$javaPath = Join-Path $projectRoot '.tools/jdk-17.0.20.1+1/bin/java.exe'
$classPath = (@($classesRoot) + @(Get-ChildItem -LiteralPath $libraryRoot -Filter '*.jar' | ForEach-Object FullName)) -join [IO.Path]::PathSeparator
$cases = @(
    @{Name='Phone-Day'; Width=390; Height=844; Theme='day'},
    @{Name='Phone-Night'; Width=390; Height=844; Theme='night'},
    @{Name='Desktop-Day'; Width=480; Height=800; Theme='day'},
    @{Name='Desktop-Night'; Width=480; Height=800; Theme='night'}
)
Push-Location $projectRoot
try {
    foreach ($case in $cases) {
        $casePath = Join-Path $captureRoot $case.Name
        if (Test-Path -LiteralPath $casePath) { throw "Capture exists; use a fresh OutputRoot: $casePath" }
        New-Item -ItemType Directory -Path $casePath -Force | Out-Null
        & $javaPath "-Djava.io.tmpdir=$casePath" "-Dorg.lwjgl.system.SharedLibraryExtractPath=$casePath/native" `
            -cp $classPath yio.tro.opacha.lwjgl3.OrganicVisualTest $case.Width $case.Height $casePath $case.Theme
        if ($LASTEXITCODE -ne 0) { throw "Organic native capture failed: $($case.Name)" }
    }
} finally { Pop-Location }
