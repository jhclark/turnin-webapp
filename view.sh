#!/usr/bin/env bash
set -e

if [ $# != 2 ]; then
    echo >&2 "Usage: $0 keyFileIn receiptStr"
    exit 1
fi

scala -cp webapps/turnin/WEB-INF/classes View $1 $2
