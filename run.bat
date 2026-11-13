@echo off
setlocal
cd /d "%~dp0"
where java >nul 2>nul
if errorlevel 1 (
  echo Java 17 or later is required. Install a desktop JDK and retry.
  pause
  exit /b 1
)
if not exist "target\SmartStock.jar" (
  where mvn >nul 2>nul
  if errorlevel 1 (
    echo Install Maven 3.8+ to build this source project, or download the runnable JAR.
    pause
    exit /b 1
  )
  call mvn -B clean package
  if errorlevel 1 (
    echo Build failed. Check the messages above.
    pause
    exit /b 1
  )
)
java -jar "target\SmartStock.jar" %*
if errorlevel 1 pause
