#!/usr/bin/env bash

cat ${1} | awk -F, '
    {
        if (NR==1) {
           for (i = 1; i < NF + 1; i++){
               header[i] = $i
           }
        } else {
           ZOO_ID = NR - 1
           KFK_ID = NR - 2
           out = sprintf("./envs/env-%s.sh", $1)
           print "#!/bin/sh" > out
           printf("export KFK_ID=%s\n",     KFK_ID) >> out
           printf("export ZOO_ID=%s\n",     ZOO_ID) >> out
           for (i = 1; i < NF + 1; i++){
               printf("export %s=%s\n", header[i], $i) >> out
           }
           close(out)
           print $1
        }
    }
    '
