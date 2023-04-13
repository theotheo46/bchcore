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
#
export CORE_PEER_LOCALMSPID=${ORG}
export CORE_PEER_TLS_ENABLED=true
export CORE_PEER_TLS_ROOTCERT_FILE=$BASE_DIR/crypto-config/peerOrganizations/${ORG}.${DOMAIN}/peers/peer0.${ORG}.${DOMAIN}/tls/ca.crt
export CORE_PEER_MSPCONFIGPATH=$BASE_DIR/crypto-config/peerOrganizations/${ORG}.${DOMAIN}/users/Admin@${ORG}.${DOMAIN}/msp


hlf/bin/peer lifecycle chaincode queryinstalled --peerAddresses localhost:7151  --tlsRootCertFiles=${CORE_PEER_TLS_ROOTCERT_FILE} >&log.txt


PACKAGE_ID=$(sed -n "/${CC_NAME}_${CC_VERSION}/{s/^Package ID: //; s/, Label:.*$//; p;}" log.txt)
rm log.txt
echo $PACKAGE_ID
