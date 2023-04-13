#!/bin/bash
set -e
FILE_PATH=$0
if [[ "$(uname)" == "Darwin" ]]; then
  DIR=$(greadlink -f ${FILE_PATH})
else
  DIR=$(readlink -f ${FILE_PATH})
fi
BASE_DIR=$(dirname $(dirname $DIR))


echo "Waiting for Timestamp Data Feed to get ready..."
grep -m 1 "CNFTTimestampDataFeedMain\$ - Started" <(tail -n +1 -f $BASE_DIR/logs/timestamp-data-feed.log)
