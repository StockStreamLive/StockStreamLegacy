#!/bin/bash
git pull --rebase
./gradlew fatJar
runcommand='java -Xms8g -Xmx8g -jar build/libs/twitch-investbot-1.0-SNAPSHOT-all.jar'
export DISPLAY=":0.0"
nohup $runcommand $@ &
