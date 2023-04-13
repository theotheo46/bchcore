#!/bin/bash
set -e

FILE_PATH=$0

if [[ "$(uname)" == "Darwin" ]]; then
  DIR=$(greadlink -f ${FILE_PATH})
else
  DIR=$(readlink -f ${FILE_PATH})
fi

BASE_DIR=$(dirname $(dirname $DIR))


[ -f $BASE_DIR/logs/peer.log ]  && rm -f $BASE_DIR/logs/peer.log

echo "Starting Peer...."

. envs/envs.sh
. envs/env-sberbank.sh

export FABRIC_LOGGING_SPEC=INFO
export CORE_PEER_TLS_ENABLED=true
export CORE_PEER_GOSSIP_USELEADERELECTION=true
export CORE_PEER_GOSSIP_ORGLEADER=false
export CORE_PEER_PROFILE_ENABLED=false
export CORE_PEER_TLS_CERT_FILE=$BASE_DIR/crypto-config/peerOrganizations/${ORG}.${DOMAIN}/peers/peer0.${ORG}.${DOMAIN}/tls/server.crt
export CORE_PEER_TLS_KEY_FILE=$BASE_DIR/crypto-config/peerOrganizations/${ORG}.${DOMAIN}/peers/peer0.${ORG}.${DOMAIN}/tls/server.key
export CORE_PEER_TLS_ROOTCERT_FILE=$BASE_DIR/crypto-config/peerOrganizations/${ORG}.${DOMAIN}/peers/peer0.${ORG}.${DOMAIN}/tls/ca.crt
export CORE_PEER_MSPCONFIGPATH=$BASE_DIR/crypto-config/peerOrganizations/${ORG}.${DOMAIN}/peers/peer0.${ORG}.${DOMAIN}/msp
export CORE_PEER_ID=peer0.${ORG}.${DOMAIN}
export CORE_PEER_ADDRESS=localhost:${PEER_PORT_1}
export CORE_PEER_LISTENADDRESS=localhost:${PEER_PORT_1}
export CORE_CHAINCODE_JAVA_RUNTIME=enterprisedlt/fabric-jar-env
export CORE_PEER_GOSSIP_BOOTSTRAP=localhost:${PEER_PORT_1}
export CORE_PEER_GOSSIP_EXTERNALENDPOINT=localhost:${PEER_PORT_1}
export CORE_PEER_LOCALMSPID=${ORG}
export GODEBUG=netdns=go
export CORE_PEER_FILESYSTEMPATH=$BASE_DIR/persistence
export FABRIC_CFG_PATH=$BASE_DIR/hlf/config

hlf/bin/peer node start > $BASE_DIR/logs/peer.log 2>&1 &

