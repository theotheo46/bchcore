#!/bin/bash

export COMPOSE_PROJECT_NAME=test_net_${ORG}_nginx

docker-compose -f ./template/nginx.yaml up -d


