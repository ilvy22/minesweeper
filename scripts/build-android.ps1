param(
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$AndroidSdk = $env:ANDROID_HOME,
    [string]$Gradle = '',
    [string]$ReadOnlyDependencyCache = '',
    [string]$GradleUserHome = '',
    [switch]$Offline
)
$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
if (-not $AndroidSdk) { $AndroidSdk = $env:ANDROID_SDK_ROOT }
if (-not $JavaHome -or -not (Test-Path -LiteralPath (Join-Path $JavaHome 'bin\java.exe'))) {
    throw 'Set -JavaHome or JAVA_HOME to a JDK 17 installation.'
}
if (-not $AndroidSdk -or -not (Test-Path -LiteralPath (Join-Path $AndroidSdk 'platforms\android-36\android.jar'))) {
    throw 'Set -AndroidSdk or ANDROID_HOME to an Android SDK containing platform 36 and Android build tools.'
}
if (-not $GradleUserHome) { $GradleUserHome = Join-Path $taskRoot '.tooling\gradle-home' }
$env:JAVA_HOME = $JavaHome
$env:ANDROID_HOME = $AndroidSdk
$env:ANDROID_SDK_ROOT = $AndroidSdk
$env:ANDROID_USER_HOME = Join-Path $taskRoot '.android'
$env:GRADLE_USER_HOME = $GradleUserHome
if ($ReadOnlyDependencyCache) { $env:GRADLE_RO_DEP_CACHE = $ReadOnlyDependencyCache }
New-Item -ItemType Directory -Force -Path $env:ANDROID_USER_HOME,$env:GRADLE_USER_HOME | Out-Null
$escapedSdk = $AndroidSdk.Replace('\', '\\').Replace(':', '\:')
Set-Content -LiteralPath (Join-Path $taskRoot 'local.properties') -Value "sdk.dir=$escapedSdk" -Encoding ascii

# Each checkout keeps its own signing key outside source control. Retain this
# file privately to install future builds over this APK without losing progress.
$keyStore = Join-Path $env:ANDROID_USER_HOME 'debug.keystore'
if (-not (Test-Path -LiteralPath $keyStore)) {
    $keyTool = Join-Path $JavaHome 'bin\keytool.exe'
    & $keyTool -genkeypair -keystore $keyStore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10950 -dname 'CN=Android Debug,O=Haru Minesweeper,C=KR' -noprompt
    if ($LASTEXITCODE -ne 0) { throw "Debug signing key creation failed with exit code $LASTEXITCODE" }
}
if (-not $Gradle) { $Gradle = Join-Path $taskRoot 'gradlew.bat' }
$arguments = @(':app:assembleDebug', ':app:testDebugUnitTest', ':app:lintDebug', '--console=plain')
if ($Offline) { $arguments += '--offline' }
Push-Location $taskRoot
try {
    & $Gradle @arguments
    if ($LASTEXITCODE -ne 0) { throw "Gradle failed with exit code $LASTEXITCODE" }
    $destination = Join-Path $taskRoot 'dist'
    New-Item -ItemType Directory -Force -Path $destination | Out-Null
    $apkName = 'HaruMinesweeper-v1.0.0.apk'
    $apk = Join-Path $destination $apkName
    Copy-Item -LiteralPath (Join-Path $taskRoot 'app\build\outputs\apk\debug\app-debug.apk') -Destination $apk -Force
    $digest = (Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash.ToLowerInvariant()
    Set-Content -LiteralPath "$apk.sha256" -Value "$digest  $apkName" -Encoding ascii
    Write-Output $apk
} finally { Pop-Location }
