#!/bin/bash

set -e

FILE_PATH=$0

if [[ "$(uname)" == "Darwin" ]]; then
  DIR=$(greadlink -f ${FILE_PATH})
else
  DIR=$(readlink -f ${FILE_PATH})
fi
BASE_DIR=$(dirname $(dirname $DIR))

[ -f $BASE_DIR/logs/osn.log ]  && rm -f $BASE_DIR/logs/osn.log

echo "Starting OSN...."

. envs/envs.sh
. envs/env-sberbank.sh

export FABRIC_LOGGING_SPEC=INFO
export ORDERER_GENERAL_LISTENADDRESS=localhost
export ORDERER_GENERAL_LISTENPORT=${OSN_PORT}
export ORDERER_FILELEDGER_LOCATION=$BASE_DIR/persistence
export ORDERER_GENERAL_GENESISMETHOD=file
export ORDERER_GENERAL_GENESISFILE=$BASE_DIR/channel-artifacts/genesis.block
export ORDERER_GENERAL_LOCALMSPID=OrdererMSP
export ORDERER_GENERAL_LOCALMSPDIR=$BASE_DIR/crypto-config/ordererOrganizations/${DOMAIN}/orderers/orderer-${ORG}.${DOMAIN}/msp
export ORDERER_GENERAL_TLS_ENABLED=true
export ORDERER_GENERAL_TLS_PRIVATEKEY=$BASE_DIR/crypto-config/ordererOrganizations/${DOMAIN}/orderers/orderer-${ORG}.${DOMAIN}/tls/server.key
export ORDERER_GENERAL_TLS_CERTIFICATE=$BASE_DIR/crypto-config/ordererOrganizations/${DOMAIN}/orderers/orderer-${ORG}.${DOMAIN}/tls/server.crt
export ORDERER_GENERAL_TLS_ROOTCAS=$BASE_DIR/crypto-config/ordererOrganizations/${DOMAIN}/orderers/orderer-${ORG}.${DOMAIN}/tls/ca.crt
export ORDERER_GENERAL_CLUSTER_CLIENTCERTIFICATE=$BASE_DIR/crypto-config/ordererOrganizations/${DOMAIN}/orderers/orderer-${ORG}.${DOMAIN}/tls/server.crt
export ORDERER_GENERAL_CLUSTER_CLIENTPRIVATEKEY=$BASE_DIR/crypto-config/ordererOrganizations/${DOMAIN}/orderers/orderer-${ORG}.${DOMAIN}/tls/server.key
export ORDERER_CA=$BASE_DIR/crypto-config/ordererOrganizations/${DOMAIN}/orderers/orderer-${ORG}.${DOMAIN}/msp/tlscacerts/tlsca.${DOMAIN}-cert.pem
export FABRIC_CFG_PATH=$BASE_DIR/hlf/config
export ORDERER_CONSENSUS_SNAPDIR=$BASE_DIR/persistence/orderer/etcdraft/snapshot
export ORDERER_CONSENSUS_WALDIR=$BASE_DIR/persistence/orderer/etcdraft/wal



nohup hlf/bin/orderer 2>&1 > $BASE_DIR/logs/osn.log &

