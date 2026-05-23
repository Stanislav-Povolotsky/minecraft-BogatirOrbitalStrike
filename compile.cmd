@echo off
setlocal EnableDelayedExpansion

:: ============================================================
::  BogatirOrbitalStrike — Build Script
::  Target: Java 21 / Paper 26.1.2
::  Output: dist\BogatirOrbitalStrike.jar
:: ============================================================

set PAPER_JAR=libs\paper.jar
:: Paper 26.x API jar (contains org.bukkit.*, io.papermc.*, net.kyori.*):
set API_JAR=libraries\io\papermc\paper\paper-api\26.1.2.build.65-stable\paper-api-26.1.2.build.65-stable.jar
:: Adventure (kyori) transitive dependencies required by javac:
set ADV=libraries\net\kyori
set ADV_CP=%ADV%\adventure-api\4.26.1\adventure-api-4.26.1.jar;%ADV%\adventure-key\4.26.1\adventure-key-4.26.1.jar;%ADV%\adventure-text-serializer-plain\4.26.1\adventure-text-serializer-plain-4.26.1.jar;%ADV%\examination-api\1.3.0\examination-api-1.3.0.jar
set GUAVA_JAR=libraries\com\google\guava\guava\33.5.0-jre\guava-33.5.0-jre.jar
set BUNGEE_JAR=libraries\net\md-5\bungeecord-chat\1.21-R0.2-deprecated+build.21\bungeecord-chat-1.21-R0.2-deprecated+build.21.jar
set SRC_DIR=src
set BUILD_DIR=build\classes
set OUTPUT_DIR=dist
set JAR_NAME=BogatirOrbitalStrike.jar

:: ── Java executables (use modern Java 26 from tools folder) ─────────────────
set JAVA_EXE=C:\Private\Dropbox\Tools\Interpretators\Java\jdk-26.0.1\bin\java.exe
set JAVAC_EXE=C:\Private\Dropbox\Tools\javac26.exe
set JAR_EXE=C:\Private\Dropbox\Tools\Interpretators\Java\jdk-26.0.1\bin\jar.exe

echo.
echo  ==========================================
echo   BogatirOrbitalStrike Compiler
echo   Java 26.0.1  ^|  Paper 26.1.2
echo  ==========================================
echo.

:: ── 1. Verify Java is installed ─────────────────────────────
if not exist "%JAVA_EXE%" (
    echo [ERROR] Java not found at: %JAVA_EXE%
    pause & exit /b 1
)
if not exist "%JAVAC_EXE%" (
    echo [ERROR] javac not found at: %JAVAC_EXE%
    pause & exit /b 1
)

:: Check Java version
for /f "tokens=*" %%v in ('"%JAVAC_EXE%" -version 2^>^&1') do set JAVA_VER_STR=%%v
echo [INFO]  Compiler: %JAVA_VER_STR%

:: ── 2. Verify Paper API jar ──────────────────────────────────
if not exist "%PAPER_JAR%" (
    echo.
    echo [ERROR] Paper jar not found at: %PAPER_JAR%
    echo.
    echo  Steps to fix:
    echo   1. Create a folder called  libs\
    echo   2. Download Paper 26.1.2 from:
    echo        https://papermc.io/downloads/paper
    echo   3. Rename the downloaded file to  paper.jar
    echo   4. Place it in the  libs\  folder
    echo.
    pause & exit /b 1
)

:: ── 2b. Verify the real API jar exists ──────────────────────
::  Paper 26.x stores the API jar (org.bukkit.*, io.papermc.*, net.kyori.*) at:
::    libraries\io\papermc\paper\paper-api\26.1.2.build.65-stable\...jar
::  This is extracted automatically when Paper server runs for the first time.
if not exist "%API_JAR%" (
    echo.
    echo [ERROR] API jar not found: %API_JAR%
    echo.
    echo  Run the Paper server once to extract libraries:
    echo    "%JAVA_EXE%" -jar libs\paper.jar --nogui
    echo  Wait for "Done", then type: stop
    echo  Then re-run compile.cmd
    echo.
    pause ^& exit /b 1
)
echo [INFO]  Using API jar: %API_JAR%

:: ── 3. Create output directories ────────────────────────────
if exist "%BUILD_DIR%" rd /s /q "%BUILD_DIR%"
mkdir "%BUILD_DIR%"
if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"

:: ── 4. Collect all .java source files ───────────────────────
echo [INFO]  Scanning sources in %SRC_DIR%\...
dir /s /b "%SRC_DIR%\*.java" > build\sources.txt 2>nul

for /f %%c in ('type build\sources.txt ^| find /c /v ""') do set FILE_COUNT=%%c
echo [INFO]  Found %FILE_COUNT% source file(s).

if "%FILE_COUNT%"=="0" (
    echo [ERROR] No .java files found under %SRC_DIR%\
    pause & exit /b 1
)

:: ── 5. Compile ───────────────────────────────────────────────
echo [INFO]  Compiling (targeting Java 25 for server compatibility, Paper 26.1.2)...
set FULL_CP=%CD%\%API_JAR%;%CD%\%ADV%\adventure-api\4.26.1\adventure-api-4.26.1.jar;%CD%\%ADV%\adventure-key\4.26.1\adventure-key-4.26.1.jar;%CD%\%ADV%\adventure-text-serializer-plain\4.26.1\adventure-text-serializer-plain-4.26.1.jar;%CD%\%ADV%\examination-api\1.3.0\examination-api-1.3.0.jar;%CD%\%GUAVA_JAR%;%CD%\%BUNGEE_JAR%
"%JAVAC_EXE%" --release 25 -encoding UTF-8 -cp "%FULL_CP%" -d "%BUILD_DIR%" @build\sources.txt

if errorlevel 1 (
    echo.
    echo [ERROR] Compilation failed. See errors above.
    pause & exit /b 1
)
echo [OK]    Compilation successful.

:: ── 6. Copy resources (plugin.yml, etc.) ────────────────────
echo [INFO]  Copying resources...
copy /y "%SRC_DIR%\resources\plugin.yml" "%BUILD_DIR%\plugin.yml" >nul
echo [OK]    plugin.yml copied.

:: ── 7. Package into JAR ─────────────────────────────────────
echo [INFO]  Packaging JAR...
"%JAR_EXE%" --create --file "%CD%\%OUTPUT_DIR%\%JAR_NAME%" -C "%CD%\%BUILD_DIR%" .

if errorlevel 1 (
    echo [ERROR] JAR packaging failed.
    pause & exit /b 1
)

:: ── 8. Done ──────────────────────────────────────────────────
echo.
echo  ==========================================
echo   SUCCESS:  %OUTPUT_DIR%\%JAR_NAME%
echo  ==========================================
echo.
echo  Next steps:
echo    1. Copy  %OUTPUT_DIR%\%JAR_NAME%  to your server's  plugins\  folder
echo    2. Restart (or reload) the server
echo    3. In-game: /orbitalstrike give
echo    4. Place the Orbital Strike block and right-click it
echo.
pause
endlocal
