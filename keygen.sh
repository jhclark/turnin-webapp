#!/usr/bin/env bash
set -e

if [ $# != 1 ]; then
    echo >&2 "Usage: $0 keyFileOut"
    exit 1
fi

scala -cp webapps/turnin/WEB-INF/classes KeyGen $1
