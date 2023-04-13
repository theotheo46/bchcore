#!/bin/bash
set -e
FILE_PATH=$0
if [[ "$(uname)" == "Darwin" ]]; then
  DIR=$(greadlink -f ${FILE_PATH})
else
  DIR=$(readlink -f ${FILE_PATH})
fi

BASE_DIR=$(dirname $(dirname $DIR))

[ -f $BASE_DIR/logs/gate.log ]  && rm -f $BASE_DIR/logs/gate.log

echo "Starting Gate...."

. envs/envs.sh
. envs/env-sberbank.sh

export ORG=$ORG
export ORDERER_HOST=localhost
export ORDERER_PORT=${OSN_PORT}
export ORDERER_KEY_CRT=$BASE_DIR/crypto-config/ordererOrganizations/$DOMAIN/orderers/orderer-$ORG.$DOMAIN/tls/server.crt
export PEER_HOST=localhost
export PEER_PORT=${PEER_PORT_1}
export PEER_KEY_CRT=$BASE_DIR/crypto-config/peerOrganizations/$ORG.$DOMAIN/peers/peer0.$ORG.$DOMAIN/tls/server.crt
export PEER_DISCOVERY=true
export USER_CRT_PATH=$BASE_DIR/crypto-config/peerOrganizations/$ORG.$DOMAIN/users/User1@$ORG.$DOMAIN/msp/signcerts/User1@$ORG.$DOMAIN-cert.pem
export USER_KEY_PATH=$BASE_DIR/crypto-config/peerOrganizations/$ORG.$DOMAIN/users/User1@$ORG.$DOMAIN/msp/keystore/sk.pem
export CNFT_GATE_PORT=${CNFT_GATE_PORT}
export ENDORSEMENT_TIMEOUT=3000
export QUERYING_INTERVAL=1000
export CHANNEL_NAME=cnft
export CNFT_CHAINCODE=cnft
export CNFT_GATE_ID=${1}
export LOG_LEVEL="TRACE"
export MAX_BATCH_SIZE=1024
export BATCH_AWAIT_BEFORE="300ms"
export MAX_INBOUND_MESSAGE_SIZE=100

$BASE_DIR/distribution/gate/bin/gate-impl -Dfile.encoding=US-ASCII > $BASE_DIR/logs/gate.log 2>&1 &
