#!/bin/bash
cd "$(dirname "$0")"
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 LANG=C.UTF-8 LC_ALL=C.UTF-8 PATH=/usr/lib/jvm/java-21-openjdk-amd64/bin:$PATH
M3_DONOR_BUILD_DIR=/tmp/donorcat bash dm/donor-catalogue/verify.sh > /tmp/donorcat.log 2>&1; echo "merged verify rc=$?"; tail -3 /tmp/donorcat.log | cut -c1-150; grep -hE "checks=|PASS" /tmp/donorcat/*.log 2>/dev/null | cut -c1-120 | head -8; cat /tmp/donorcat/javadoc.log | head -5
