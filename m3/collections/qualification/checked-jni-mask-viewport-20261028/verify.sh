#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Checked-JNI parity for the unchanged current-master M3 primitive mask viewport.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/../../../.."
: "${JAVA_HOME:?Java 21 installation and JNI headers required}"
command -v cc >/dev/null || { echo "Native C compiler required" >&2; exit 1; }
command -v mvn >/dev/null || { echo "Maven required" >&2; exit 1; }

verify_blob() {
  local expected=$1 path=$2 actual
  actual=$(git hash-object "$path")
  if [[ "$actual" != "$expected" ]]; then
    echo "REFUSAL unreviewed native/Java owner: $path $actual != $expected" >&2
    exit 1
  fi
}

verify_blob b0d387f2612a315c6b53a50a814432c916ca61c8 \
  m3/collections/src/main/c/m3maskviewport.c
verify_blob 4f73ddb9aae36dadb8003257d3a6988536a18066 \
  m3/collections/src/main/java/jdk/m3/collections/M3MaskViewportNative.java
verify_blob 5966bffacc730b3ef7102d615a2331a42ae4a22b \
  m3/collections/src/main/java/jdk/m3/collections/M3MaskViewportKernel.java
verify_blob 27d1b0f38164369f49919f83bfc1a960061fa043 \
  m3/collections/src/test/java/jdk/m3/collections/M3MaskViewportCheckedJniContract.java

build=$(mktemp -d)
trap 'rm -rf -- "$build"' EXIT
cc -std=c11 -O1 -Wall -Wextra -Werror -Wpedantic \
  -fsanitize=undefined -fno-sanitize-recover=all -shared -fPIC \
  -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux" \
  m3/collections/src/main/c/m3maskviewport.c -o "$build/libm3maskviewport.so"

# The Java owner is a real first-party Maven module, not a mocked proxy.
mvn -B -ntp -f m3/arrays/pom.xml -Dmaven.test.skip=true install
mvn -B -ntp -f m3/collections/pom.xml -DskipTests test-compile

classpath="m3/collections/target/classes:m3/collections/target/test-classes:m3/arrays/target/classes"
for collector in G1GC SerialGC; do
  "$JAVA_HOME/bin/java" -Xcheck:jni "-XX:+Use$collector" \
    -cp "$classpath" jdk.m3.collections.M3MaskViewportCheckedJniContract \
    "$build/libm3maskviewport.so"
done
echo "PASS current-master 3-export checked JNI parity on G1/Serial with UBSan C"
echo "NOTE full JDK java.base/jtreg and all JNI/OS/architecture profiles remain separate"
