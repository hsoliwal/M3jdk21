#!/bin/bash
cd "$(dirname "$0")"
export LANG=C.UTF-8 LC_ALL=C.UTF-8 PATH=/usr/lib/jvm/java-21-openjdk-amd64/bin:$PATH
bash stage_pre/m3/tooling/companion/verify.sh > verify_pre.log 2>&1; echo "pre rc=$?"; diff verify_pre.log verify.log && echo IDENTICAL_OUTPUT
