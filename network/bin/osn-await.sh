#!/bin/bash
set -e
FILE_PATH=$0
if [[ "$(uname)" == "Darwin" ]]; then
  DIR=$(greadlink -f ${FILE_PATH})
else
  DIR=$(readlink -f ${FILE_PATH})
fi
BASE_DIR=$(dirname $(dirname $DIR))


echo "Waiting for OSN to get ready..."
grep -m 1 "Raft leader changed" <(tail -n +1 -f $BASE_DIR/logs/osn.log)
