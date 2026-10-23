[CmdletBinding()]
param(
    [ValidateSet('Quick','Full','Random','Play','Touch','Feel')] [string] $Mode = 'Quick',
    [ValidateRange(1,48)] [int] $Level = 1,
    [string] $Levels,
    [ValidateRange(1,1000)] [int] $Seeds = 2,
    [ValidateRange(1,1000000)] [int] $SeedStart = 1,
    [ValidateSet('easy','normal','hard')] [string] $Difficulty = 'hard',
    [ValidatePattern('^[a-z,]*$')] [string] $Styles = '',
    [ValidatePattern('^[a-zA-Z0-9_-]+$')] [string] $Name = '',
    [ValidateSet('Phone','Desktop')] [string] $Screen = 'Phone',
    [string] $Compare,
    [switch] $Open
)
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$labRoot = Join-Path $root 'build/playtest'
New-Item -ItemType Directory -Force -Path $labRoot | Out-Null
if (-not $Name) { $Name = (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '-' + $Mode.ToLowerInvariant() }
$output = Join-Path $labRoot $Name
if (Test-Path -LiteralPath $output) { throw "Run already exists: $output. Use a new -Name." }
$clock = [Diagnostics.Stopwatch]::StartNew()
Push-Location $root
try {
    # Content fingerprints prevent silently testing stale JARs, even after a checkout or timestamp reset.
    $files = @(& rg --files src assets core lwjgl3 gradle) + @('build.gradle','settings.gradle','gradle.properties')
    $fingerprintText = ($files | Sort-Object | ForEach-Object { $_ + ':' + (Get-FileHash -LiteralPath $_ -Algorithm SHA256).Hash }) -join "`n"
    $hasher = [Security.Cryptography.SHA256]::Create()
    try { $fingerprint = [BitConverter]::ToString($hasher.ComputeHash([Text.Encoding]::UTF8.GetBytes($fingerprintText))).Replace('-','').ToLowerInvariant() }
    finally { $hasher.Dispose() }
    $snapshot = Join-Path $labRoot "compiled/$fingerprint"
    $marker = Join-Path $snapshot 'complete'
    if (-not (Test-Path -LiteralPath $marker)) {
        $buildLock = $null
        $waitClock = [Diagnostics.Stopwatch]::StartNew()
        while (-not $buildLock) {
            try { $buildLock = [IO.File]::Open((Join-Path $labRoot 'build.lock'),[IO.FileMode]::OpenOrCreate,[IO.FileAccess]::ReadWrite,[IO.FileShare]::None) }
            catch [IO.IOException] {
                if ($waitClock.Elapsed.TotalSeconds -gt 120) { throw 'Another playtest build has not finished' }
                Start-Sleep -Milliseconds 100
            }
        }
        try {
            if (-not (Test-Path -LiteralPath $marker)) {
                & (Join-Path $root 'opacha.ps1') ':lwjgl3:installDist' ':lwjgl3:smokeTestClasses' '--console=plain' '--quiet'
                if ($LASTEXITCODE -ne 0) { throw 'Playtest build failed' }
                New-Item -ItemType Directory -Force -Path $snapshot | Out-Null
                Copy-Item -LiteralPath (Join-Path $root 'lwjgl3/build/install/new-opacha/lib') -Destination $snapshot -Recurse -Force
                Copy-Item -LiteralPath (Join-Path $root 'lwjgl3/build/classes/java/smokeTest') -Destination (Join-Path $snapshot 'classes') -Recurse -Force
                Set-Content -LiteralPath $marker -Value $fingerprint -Encoding ascii
            }
        } finally { $buildLock.Dispose() }
    }
    $lib = Join-Path $snapshot 'lib'
    $classes = Join-Path $snapshot 'classes'
    $java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/java.exe' } else { Join-Path $root '.tools/jdk-17.0.20.1+1/bin/java.exe' }
    if (-not (Test-Path -LiteralPath $java)) { $java = (Get-Command java -ErrorAction Stop).Source }
    $dimensions = if ($Screen -eq 'Phone') { @(390,844) } else { @(480,800) }
    New-Item -ItemType Directory -Path $output | Out-Null
    $nativeTemp = Join-Path $output 'native'
    New-Item -ItemType Directory -Path $nativeTemp | Out-Null
    $properties = @("-Djava.io.tmpdir=$nativeTemp", "-Dorg.lwjgl.system.SharedLibraryExtractPath=$nativeTemp", "-DlabSeedStart=$SeedStart", "-DlabSeeds=$Seeds", "-DlabDifficulty=$Difficulty", "-DlabStyles=$Styles")
    if ($Mode -eq 'Play') {
        $class = 'yio.tro.opacha.lwjgl3.PlaytestLab'
        $properties += @("-DlabLevel=$Level", "-DlabWidth=$($dimensions[0])", "-DlabHeight=$($dimensions[1])")
        $arguments = @($output)
    } elseif ($Mode -in @('Touch','Feel')) {
        $class = if ($Mode -eq 'Touch') { 'yio.tro.opacha.lwjgl3.PlayabilitySmokeTest' } else { 'yio.tro.opacha.lwjgl3.OriginalFeelSmokeTest' }
        $arguments = @($dimensions[0],$dimensions[1],$output)
    } else {
        $class = 'yio.tro.opacha.lwjgl3.BalancePlaytest'
        $arguments = @($output)
        if ($Mode -ne 'Random') {
            $arguments += 'campaign'
            if (-not $Levels -and $Mode -eq 'Quick') { $Levels = '1,6,9,19,21,28,30,39,43,48' }
            if ($Levels) {
                if ($Levels -notmatch '^([1-9]|[1-3][0-9]|4[0-8])(,([1-9]|[1-3][0-9]|4[0-8]))*$') { throw 'Levels must be comma-separated map numbers 1..48' }
                $properties += "-DlabLevels=$Levels"
            }
        }
    }
    $manifest = [ordered]@{
        mode=$Mode; source_sha256=$fingerprint; git_commit=(& git rev-parse HEAD); seed_start=$SeedStart; seeds=$Seeds;
        levels=$Levels; styles=$Styles; difficulty=$(if ($Mode -in @('Quick','Full')) { 'authored' } else { $Difficulty }); screen=$Screen; started_utc=[DateTime]::UtcNow.ToString('o');
        policy_sha256=(Get-FileHash 'lwjgl3/src/smokeTest/java/yio/tro/opacha/game/gameplay/model/HumanStylePlayer.java').Hash;
        campaign_sha256=(Get-FileHash 'assets/campaign/levels.json').Hash
        viewport_width=$(if ($Mode -in @('Quick','Full','Random')) { 480 } else { $dimensions[0] });
        viewport_height=$(if ($Mode -in @('Quick','Full','Random')) { 800 } else { $dimensions[1] });
        level=$(if ($Mode -eq 'Play') { $Level } else { $null })
    }
    $manifest | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $output 'manifest.json') -Encoding utf8
    & $java @properties '-cp' "$classes;$lib/*" $class @arguments 2>&1 | Tee-Object -FilePath (Join-Path $output 'run.log')
    if ($LASTEXITCODE -ne 0) { throw "Playtest failed. See $output/run.log" }
    if ($Mode -in @('Quick','Full','Random')) {
        $reportArguments = @((Join-Path $root 'scripts/report-playtest.py'),$output)
        if ($Compare) { $reportArguments += @('--compare',$Compare) }
        & python @reportArguments
        if ($LASTEXITCODE -ne 0) { throw 'Playtest report failed' }
        if ($Open) { Start-Process -FilePath (Join-Path $output 'report.html') }
    }
    $manifest['total_wall_seconds'] = [Math]::Round($clock.Elapsed.TotalSeconds,3)
    $manifest | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $output 'manifest.json') -Encoding utf8
    Write-Host ("Ready in {0:N1}s: {1}" -f $clock.Elapsed.TotalSeconds,$output)
} finally { Pop-Location }
