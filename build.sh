#!/usr/bin/env bash
set -e
set -o pipefail
set -u

mkdir -p webapps/turnin/WEB-INF/classes
scalac -cp $(ls lib/*.jar webapps/turnin/WEB-INF/lib/*.jar | awk '{printf "%s:",$0}') \
    -d webapps/turnin/WEB-INF/classes \
    src/*.scala