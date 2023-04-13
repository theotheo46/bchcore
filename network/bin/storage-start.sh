#!/bin/sh

set -e
FILE_PATH=$0
if [[ "$(uname)" == "Darwin" ]]; then
  DIR=$(greadlink -f ${FILE_PATH})
else
  DIR=$(readlink -f ${FILE_PATH})
fi

BASE_DIR=$(dirname $(dirname $DIR))

[ -f $BASE_DIR/logs/storage-stdout.log ]  && rm -f $BASE_DIR/logs/storage-stdout.log

. envs/envs.sh
. envs/env-sberbank.sh

if [ -z "$(docker images -q 11.12-alpine)" ]; then
  docker pull postgres:11.12-alpine
fi

if [ -n "$(docker inspect -f '{{.Id}}' storage.$ORG.$DOMAIN 2>/dev/null)" ]; then
  docker rm -f storage.$ORG.$DOMAIN
fi


docker run -d --name storage.$ORG.$DOMAIN \
  -e "POSTGRES_PASSWORD=postgres" \
  --volume=$BASE_DIR/db-schema/:/docker-entrypoint-initdb.d/ \
  --network=bridge \
   -p 5432:5432 \
  postgres:11.12-alpine
