@echo off
REM Maven Wrapper startup script for Windows

set "SCRIPT_DIR=%~dp0"
set "WRAPPER_JAR=%SCRIPT_DIR%.mvn\wrapper\maven-wrapper.jar"
set "WRAPPER_PROPERTIES=%SCRIPT_DIR%.mvn\wrapper\maven-wrapper.properties"

REM Check if wrapper JAR exists, if not download it
if not exist "%WRAPPER_JAR%" (
    echo Maven wrapper not found. Downloading...
    java -cp "%SCRIPT_DIR%.mvn\wrapper\MavenWrapperDownloader.java" MavenWrapperDownloader
)

REM Execute Maven wrapper
java -cp "%WRAPPER_JAR%" org.apache.maven.wrapper.MavenWrapperMain %*