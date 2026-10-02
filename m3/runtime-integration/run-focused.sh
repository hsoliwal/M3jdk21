#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
root=$(cd "$(dirname "$0")/../.." && pwd)
: "${M3_TEST_JDK:?Set matched candidate}"
: "${M3_BOOT_JDK:?Set original private Java21 compiler}"
: "${M3_OWNER_SRC:?Set exact verified #7498 shared owner source closure}"
result=${M3_RESULTS:-$root/m3/build/runtime-integration}
mkdir -p "$result/classes"
mapfile -t owners < <(find "$M3_OWNER_SRC" -name '*.java' ! -name SharedArrayNative.java)
"$M3_BOOT_JDK/bin/javac" -Xlint:all -Werror -d "$result/classes" "${owners[@]}" "$root"/m3/runtime-integration/tests/*.java "$root/m3/runtime-stage1/tests/JoinJni.java"
gcc -std=c11 -Wall -Wextra -Werror -Wconversion -Wshadow -pedantic -shared -fPIC -I"$M3_BOOT_JDK/include" -I"$M3_BOOT_JDK/include/linux" "$root/m3/runtime-stage1/tests/join_jni.c" -o "$result/libjoinprobe.so"
gcc -std=c11 -Wall -Wextra -Werror -Wconversion -Wshadow -pedantic -shared -fPIC -I"$M3_BOOT_JDK/include" -I"$M3_BOOT_JDK/include/linux" "$root/m3/runtime-segments/tests/segmented_jvmti.c" -o "$result/libsegmentsjvmti.so"
python3 "$root/m3/runtime-integration/build-lexicon.py" "$result/test.m3lex"
"$M3_BOOT_JDK/bin/java" -cp "$result/classes" SharedOwnerFixture "$result/shared-owner" > "$result/owner-fixture.log"
flags=(-ea -esa -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CreateCoredumpOnCrash --add-opens java.base/java.lang=ALL-UNNAMED -cp "$result/classes")
for mode in mapped local;do
 path="$result/test.m3lex";expected=true
 if [[ $mode == local ]];then path="$result/missing.m3lex";expected=false;fi
 timeout 60 "$M3_TEST_JDK/bin/java" "${flags[@]}" -Djdk.mindex.lexicon="$path" -Dm3.expect.lexicon="$expected" MIndexStringInvariant > "$result/invariant-$mode.log" 2>&1
 cat "$result/invariant-$mode.log"
done
for compact in + -;do
 for tier in mapped shared local;do
  lexicon="$result/missing.m3lex";opts=(-Dm3.expect.lexicon=false)
  if [[ $tier == mapped ]];then lexicon="$result/test.m3lex";opts=(-Dm3.expect.lexicon=true);fi
  if [[ $tier == shared ]];then lexicon="$result/shared-owner/arena.bin";opts=(-Dm3.expect.lexicon=true -Dm3.shared.owner=true);fi
  timeout 300 "$M3_TEST_JDK/bin/java" "${flags[@]}" -XX:"$compact"CompactStrings -Djdk.mindex.lexicon="$lexicon" "${opts[@]}" MIndexIntegration > "$result/integration-$tier-$compact.log" 2>&1
  cat "$result/integration-$tier-$compact.log"
 done
 timeout 300 "$M3_TEST_JDK/bin/java" "${flags[@]}" -XX:"$compact"CompactStrings -Xcheck:jni -Dm3.jni="$result/libjoinprobe.so" SegmentedJniProbe > "$result/jni-$compact.log" 2>&1
 cat "$result/jni-$compact.log"
done
timeout 180 "$M3_TEST_JDK/bin/java" "${flags[@]}" -Xcheck:jni -agentpath:"$result/libsegmentsjvmti.so" -Dm3.jvmti="$result/libsegmentsjvmti.so" SegmentedJvmti > "$result/jvmti.log" 2>&1
cat "$result/jvmti.log"
for gc in UseG1GC UseSerialGC UseZGC UseShenandoahGC;do
 timeout 240 "$M3_TEST_JDK/bin/java" "${flags[@]}" -Xmx256m -XX:+UnlockDiagnosticVMOptions -XX:+VerifyBeforeGC -XX:+VerifyAfterGC -XX:+"$gc" SegmentedConcurrent > "$result/gc-$gc.log" 2>&1
 cat "$result/gc-$gc.log"
done

timeout 60 "$M3_TEST_JDK/bin/java" "${flags[@]}" -Xcheck:jni -Djdk.mindex.lexicon="$result/shared-owner/arena.bin" -agentpath:"$result/libsegmentsjvmti.so" -Dm3.jni="$result/libjoinprobe.so" MappedNativeProbe > "$result/mapped-native.log" 2>&1
cat "$result/mapped-native.log"
timeout 60 "$M3_TEST_JDK/bin/java" "${flags[@]}" -Xmx128m WeakAdmissionProbe > "$result/weak-admission.log" 2>&1
cat "$result/weak-admission.log"
