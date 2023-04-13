#!/bin/bash

set -e
IP_ADDRESS=$1
AUTH_TYPE=${2:-TEST}

# Choice your destiny:
# -TEST
# -JWT not tested
# -OAuth2

pushd ./auth-profiles
  openssl base64 -A -in ./externalHost.json -out ./externalHost.txt

  cat template/authDetails${AUTH_TYPE}Template.json | sed "s/@IP_ADDRESS@/$IP_ADDRESS/g" > ./authDetails${AUTH_TYPE}.json
  openssl base64 -A -in ./authDetails${AUTH_TYPE}.json -out ./authDetails${AUTH_TYPE}.txt

  export AUTH_DETAILS=$(cat authDetails${AUTH_TYPE}.txt)
  cat template/serviceProviderProfileTemplate.json | sed "s/@AUTH_DETAILS@/$AUTH_DETAILS/g" | sed "s/@AUTH_TYPE@/$AUTH_TYPE/g" > ./serviceProviderProfile.json
  openssl base64 -A -in ./serviceProviderProfile.json -out ./serviceProviderProfile.txt

popd
