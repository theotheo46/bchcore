#!/bin/bash

set -e

rm -f bundle-no-docker.tgz

tar -zcvf bundle-no-docker.tgz bin buildpack chaincode envs hlf  *.sh *.yaml *.csv


echo "Success!"
