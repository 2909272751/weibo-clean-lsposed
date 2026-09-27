param([string]$Serial)
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$workspace=Split-Path $root -Parent
$sdk=Join-Path $workspace '.android_build_tools\android-sdk'
$jdk=(Get-ChildItem (Join-Path $workspace '.android_build_tools\jdk17') -Directory | Select-Object -First 1).FullName
$env:JAVA_HOME=$jdk
$env:Path=(Join-Path $jdk 'bin')+';'+$env:Path
$android=Join-Path $sdk 'platforms\android-35\android.jar'
$tools=Join-Path $sdk 'build-tools\35.0.0'
$out=Join-Path $root 'evidence\tests-v030'
New-Item -ItemType Directory -Force "$out\classes","$out\dex" | Out-Null
$sources=@('AdSignals','Config','CardFilter','TextRules','SettingsBackup') | ForEach-Object {Join-Path $root "app\src\io\github\weiboclean\$_.java"}
$tests=Get-ChildItem $PSScriptRoot -Filter '*Test.java' | ForEach-Object FullName
& "$jdk\bin\javac.exe" -encoding UTF-8 -source 8 -target 8 -nowarn -cp $android -d "$out\classes" @sources @tests
if($LASTEXITCODE -ne 0){throw 'Test compilation failed'}
& "$jdk\bin\java.exe" -cp "$out\classes" io.github.weiboclean.TextRulesTest
if($LASTEXITCODE -ne 0){throw 'Text rules failed'}
& "$jdk\bin\jar.exe" cf "$out\tests.jar" -C "$out\classes" .
& "$tools\d8.bat" --min-api 26 --lib $android --output "$out\dex" "$out\tests.jar"
if($LASTEXITCODE -ne 0){throw 'Test DEX failed'}
& "$jdk\bin\jar.exe" cf "$out\android-tests.jar" -C "$out\dex" classes.dex
if(!$Serial){Write-Host 'Android checks prepared. Pass -Serial to run on device.';exit 0}
$adb='C:\Android\adb.exe'
& $adb -s $Serial get-state
if($LASTEXITCODE -ne 0){throw 'Device unavailable; Android tests NOT executed'}
& $adb -s $Serial push "$out\android-tests.jar" /data/local/tmp/weiboclean-tests.jar
if($LASTEXITCODE -ne 0){throw 'Test push failed'}
foreach($test in @('TextRulesTest','CardFilterTest','BackupTest','AdSignalsTest')){
 & $adb -s $Serial shell "CLASSPATH=/data/local/tmp/weiboclean-tests.jar app_process / io.github.weiboclean.$test"
 if($LASTEXITCODE -ne 0){throw "$test failed"}
}
