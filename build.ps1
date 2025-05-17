# Set Java Home to your specific JDK 17 installation
$env:JAVA_HOME = "$env:USERPROFILE\Downloads\jdk-17.0.12_windows-x64_bin\jdk-17.0.12"
echo "Killing Starsector"
Get-Process java -ErrorAction SilentlyContinue | Where-Object {
    ($_ | Get-Process).Path -like 'C:\Starsector\jre\bin\java.exe'
} | Stop-Process -Force

# Verify Java Home is set correctly
if (-not (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    Write-Error "Java not found at $env:JAVA_HOME\bin\java.exe"
    Write-Host "Please install Java 17 and set the correct path in the script."
    exit 1
}

Write-Host "Using Java from: $env:JAVA_HOME"

$buildDir = ".\build\classes"
if (-not (Test-Path $buildDir)) {
    New-Item -ItemType Directory -Path $buildDir -Force | Out-Null
}

$dependencies = @(
    "C:\Starsector\starsector-core\starfarer.api.jar",
    "C:\Starsector\starsector-core\starfarer_obf.jar",
    "C:\Starsector\starsector-core\fs.common_obf.jar",
    "C:\Starsector\starsector-core\fs.sound_obf.jar",
    
    "C:\Starsector\starsector-core\jinput.jar",
    "C:\Starsector\starsector-core\log4j-1.2.9.jar",
    "C:\Starsector\starsector-core\lwjgl.jar",
    "C:\Starsector\starsector-core\lwjgl_util.jar"

)

foreach ($dep in $dependencies) {
    if (-not (Test-Path $dep)) {
        Write-Warning "Missing dependency: $dep"
        Write-Warning "This might cause compilation issues if the dependency is required."
    }
}

$classpath = ($dependencies -join ";")

Write-Host "Compiling Java sources..."
$sourceFiles = Get-ChildItem -Path ".\src" -Filter "*.java" -Recurse
$javacArgs = @(
    "-encoding", "UTF-8"
    "-source", "17"
    "-target", "17"
    "-cp", $classpath
    "-d", $buildDir
    # "-Xlint:deprecation"
    $sourceFiles.FullName
)

& "$env:JAVA_HOME\bin\javac" $javacArgs

if ($LASTEXITCODE -eq 0) {
    Write-Host "Compilation successful. Creating JAR..."
    
    $jarFile = ".\jars\FleetPresetManagerJAR.jar"
    Push-Location $buildDir
    & "$env:JAVA_HOME\bin\jar" -cf "..\..\$jarFile" .
    Pop-Location
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Build completed successfully!" -ForegroundColor Green
        Write-Host "JAR file created at: $jarFile"
        $cwd = Get-Location; 
        if ($LASTEXITCODE -eq 0) {
            echo "Starting starsector.exe"
            Set-Location C:\Starsector; .\starsector.exe; Set-Location $cwd; rm -r build
        }
    } else {
        Write-Host "JAR creation failed!" -ForegroundColor Red; rm -r build
    }
} else {
    Write-Host "Compilation failed!" -ForegroundColor Red; rm -r build
} 