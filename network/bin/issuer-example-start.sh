#!/bin/bash
set -e
FILE_PATH=$0
if [[ "$(uname)" == "Darwin" ]]; then
  DIR=$(greadlink -f ${FILE_PATH})
else
  DIR=$(readlink -f ${FILE_PATH})
fi

BASE_DIR=$(dirname $(dirname $DIR))

[ -f $BASE_DIR/logs/issuer-example.log ]  && rm -f $BASE_DIR/logs/issuer-example.log

export TOKEN_TYPE="SBC"
export GATE_URL="http://localhost:8981"
export BLOCK_POLLING_INTERVAL=1000
export ORG_NAME="issuerOrg"

$BASE_DIR/distribution/issuer-example/bin/issuer-example > $BASE_DIR/logs/issuer-example.log 2>&1 &