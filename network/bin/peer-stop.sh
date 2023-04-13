#!/bin/bash

printf "Stopping Peer instances \t\t\t\t"
for pid in `ps -ef | grep "hlf/bin/peer node start" | grep -v grep | awk '{print $2}' `
do
  kill $pid
done
printf "[DONE]\n"

