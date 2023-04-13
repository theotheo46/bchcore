#!/bin/bash



./bin/osn-start.sh
./bin/osn-await.sh

./bin/peer-start.sh
./bin/peer-await.sh

./bin/cnft-gate-start.sh
./bin//cnft-gate-await.sh
