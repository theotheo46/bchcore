#!/bin/bash

set -e
FILE_PATH=$0
if [[ "$(uname)" == "Darwin" ]]; then
  DIR=$(greadlink -f ${FILE_PATH})
else
  DIR=$(readlink -f ${FILE_PATH})
fi
BASE_DIR=$(dirname $(dirname $DIR))
. $BASE_DIR/envs/envs.sh
. $BASE_DIR/envs/env-sberbank.sh

export FABRIC_CFG_PATH=$PWD/hlf/config
#
export CORE_PEER_LOCALMSPID=sberbank
export CORE_PEER_TLS_ENABLED=true
export CORE_PEER_TLS_ROOTCERT_FILE=$PWD/crypto-config/peerOrganizations/${ORG}.${DOMAIN}/peers/peer0.${ORG}.${DOMAIN}/tls/ca.crt
export CORE_PEER_MSPCONFIGPATH=$PWD/crypto-config/peerOrganizations/${ORG}.${DOMAIN}/users/Admin@${ORG}.${DOMAIN}/msp
export CORE_PEER_ADDRESS=localhost:7151

hlf/bin/peer lifecycle chaincode checkcommitreadiness --tls true  --channelID cnft --name cnft --version 1.0 --sequence 1
