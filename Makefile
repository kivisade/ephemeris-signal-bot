SHELL:=/bin/bash

.SILENT: build build-windows run run-windows
.PHONY:  build build-windows run run-windows

build:
	gradlew fatJar

build-windows:
	GRADLE_OPTS="-Dfile.encoding=UTF8" ./gradlew fatJar

run:
	java -jar build/libs/ephemeris-signal-bot-all.jar

run-windows:
	JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF8" java -jar build/libs/ephemeris-signal-bot-all.jar
