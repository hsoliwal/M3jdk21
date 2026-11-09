#!/bin/bash
cd "$(dirname "$0")"
export LANG=C.UTF-8 LC_ALL=C.UTF-8 PATH=/usr/lib/jvm/java-21-openjdk-amd64/bin:$PATH
bash stage/m3/tooling/companion/verify.sh > verify.log 2>&1; echo "verify rc=$?"; cat verify.log | cut -c1-200
