#!/bin/sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
OUT=$(mktemp -d)
trap 'rm -rf "$OUT"' EXIT HUP INT TERM
javac --release 21 -Xlint:all -Werror -d "$OUT" \
  "$ROOT/src/main/java/com/m3/tooling/priority/M3LazyLongPriorityFrontier.java" \
  "$ROOT/src/test/java/com/m3/tooling/priority/M3LazyLongPriorityFrontierTest.java"
java -ea -cp "$OUT" com.m3.tooling.priority.M3LazyLongPriorityFrontierTest
