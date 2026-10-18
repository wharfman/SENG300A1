#!/usr/bin/env sh
cd "$(dirname "$0")" || exit 1
[ -x "$HOME/.jdks/openjdk-24/bin/java" ] && export JAVA_HOME="$HOME/.jdks/openjdk-24"
exec mvn -q compile exec:java -Dexec.mainClass=gocycle.app.RiderApp
