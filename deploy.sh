#!/usr/bin/env bash
set -e
echo Copying sources
rsync --filter=':- .gitignore' -r . ec2-user@demo.thesamet.com:proto-user
echo Compiling...
ssh ec2-user@demo.thesamet.com -C "cd proto-user; sbt compile"

