#!/bin/bash


CHANNEL_NAME=$1
CHAINCODE_NAME=$2
ORGS=$3

REQUEST_NAME=request_${CHANNEL_NAME}_${CHAINCODE_NAME}

rm -rf REQUEST_NAME.json

cat >> tmp.json  << EOL
{
  "name": "${CHAINCODE_NAME}",
  "version": "1.0",
  "contractType": "cnft-1.0",
  "channelName": "${CHANNEL_NAME}",
  "parties": [
EOL
cat >> tmp.json  << EOL
    {
      "mspId": "$BANK",
      "role": "participants"
    },
EOL
  for org in $ORGS; do
    cat >> tmp.json  << EOL
    {
      "mspId": "$org",
      "role": "participants"
    },
EOL
done
awk 'NR>1 {print prev} {prev=$0} END {sub(/,$/,"", prev); print prev}' tmp.json > $REQUEST_NAME.json


cat >> $REQUEST_NAME.json  << EOL
  ],
  "initArgs": []
}
EOL
rm -rf tmp.json
