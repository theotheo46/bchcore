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
export CORE_PEER_LOCALMSPID=${ORG}
export CORE_PEER_MSPCONFIGPATH=$BASE_DIR/crypto-config/peerOrganizations/${ORG}.${DOMAIN}/users/Admin@${ORG}.${DOMAIN}/msp
export ORDERER_CA=$BASE_DIR/crypto-config/ordererOrganizations/${DOMAIN}/orderers/orderer-${ORG}.${DOMAIN}/msp/tlscacerts/tlsca.${DOMAIN}-cert.pem
export CORE_PEER_ADDRESS=localhost:${PEER_PORT1}
export CORE_PEER_TLS_ROOTCERT_FILE=$BASE_DIR/crypto-config/peerOrganizations/${ORG}.${DOMAIN}/peers/peer0.${ORG}.${DOMAIN}/tls/ca.crt


hlf/bin/peer channel create -o localhost:${OSN_PORT} -c ${CHANNEL_NAME} -f $BASE_DIR/channel-artifacts/${CHANNEL_NAME}.tx --tls --cafile ${ORDERER_CA} --timeout 100s
rm -rfv $BASE_DIR/channel-artifacts/${CHANNEL_NAME}.block
mv -v ${CHANNEL_NAME}.block $BASE_DIR/channel-artifacts/${CHANNEL_NAME}.block
chmod -v a+rw $BASE_DIR/channel-artifacts/${CHANNEL_NAME}.block
