# Script to generate Android release keystore

$keystorePath = "habittracker-release-key.jks"
$alias = "habittracker"
$storePass = "HabitTracker2025!"
$keyPass = "HabitTracker2025!"
$validity = 10000
$dname = "CN=Atrajit Sarkar, OU=HabitTracker, O=HabitTracker, L=India, S=India, C=IN"

Write-Host "Generating Android release keystore..."
Write-Host "This will use Java from Android Studio or your system Java installation"

# Try to find keytool from Android Studio
$androidStudioPaths = @(
    "$env:LOCALAPPDATA\Android\Sdk\cmdline-tools\latest\bin",
    "C:\Program Files\Android\Android Studio\jbr\bin",
    "C:\Program Files\Android\Android Studio\jre\bin"
)

$keytoolPath = $null
foreach ($path in $androidStudioPaths) {
    $testPath = Join-Path $path "keytool.exe"
    if (Test-Path $testPath) {
        $keytoolPath = $testPath
        break
    }
}

if (-not $keytoolPath) {
    Write-Error "keytool.exe not found. Please install Java JDK or Android Studio."
    exit 1
}

Write-Host "Using keytool from: $keytoolPath"

& $keytoolPath -genkeypair -v `
    -keystore $keystorePath `
    -keyalg RSA `
    -keysize 2048 `
    -validity $validity `
    -alias $alias `
    -storepass $storePass `
    -keypass $keyPass `
    -dname $dname

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n✅ Keystore generated successfully: $keystorePath" -ForegroundColor Green
    Write-Host "`nNow creating keystore.properties file..."
    
    $propertiesContent = @"
RELEASE_STORE_FILE=habittracker-release-key.jks
RELEASE_STORE_PASSWORD=$storePass
RELEASE_KEY_ALIAS=$alias
RELEASE_KEY_PASSWORD=$keyPass
"@
    
    Set-Content -Path "keystore.properties" -Value $propertiesContent
    Write-Host "✅ keystore.properties created" -ForegroundColor Green
    
    Write-Host "`n⚠️  IMPORTANT: Keep these files secure and do NOT commit them to git!" -ForegroundColor Yellow
    Write-Host "Store password: $storePass"
    Write-Host "Key alias: $alias"
} else {
    Write-Error "Failed to generate keystore"
    exit 1
}
