#!/bin/bash
set -e
FILE_PATH=$0
if [[ "$(uname)" == "Darwin" ]]; then
  DIR=$(greadlink -f ${FILE_PATH})
else
  DIR=$(readlink -f ${FILE_PATH})
fi
BASE_DIR=$(dirname $(dirname $DIR))


echo "Waiting for Wallet Remote to get ready..."
grep -m 1 "CNFTWalletRemoteMain\$ - Started" <(tail -n +1 -f $BASE_DIR/logs/remote-wallet.log)
