#!/bin/bash

printf "Stopping TimestampDataFeed instances \t\t\t"
for pid in `ps -ef | grep "CNFTTimestampDataFeedMain" | grep -v grep | awk '{print $2}' `
do
  kill $pid
done
printf "[DONE]\n"