#!/bin/bash

printf "Stopping CNFTChainCode instances \t\t\t"
for pid in `ps -ef | grep "CNFTChainCode" | grep -v grep | awk '{print $2}' `
do
  kill $pid
done
printf "[DONE]\n"
