#!/bin/bash
set -e
FILE_PATH=$0
if [[ "$(uname)" == "Darwin" ]]; then
  DIR=$(greadlink -f ${FILE_PATH})
else
  DIR=$(readlink -f ${FILE_PATH})
fi

BASE_DIR=$(dirname $(dirname $DIR))

[ -f $BASE_DIR/logs/blockProcessor-stdout.log ]  && rm -f $BASE_DIR/logs/blockProcessor-stdout.log

. envs/envs.sh
. envs/env-sberbank.sh

export CNFT_GATE_ADDRESS=http://$1:${CNFT_GATE_PORT}
export ENDORSEMENT_TIMEOUT=3000
export PROCESSOR_TIMEOUT=1
export QUERYING_INTERVAL=1000
export CHANNEL_NAME=service
export CNFT_CHAINCODE=cnft
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export DB_SCHEMA=public
export DB_URL=jdbc:postgresql://0.0.0.0:5432/postgres

echo "Starting block-processor...."

nohup $BASE_DIR/services/block-processor/stage/bin/block-processor > $BASE_DIR/logs/blockProcessor-stdout.log &
