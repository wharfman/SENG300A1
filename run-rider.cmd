@echo off
cd /d "%~dp0"
if exist "%USERPROFILE%\.jdks\openjdk-24\bin\java.exe" set "JAVA_HOME=%USERPROFILE%\.jdks\openjdk-24"
call mvn -q compile exec:java -Dexec.mainClass=gocycle.app.RiderApp
