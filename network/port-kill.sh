#!/bin/bash

function delete_ports_for_linux {
    echo "KILL 8981 CNFT-GATE"
    fuser -n tcp -k -KILL 8981
    echo "KILL 7571"
    fuser -n tcp -k -KILL 7150
    echo "KILL 9443"
    fuser -n tcp -k -KILL 9443
    echo "KILL 7150"
    fuser -n tcp -k -KILL 7150
    echo "KILL 8881 wallet-service"
    fuser -n tcp -k -KILL 8881
    echo "KILL 5432 storage"
    fuser -n tcp -k -KILL 5432
    echo "KILL 8080 blockProcessor"
    fuser -n tcp -k -KILL 8080
    echo "KILL 8081"
    fuser -n tcp -k -KILL 8081
    echo "KILL 80 nginx"
    fuser -n tcp -k -KILL 80
    echo "delletesudo persistence"
    rm -rf ./persistence
}

function delete_ports_for_mac {
    echo "KILL 8981 CNFT-GATE"
    lsof -nti:8981 | xargs kill -9
    echo "KILL 7571"
    lsof -nti:7571 | xargs kill -9
    echo "KILL 9443"
    lsof -nti:9443 | xargs kill -9
    echo "KILL 7150"
    lsof -nti:7150 | xargs kill -9
    echo "KILL 8881 wallet-service"
    lsof -nti:8881 | xargs kill -9
    echo "KILL 5432 storage"
    lsof -nti:5432 | xargs kill -9
    echo "KILL 8080 blockProcessor"
    lsof -nti:8080 | xargs kill -9
    echo "KILL 8081"
    lsof -nti:8081 | xargs kill -9
    echo "KILL 80 nginx"
    lsof -nti:80 | xargs kill -9
    echo "delete persistence"
    rm -rf ./persistence
}

if [ "$(uname)" == "Darwin" ]; then
    delete_ports_for_mac
elif [ "$(expr substr $(uname -s) 1 5)" == "Linux" ]; then
    delete_ports_for_linux
fi
