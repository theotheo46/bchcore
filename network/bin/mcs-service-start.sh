#!/bin/bash
set -e
FILE_PATH=$0
if [[ "$(uname)" == "Darwin" ]]; then
  DIR=$(greadlink -f ${FILE_PATH})
else
  DIR=$(readlink -f ${FILE_PATH})
fi

BASE_DIR=$(dirname $(dirname $DIR))

[ -f $BASE_DIR/logs/megacuksservice.log ]  && rm -f $BASE_DIR/logs/megacuksservice.log


export MEGACUKS_SERVER_PORT=9191
export MEGACUKS_SERVICE_URL="http://localhost:7766"
export MEGACUKS_VERIFY_SERVICE="verify_service"
export MEGACUKS_SYSTEM_ID="system_id"
export MEGACUKS_BSN_CODE="bsn_code"
export MEGACUKS_DEFAULT_KEY_SERVICE_ID="key_service_id"
export MEGACUKS_DEFAULT_KEY_SERVICE_CERTIFICATE_B64=""MIIDCjCCArmgAwIBAgITEgBdgOR4rLmKnl4zhwABAF2A5DAIBgYqhQMCAgMwfzEjMCEGCSqGSIb3DQEJARYUc3VwcG9ydEBjcnlwdG9wcm8ucnUxCzAJBgNVBAYTAlJVMQ8wDQYDVQQHEwZNb3Njb3cxFzAVBgNVBAoTDkNSWVBUTy1QUk8gTExDMSEwHwYDVQQDExhDUllQVE8tUFJPIFRlc3QgQ2VudGVyIDIwHhcNMjIwMTI3MTA1NjMyWhcNMjIwNDI3MTEwNjMyWjAPMQ0wCwYDVQQDDAR0ZXN0MGYwHwYIKoUDBwEBAQEwEwYHKoUDAgIkAAYIKoUDBwEBAgIDQwAEQO+KG8XaMBR7nWx76c0hkz+uve/oXtbFgJi++qmxz0qQnvf558PFvCv9mnHinAkGHqhIws+B71zDa0+5bMW8NYWjggF3MIIBczAPBgNVHQ8BAf8EBQMDB/AAMBMGA1UdJQQMMAoGCCsGAQUFBwMCMB0GA1UdDgQWBBRlbGJKjTNT4MOpJxCkQnpjB3D26DAfBgNVHSMEGDAWgBROgz4Uae/sXXqVK18R/jcyFklVKzBcBgNVHR8EVTBTMFGgT6BNhktodHRwOi8vdGVzdGNhLmNyeXB0b3Byby5ydS9DZXJ0RW5yb2xsL0NSWVBUTy1QUk8lMjBUZXN0JTIwQ2VudGVyJTIwMigxKS5jcmwwgawGCCsGAQUFBwEBBIGfMIGcMGQGCCsGAQUFBzAChlhodHRwOi8vdGVzdGNhLmNyeXB0b3Byby5ydS9DZXJ0RW5yb2xsL3Rlc3QtY2EtMjAxNF9DUllQVE8tUFJPJTIwVGVzdCUyMENlbnRlciUyMDIoMSkuY3J0MDQGCCsGAQUFBzABhihodHRwOi8vdGVzdGNhLmNyeXB0b3Byby5ydS9vY3NwL29jc3Auc3JmMAgGBiqFAwICAwNBAD825otHIlQkHCnEBwB5X3+fOTreY9cngp3YGC2espMos6pRfDx6aDxHJnx34gi/1Z7w5FmRFN2T4MxvEUEOEng=""

$BASE_DIR/distribution/megacuksservice/bin/megacuksservice > $BASE_DIR/logs/megacuksservice.log 2>&1 &
