#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
root=$(cd "$(dirname "$0")/../../../.." && pwd)
python3 "$root/m3/collections/qualification/stored-representative-20261017/verify_receiver.py"
python3 "$root/m3/collections/qualification/stored-representative-20261017/test_receipt.py"
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
pkg="$root/m3/collections/src"
javac --release 21 -proc:none -Xlint:all,-serial -Werror -d "$work" \
  "$pkg/main/java/com/m3/collections/M3FilteredViews.java" \
  "$pkg/main/java/com/m3/collections/M3MutableFilteredViews.java" \
  "$pkg/test/java/com/m3/collections/M3StoredRepresentativeContract.java"
java -ea -cp "$work" com.m3.collections.M3StoredRepresentativeContract
java -Xint -ea -cp "$work" com.m3.collections.M3StoredRepresentativeContract
echo "M3_STORED_REP_RECEIVER_JAVA21_PASS compiled=2 sources oracle=1 interpretermode=pass"
