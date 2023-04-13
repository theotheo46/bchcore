#!/bin/bash

set -e

mkdir -p hlf
pushd hlf
  rm -rf bin config
  curl -sSL https://bit.ly/2ysbOFE | bash -s -- 2.2.0 -d -s
popd