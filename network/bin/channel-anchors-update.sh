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

export FABRIC_CFG_PATH=$BASE_DIR/hlf/config
export ORDERER_CA=$BASE_DIR/crypto-config/ordererOrganizations/${DOMAIN}/orderers/orderer-${ORG}.${DOMAIN}/msp/tlscacerts/tlsca.${DOMAIN}-cert.pem
export CORE_PEER_LOCALMSPID=${ORG}
export CORE_PEER_ADDRESS=localhost:7151
export CORE_PEER_TLS_ENABLED=true
export CORE_PEER_TLS_ROOTCERT_FILE=$BASE_DIR/crypto-config/peerOrganizations/${ORG}.${DOMAIN}/peers/peer0.${ORG}.${DOMAIN}/tls/ca.crt
export CORE_PEER_MSPCONFIGPATH=$BASE_DIR/crypto-config/peerOrganizations/${ORG}.${DOMAIN}/users/Admin@${ORG}.${DOMAIN}/msp


hlf/bin/peer channel update -o localhost:7150 -c ${CHANNEL_NAME} --tls true --cafile ${ORDERER_CA} -f $BASE_DIR/channel-artifacts/${ORG}-anchors.tx
