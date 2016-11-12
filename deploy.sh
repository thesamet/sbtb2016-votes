#!/usr/bin/env bash
set -e
echo Copying sources
rsync --filter=':- .gitignore' -r . ec2-user@demo.thesamet.com:proto-demo
echo Compiling...
ssh -t ec2-user@demo.thesamet.com -C "./session.sh"

