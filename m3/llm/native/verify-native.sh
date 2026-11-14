#!/bin/sh
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
set -eu
project=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
: "${JAVA_HOME:?Set JAVA_HOME to a complete JDK 21}"
mvn_command=${MVN:-mvn}
cc_command=${CC:-cc}
cxx_command=${CXX:-c++}
case $(uname -s) in
  Linux) jni_os=linux; library=libm3_llm_ncm.so; shared=-shared ;;
  Darwin) jni_os=darwin; library=libm3_llm_ncm.dylib; shared=-dynamiclib ;;
  *) printf '%s\n' 'This verification script supports Linux/macOS; use CMake for other platforms.' >&2; exit 2 ;;
esac
"$mvn_command" -f "$project/pom.xml" -DskipTests package
mkdir -p "$project/target/ncm-jni-headers" "$project/target/ncm-jni-classes" \
    "$project/target/ncm-native-c" "$project/target/ncm-native-cpp"
"$JAVA_HOME/bin/javac" --release 21 -Xlint:all -Werror \
    -h "$project/target/ncm-jni-headers" -d "$project/target/ncm-jni-classes" \
    -cp "$project/target/classes" \
    "$project/src/main/java/jdk/m3/llm/projection/M3NcmNativeOps.java"
for language in c cpp; do
  if [ "$language" = c ]; then compiler=$cc_command; standard=c17; syntax=c
  else compiler=$cxx_command; standard=c++20; syntax=c++; fi
  "$compiler" -x "$syntax" -std="$standard" -O2 -fPIC -fno-fast-math -ffp-contract=off \
      -Wall -Wextra -Werror -Wconversion -Wshadow -Wpedantic \
      -I "$JAVA_HOME/include" -I "$JAVA_HOME/include/$jni_os" \
      -include "$project/target/ncm-jni-headers/jdk_m3_llm_projection_M3NcmNativeOps.h" \
      "$shared" "$project/native/ncm_jni.c" -lm \
      -o "$project/target/ncm-native-$language/$library"
  "$mvn_command" -f "$project/pom.xml" \
      "-DargLine=-Xcheck:jni -Djava.library.path=$project/target/ncm-native-$language -Dncm.test.native.required=true" test
done
