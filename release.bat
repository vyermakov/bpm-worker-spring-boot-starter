@echo off

REM Set JAVA_HOME to JDK 21
set "JAVA_HOME=C:\Program Files\Java\jdk-21"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo Using Java version:
java -version
echo.

echo Starting Maven release process...
echo This will:
echo  1. Update version from SNAPSHOT to release version
echo  2. Build and test the project
echo  3. Sign artifacts with GPG
echo  4. Deploy to Maven Central
echo  5. Tag the release in Git
echo  6. Update to next SNAPSHOT version
echo.

echo Step 1: Preparing release...
call mvnw release:clean release:prepare -P release
if %errorlevel% neq 0 (
    echo Release preparation failed!
    pause
    exit /b %errorlevel%
)

echo.
echo Step 2: Performing release...
call mvnw release:perform -P release
if %errorlevel% neq 0 (
    echo Release perform failed!
    pause
    exit /b %errorlevel%
)

echo.
echo Release completed successfully!
echo Please verify the artifacts in Maven Central staging repository.
echo.

pause
