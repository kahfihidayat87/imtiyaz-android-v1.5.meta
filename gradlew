#!/usr/bin/env sh
#
# Copyright 2015 the original author or authors.
#
# Licensed under the Apache License, Version 2.0
#
set -e
APP_HOME=$(cd "${0%/*}" && pwd -P)
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar
if [ ! -f "$CLASSPATH" ]; then
  echo "Downloading gradle wrapper jar..."
  mkdir -p $(dirname $CLASSPATH)
  # Use gradle directly if wrapper jar missing (Github Actions has gradle)
  exec gradle "$@"
else
  exec java -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
fi
