#!/bin/bash

echo "curling 10 requests"

for i in {1..10}; do
    curl -s -X POST -H "Content-Type: application/json" localhost:8981/token-id --data-raw '[{"require":'1',"keys":["MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEWgufvvEhaGQwqtxGPD6N2QyADTGxwa+KOIjHAzwsLF5O5MyDMcZ8qQe3G/+EyAZQfBTpRbetcCgZ5jfQ50mOlg=="]}]' &
done

wait