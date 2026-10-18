@echo off
rem Opens the GoCycle rider app in a new terminal window.
cd /d "%~dp0"
if exist "%USERPROFILE%\.jdks\openjdk-24\bin\java.exe" set "JAVA_HOME=%USERPROFILE%\.jdks\openjdk-24"
call mvn -q compile || exit /b 1
start "GoCycle - Rider" cmd /k mvn -q exec:java -Dexec.mainClass=gocycle.app.RiderApp
