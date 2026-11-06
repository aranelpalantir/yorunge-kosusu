$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
$sdk = Join-Path $PSScriptRoot '.tools\android-sdk'
$bt = Join-Path $sdk 'build-tools\35.0.0'
$platform = Join-Path $sdk 'platforms\android-35\android.jar'
$jdk = $env:JAVA_HOME
function Check-Exit { if ($LASTEXITCODE -ne 0) { throw "Build failed: exit $LASTEXITCODE" } }
New-Item -ItemType Directory -Force build\classes,build\dex,dist | Out-Null
& "$bt\aapt.exe" package -f -M AndroidManifest.xml -S res -A assets -I $platform -F build\base.apk
Check-Exit
$sources = @(Get-ChildItem src -Recurse -Filter *.java | ForEach-Object FullName)
& "$jdk\bin\javac.exe" -encoding UTF-8 -source 8 -target 8 -bootclasspath $platform -d build\classes @sources
Check-Exit
& "$jdk\bin\jar.exe" cf build\classes.jar -C build\classes .
Check-Exit
& "$bt\d8.bat" --lib $platform --min-api 26 --output build\dex build\classes.jar
Check-Exit
Copy-Item build\base.apk build\unsigned.apk -Force
Add-Type -AssemblyName System.IO.Compression,System.IO.Compression.FileSystem
$archive = [IO.Compression.ZipFile]::Open((Join-Path $PSScriptRoot 'build\unsigned.apk'),[IO.Compression.ZipArchiveMode]::Update)
try { [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive,(Join-Path $PSScriptRoot 'build\dex\classes.dex'),'classes.dex') | Out-Null } finally { $archive.Dispose() }
& "$bt\zipalign.exe" -f 4 build\unsigned.apk build\aligned.apk
Check-Exit
if (!(Test-Path .tools\prototype.jks)) {
    & "$jdk\bin\keytool.exe" -genkeypair -keystore .tools\prototype.jks -storepass android -keypass android -alias prototype -dname 'CN=Yorunge Prototype' -keyalg RSA -keysize 2048 -validity 10000
    Check-Exit
}
& "$bt\apksigner.bat" sign --ks .tools\prototype.jks --ks-pass pass:android --key-pass pass:android --out dist\Yorunge-Kosusu.apk build\aligned.apk
Check-Exit
& "$bt\apksigner.bat" verify --verbose dist\Yorunge-Kosusu.apk
Check-Exit
python tools\build_pwa.py
Check-Exit
Get-Item dist\Yorunge-Kosusu.apk | Select-Object FullName,Length
