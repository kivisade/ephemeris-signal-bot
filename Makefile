SHELL:=/bin/bash

.SILENT: build run run-windows
.PHONY:  build run run-windows

build:
	gradlew fatJar

run:
	java -jar build/libs/ephemeris-signal-bot-all.jar

run-windows:
	JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF8" java -jar build/libs/ephemeris-signal-bot-all.jar
