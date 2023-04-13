#!/bin/bash

set -e

. ./envs/envs.sh
export PROFILE_NAME=$1
export CHANNEL_NAME=cnft
export DOMAIN=tokenization.platform
export ORG_NAMES=""
export PATH=$PATH:$PWD/hlf/bin

function generateCerts() {
  rm -rf crypto-config
  which cryptogen
  if [[ "$?" -ne 0 ]]; then
    echo "cryptogen tool not found. exiting"
    exit 1
  fi
  echo
  echo "##########################################################"
  echo "##### Generate certificates using cryptogen tool #########"
  echo "##########################################################"

  if [ -d "crypto-config" ]; then
    rm -Rf crypto-config
  fi
  set -x
  cryptogen generate --config=./crypto-config.yaml
  res=$?
  set +x
  if [ $res -ne 0 ]; then
    echo "Failed to generate certificates..."
    exit 1
  fi
  echo "Copying sk keys ..."
  for ORG in $ORG_NAMES; do
    pushd ./crypto-config/peerOrganizations/${ORG}.${DOMAIN}/
    pushd ca
    cp -v *_sk sk.pem
    popd
    pushd tlsca
    cp -v *_sk sk.pem
    popd
    pushd users/Admin@${ORG}.${DOMAIN}/msp/keystore
    cp -v *_sk sk.pem
    popd
    pushd users/User1@${ORG}.${DOMAIN}/msp/keystore
    cp -v *_sk sk.pem
    popd
    popd

    cat template/fabric-ca-server-template.yaml | sed "s/@ORG@/$ORG/g" >./crypto-config/peerOrganizations/${ORG}.${DOMAIN}/ca/fabric-ca-server-config.yaml
  done
  echo "Success generateCerts"
}

function generateChannelArtifacts() {
  rm -rf channel-artifacts
  mkdir channel-artifacts

  which configtxgen
  if [ "$?" -ne 0 ]; then
    echo "configtxgen tool not found. exiting"
    exit 1
  fi

  echo "##########################################################"
  echo "#########  Generating Orderer Genesis block ##############"
  echo "##########################################################"
  set -x
  configtxgen -profile OrgsOrdererGenesis -outputBlock ./channel-artifacts/genesis.block -channelID sys-channel
  res=$?
  set +x
  if [[ $res -ne 0 ]]; then
    echo "Failed to generate orderer genesis block..."
    exit 1
  fi
  echo
  echo "#################################################################"
  echo "### Generating channel configuration transaction 'channel.tx' ###"
  echo "#################################################################"
  set -x
  configtxgen -profile OrgsChannel -outputCreateChannelTx ./channel-artifacts/$CHANNEL_NAME.tx -channelID $CHANNEL_NAME
  res=$?
  set +x
  if [ $res -ne 0 ]; then
    echo "Failed to generate channel configuration transaction..."
    exit 1
  fi

  for ORG in $ORG_NAMES; do
    echo
    echo "#################################################################"
    echo "#######    Generating anchor peer update for ${ORG}   ##########"
    echo "#################################################################"
    set -x
    configtxgen -profile OrgsChannel -outputAnchorPeersUpdate ./channel-artifacts/${ORG}-anchors.tx -channelID $CHANNEL_NAME -asOrg $ORG
    res=$?
    set +x
    if [ $res -ne 0 ]; then
      echo "Failed to generate anchor peer update for Org1MSP..."
      exit 1
    fi
  done
  echo "Success generateChannelArtifacts"
}

function generateEnvs() {
  rm -rf envs/!envs.sh

  echo "preparing envs"

  ORG_NAMES=$(./print-orgs.sh ${PROFILE_NAME})

  echo $ORG_NAMES >./envs/orgs.list

  for ORG in $ORG_NAMES; do
    . ./envs/env-${ORG}.sh

    hosts_file="./envs/hosts-from-${ORG}"
    if [[ $ORG_PIP != "127.0.0.1" ]]; then
      echo "# ${ORG}:" >$hosts_file
      echo "${ORG_PIP} orderer-${ORG}.${DOMAIN}" >>$hosts_file
      echo "${ORG_PIP} peer0.${ORG}.${DOMAIN}" >>$hosts_file
    fi

  done

  for ORG in $ORG_NAMES; do
    env_file="./envs/env-${ORG}.sh"
    echo "export DOMAIN=\"${DOMAIN}\"" >>$env_file
    echo "export HASHCASH_DIFFICULTY=15" >> $env_file

    # generate hosts for each org
    hosts_file="./envs/hosts-${ORG}"
    echo "127.0.0.1 localhost" >$hosts_file
    for h_org in $ORG_NAMES; do
      if [[ "$h_org" != "$ORG" ]] && [[ -f "./envs/hosts-from-${h_org}" ]]; then
        cat "./envs/hosts-from-${h_org}" >>$hosts_file
      fi
    done
  done

  for ORG in $ORG_NAMES; do
    rm -f "./envs/hosts-from-${ORG}"
  done

  echo "Success generateEnvs"
}


generateEnvs
generateCerts
generateChannelArtifacts

