#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../../../.."
classdir="$(mktemp -d)"
trap 'rm -rf "$classdir"' EXIT
javac --release 21 -proc:none -Xlint:all -Werror -d "$classdir" \
  m3/collections/src/main/java/com/m3/collections/M3SlotSpliterators.java \
  m3/collections/src/main/java/com/m3/collections/M3FilteredDeques.java \
  m3/collections/src/main/java/com/m3/collections/M3SlotSpliterators.java \
  m3/collections/verification/filtered-object-deques-20261017/M3FilteredDequeContract.java
java -cp "$classdir" com.m3.collections.M3FilteredDequeContract
