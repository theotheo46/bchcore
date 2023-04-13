#!/bin/bash

export COMPOSE_PROJECT_NAME=test_net_${ORG}_nginx
set -e
FILE_PATH=$0
if [[ "$(uname)" == "Darwin" ]]; then
  DIR=$(greadlink -f ${FILE_PATH})
else
  DIR=$(readlink -f ${FILE_PATH})
fi

BASE_DIR=$(dirname $(dirname $DIR))

[ -f $BASE_DIR/logs/nginx-stdout.log ]  && rm -f $BASE_DIR/logs/nginx-stdout.log

echo "Starting nginx...."

 bash -c "nginx -g 'daemon off;'" -p ../webapp/:/usr/share/nginx/html/ -p ../conf/nginx.conf:/etc/nginx/conf.d/default.conf


