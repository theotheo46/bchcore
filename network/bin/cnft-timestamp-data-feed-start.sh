#!/bin/bash
set -e
FILE_PATH=$0
if [[ "$(uname)" == "Darwin" ]]; then
  DIR=$(greadlink -f ${FILE_PATH})
else
  DIR=$(readlink -f ${FILE_PATH})
fi

BASE_DIR=$(dirname $(dirname $DIR))

[ -f $BASE_DIR/logs/timestamp-data-feed.log ]  && rm -f $BASE_DIR/logs/timestamp-data-feed.log

echo "Starting Timestamp Data Feed...."

. envs/envs.sh
. envs/env-sberbank.sh

export LOG_LEVEL="DEBUG"
export GATE_URL="http://localhost:8981"
export ADMIN_WALLET_URL="http://localhost:8983"

$BASE_DIR/distribution/timestamp-data-feed/bin/timestamp-data-feed > $BASE_DIR/logs/timestamp-data-feed.log 2>&1 &
