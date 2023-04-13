#!/bin/bash

set -e

export SBT_OPTS="-XX:+CMSClassUnloadingEnabled -Xmx4G -Xms2G -Xss10m"

#Absolute path to repositories
REPOS_PATH=${1:-..}

pushd ${REPOS_PATH}
  sbt --warn compile
  sbt --warn stage
#  sbt --warn makeNPM
popd

rm -rf ./distribution
mkdir ./distribution

./distrcopy.sh

echo "Success!"
