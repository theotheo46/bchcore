#!/bin/bash

#Absolute path to repositories
REPOS_PATH=${1:-..}

rm -rf ./distribution
mkdir ./distribution

cp -r ${REPOS_PATH}/chaincode/chaincode-impl/target/universal/stage ./distribution/chaincode
cp -r ${REPOS_PATH}/gate/gate-impl/target/universal/stage ./distribution/gate
cp -r ${REPOS_PATH}/wallet-remote/target/universal/stage ./distribution/wallet-remote
cp -r ${REPOS_PATH}/timestamp-data-feed/target/universal/stage ./distribution/timestamp-data-feed
cp -r ${REPOS_PATH}/issuer-example/target/universal/stage ./distribution/issuer-example
cp -r ${REPOS_PATH}/migration-tests/target/universal/stage ./distribution/migration_tests
cp -r ${REPOS_PATH}/megacuksservice/target/universal/stage ./distribution/megacuksservice
cp -r ${REPOS_PATH}/wallet-generator/target/universal/stage ./distribution/wallet-gen
echo  '{"path":"./chaincode/","type":"external","label":"cnft_1"}' > ./distribution/chaincode/metadata.json
echo '{ "address": "localhost:3737", "dial_timeout": "10s", "tls_required": false, "client_auth_required": false, "client_key": "", "client_cert": "", "root_cert": "" }' > ./distribution/chaincode/connection.json

pushd ./distribution/chaincode
  tar -zcf code.tar.gz bin lib connection.json
  tar -zcf cnft-external.tgz code.tar.gz metadata.json
  rm code.tar.gz
popd

echo "copyed...!"
