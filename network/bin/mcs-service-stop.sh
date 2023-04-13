#!/bin/bash

printf "Stopping megacuksservice instances \t\t\t"
for pid in `ps -ef | grep "megacuksservice" | grep -v grep | awk '{print $2}' `
do
  kill $pid
done
printf "[DONE]\n"
