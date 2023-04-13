#!/bin/bash
set -e
WALLET_PORT=8881

### Register token type
echo "Register token type"
curl -s -X POST -H "Authorization: sber" -H "Content-Type: application/json" localhost:${WALLET_PORT}/wallet-service/token-types --data-raw '{"tokenTypeName":"SBC","tokenMeta":"[]"}'
echo ""
