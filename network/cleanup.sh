#!/bin/bash

echo "Stopping processes ..."

#docker stop nginx.sberbank.tokenization.platform
#docker rm nginx.sberbank.tokenization.platform
#docker volume prune -f

./bin/issuer-example-stop.sh
./bin/cnft-gate-stop.sh
./bin/cnft-wallet-remote-stop.sh
./bin/cnft-timestamp-data-feed-stop.sh
./bin/mcs-service-stop.sh
./bin/peer-stop.sh
./bin/cnft-cc-stop.sh
./bin/osn-stop.sh


echo "Cleaning files ..."
rm -rf ./logs/*
rm -rf ./persistence/*

echo "Cleanup complete."


