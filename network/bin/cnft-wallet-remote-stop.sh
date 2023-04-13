#!/bin/bash

printf "Stopping WalletRemote instances \t\t\t"
for pid in `ps -ef | grep "CNFTWalletRemoteMain" | grep -v grep | awk '{print $2}' `
do
  kill $pid
done
printf "[DONE]\n"