#!/usr/bin/env bash
sbt server/stage
server/target/universal/stage/bin/server -Dplay.crypto.secret=tatata

