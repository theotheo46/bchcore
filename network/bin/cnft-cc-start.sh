#!/bin/bash
set -e
FILE_PATH=$0
if [[ "$(uname)" == "Darwin" ]]; then
  DIR=$(greadlink -f ${FILE_PATH})
else
  DIR=$(readlink -f ${FILE_PATH})
fi

BASE_DIR=$(dirname $(dirname $DIR))

[ -f $BASE_DIR/logs/chaincode.log ]  && rm -f $BASE_DIR/logs/chaincode.log

echo "Starting CNFT CC ..."

. envs/envs.sh
. envs/env-sberbank.sh

export ORG=$ORG
export LOG_LEVEL="TRACE"
export CHAINCODE_SERVER_PORT=3737
export CORE_CHAINCODE_ID_NAME="$PACKAGE_ID"
#export CORE_PEER_ADDRESS=localhost:${PEER_PORT_1}

$BASE_DIR/distribution/chaincode/bin/chaincode-impl > $BASE_DIR/logs/chaincode.log 2>&1 &
