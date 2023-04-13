#!/bin/bash
set -e
FILE_PATH=$0
if [[ "$(uname)" == "Darwin" ]]; then
  DIR=$(greadlink -f ${FILE_PATH})
else
  DIR=$(readlink -f ${FILE_PATH})
fi

BASE_DIR=$(dirname $(dirname $DIR))

[ -f $BASE_DIR/logs/remote-wallet.log ]  && rm -f $BASE_DIR/logs/remote-wallet.log

echo "Starting Wallet Remote...."

. envs/envs.sh
. envs/env-sberbank.sh

export LOG_LEVEL="DEBUG"
export GATE_URL="http://localhost:8981"
export WALLET_REMOTE_PORT="8983"
export WALLET_REMOTE_IDENTITY_OPERATIONS="hd"
export WALLET_REMOTE_ADDRESS_OPERATIONS="hd"

$BASE_DIR/distribution/wallet-remote/bin/wallet-remote > $BASE_DIR/logs/remote-wallet.log 2>&1 &
