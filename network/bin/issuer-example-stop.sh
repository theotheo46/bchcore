#!/bin/bash

printf "Stopping IssuerExample instances \t\t\t"
for pid in `ps -ef | grep "IssuerExampleMain" | grep -v grep | awk '{print $2}' `
do
  kill $pid
done
printf "[DONE]\n"
