#!/bin/bash

printf "Stopping CNFTGate instances \t\t\t\t"
for pid in `ps -ef | grep "CNFTGateMain" | grep -v grep | awk '{print $2}' `
do
  kill $pid
done
printf "[DONE]\n"


