#!/bin/bash

echo "Waiting for payment storage to get ready..."
grep -m 1 "database system is ready to accept connections" <(docker logs -f storage.$ORG.$DOMAIN 2>&1)
