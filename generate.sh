#!/usr/bin/env bash
set -e

if [ $# != 2 ]; then
    echo >&2 "Usage: $0 keyFileIn submissionFileIn"
    exit 1
fi

scala -cp webapps/turnin/WEB-INF/classes Generate $1 $2
