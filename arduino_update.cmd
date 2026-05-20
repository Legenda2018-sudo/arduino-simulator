@echo off
setlocal enabledelayedexpansion

cd /d "%~dp0"
set "ORIGIN=%CD%"

color 0A
title Arduino Simulator - build exe
cls

:: --- JDK 17 ---
set "JAVA_EXE="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
if not defined JAVA_EXE if exist "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
    set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
)
if not defined JAVA_EXE (
    for /f "delims=" %%P in ('where jpackage 2^>nul') do (
        set "JAVA_EXE=%%~dpPjava.exe"
        for %%D in ("%%~dpP..") do set "JAVA_HOME=%%~fD"
        goto :java_found
    )
)
:java_found
if not exist "%JAVA_EXE%" (
    echo [ERROR] JDK 17 not found. Install Eclipse Temurin 17.
    pause
    exit /b 1
)
set "PATH=%JAVA_HOME%\bin;%PATH%"

where jpackage >nul 2>&1
if errorlevel 1 (
    echo [ERROR] jpackage not found in JDK 17.
    pause
    exit /b 1
)

:: --- maven-wrapper.jar ---
set "WRAPPER_SRC=%ORIGIN%\.mvn\wrapper\maven-wrapper.jar"
if not exist "%WRAPPER_SRC%" (
    echo [INFO] Downloading maven-wrapper.jar ...
    powershell -NoProfile -Command "Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.1.0/maven-wrapper-3.1.0.jar' -OutFile '%WRAPPER_SRC%' -UseBasicParsing"
)
if not exist "%WRAPPER_SRC%" (
    echo [ERROR] Missing .mvn\wrapper\maven-wrapper.jar
    pause
    exit /b 1
)

echo ============================================
echo   Arduino Simulator - update / build exe
echo ============================================
echo.
echo Source: %ORIGIN%
echo Java:   %JAVA_EXE%
echo.
echo Press any key to start build...
pause >nul
echo.

:: --- copy to TEMP (no brackets in path - fixes mvnw/java on Windows) ---
set "BUILD_DIR=%TEMP%\arduino_sim_build"
if exist "%BUILD_DIR%" rd /s /q "%BUILD_DIR%" 2>nul
mkdir "%BUILD_DIR%" 2>nul
if not exist "%BUILD_DIR%" (
    echo [ERROR] Cannot create %BUILD_DIR%
    pause
    exit /b 1
)

echo [INFO] Copying project to temp folder...
robocopy "%ORIGIN%" "%BUILD_DIR%" /E /XD target .git .idea /NFL /NDL /NJH /NJS /nc /ns /np >nul
if errorlevel 8 (
    echo [ERROR] Copy failed.
    pause
    exit /b 1
)

cd /d "%BUILD_DIR%"
set "WRAPPER=%BUILD_DIR%\.mvn\wrapper\maven-wrapper.jar"

echo [1/2] Maven: clean package -DskipTests ...
call :do_maven clean package -DskipTests
if errorlevel 1 goto failed
if not exist "%BUILD_DIR%\target\classes\org\example\arduino\HelloApplication.class" (
    echo [ERROR] Compile failed - no .class files.
    goto failed
)

echo.
echo [2/2] Maven: package exe ...
call :do_maven package -f packaging/exe/pom.xml -DskipTests
if errorlevel 1 goto failed
if not exist "%BUILD_DIR%\target\dist\ArduinoSimulator\ArduinoSimulator.exe" (
    echo [ERROR] EXE not created.
    goto failed
)

echo.
echo [INFO] Copying result back to project folder...
if not exist "%ORIGIN%\target" mkdir "%ORIGIN%\target"
robocopy "%BUILD_DIR%\target" "%ORIGIN%\target" /E /NFL /NDL /NJH /NJS /nc /ns /np >nul

echo.
echo ============================================
echo   Build OK
echo ============================================
echo.
echo EXE: %ORIGIN%\target\dist\ArduinoSimulator\ArduinoSimulator.exe
echo Copy folder ArduinoSimulator to another PC.
echo.
rd /s /q "%BUILD_DIR%" 2>nul
pause
exit /b 0

:do_maven
"%JAVA_EXE%" -classpath "%WRAPPER%" "-Dmaven.multiModuleProjectDirectory=%BUILD_DIR%" org.apache.maven.wrapper.MavenWrapperMain %*
exit /b !errorlevel!

:failed
echo.
echo ============================================
echo   Build FAILED
echo ============================================
echo.
echo Check:
echo  - internet connection (Maven downloads libs first time)
echo  - or IntelliJ: Maven - Lifecycle - package
echo.
rd /s /q "%BUILD_DIR%" 2>nul
pause
exit /b 1
