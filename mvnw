#!/bin/sh
#
# Maven Wrapper startup script
#
# This script will download the Maven wrapper JAR if needed and then
# execute Maven with the provided arguments.
#

# Resolve the directory of this script
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
WRAPPER_JAR="$SCRIPT_DIR/.mvn/wrapper/maven-wrapper.jar"
WRAPPER_PROPERTIES="$SCRIPT_DIR/.mvn/wrapper/maven-wrapper.properties"

# Check if wrapper JAR exists, if not download it
if [ ! -f "$WRAPPER_JAR" ]; then
    echo "Maven wrapper not found. Downloading..."
    java -cp "$SCRIPT_DIR/.mvn/wrapper/MavenWrapperDownloader.java" MavenWrapperDownloader
fi

# Execute Maven wrapper
exec java -cp "$WRAPPER_JAR" org.apache.maven.wrapper.MavenWrapperMain "$@"