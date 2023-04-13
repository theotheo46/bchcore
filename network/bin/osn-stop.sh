#!/bin/bash

printf "Stopping OSN instances \t\t\t\t\t"
for pid in `ps -ef | grep "hlf/bin/orderer" | grep -v grep | awk '{print $2}' `
do
  kill $pid
done
printf "[DONE]\n"

