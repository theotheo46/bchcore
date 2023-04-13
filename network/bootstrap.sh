#!/bin/bash

. envs/envs.sh
. envs/env-sberbank.sh

[ -d "./logs" ]  && rm -rf logs
mkdir logs

IP_ADDRESS=$1

./bin/osn-start.sh
./bin/osn-await.sh

./bin/peer-start.sh
./bin/peer-await.sh

./bin/channel-create.sh
./bin/channel-join.sh
#./bin/channel-anchors-update.sh
#
./bin/channel-cc-install.sh
export PACKAGE_ID=$(./bin/channel-cc-query-installed.sh)
echo $PACKAGE_ID
#
./bin/mcs-service-start.sh

./bin/cnft-cc-start.sh

./bin/channel-cc-approve-for-my-org.sh $PACKAGE_ID
./bin/channel-cc-check-commit-readiness.sh
./bin/channel-cc-commit-chaincode-definition.sh
./bin/channel-cc-query-commited.sh
#
./bin/cnft-gate-start.sh "gate-0"
./bin/cnft-gate-await.sh

while getopts 'p' flag; do
  case "${flag}" in
    p) echo "Starting Postgress DB Container" && ./bin/storage-start.sh ;;
   esac
done

# Generate crypto for wallet remote:
export WALLET_CRYPTO=`./distribution/wallet-gen/bin/wallet-generator 2>/dev/null`

echo "========================================="
echo $WALLET_CRYPTO
echo "========================================="

./bin/cnft-wallet-remote-start.sh
./bin/cnft-wallet-remote-await.sh

#
#./bin/mk-nginx-config.sh
#./bin/nginx-start.sh
./bin/issuer-example-start.sh

# Generate crypto for timestamp feed:
export WALLET_CRYPTO=`./distribution/wallet-gen/bin/wallet-generator 2>/dev/null`

echo "========================================="
echo $WALLET_CRYPTO
echo "========================================="

./bin/cnft-timestamp-data-feed-start.sh
./bin/cnft-timestamp-data-feed-await.sh
