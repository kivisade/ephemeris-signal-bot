SHELL:=/bin/bash

.SILENT: build run
.PHONY:  build run

ifeq ($(OS),Windows_NT)

build:
	GRADLE_OPTS="-Dfile.encoding=UTF-8" ./gradlew fatJar

run:
	JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8" java -jar build/libs/ephemeris-signal-bot-all.jar

else

build:
	./gradlew fatJar

run:
	java -jar build/libs/ephemeris-signal-bot-all.jar

endif
