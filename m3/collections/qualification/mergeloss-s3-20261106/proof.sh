#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Cases 10 + 14 proof (Linux, JDK 21, gcc) on the staged master owners: the dropped code's own contracts run against
# master's later implementations; the held #2547 declaration is a duplicate of master's.
# usage: TMPDIR=<tmp> JAVA_HOME=<jdk21> proof.sh <staged-dir>
set -uo pipefail
D=$(cd "$1" && pwd)
JDK=${JAVA_HOME:?}; export PATH=$JDK/bin:$PATH
JO="-Djava.io.tmpdir=${TMPDIR:-/tmp}"
F="-std=c17 -O2 -Wall -Wextra -Wpedantic -Werror -fPIC -shared -I$JDK/include -I$JDK/include/linux"
cd "$D" || exit 9; rm -rf out; mkdir -p out/classes out/hdr out/held out/lib out/tree/src/main/c out/tree/src/test/native out/tree2/src/main/c out/tree2/src/test/native
echo "== javac -h master viewport owners + origin contracts (--release 21 -Xlint:all,-module -Werror)"
javac -J$JO --release 21 -proc:none -Xlint:all,-module -Werror -h out/hdr -d out/classes master/jdk/m3/collections/*.java && echo "javac OK"
python3 - out/hdr native/master_m3maskviewport.c project projectPage projectFramePage <<'PY'
import re, sys, pathlib
hdr, src, names = pathlib.Path(sys.argv[1]), pathlib.Path(sys.argv[2]).read_text(), sys.argv[3:]
heads = ''.join(p.read_text() for p in sorted(hdr.glob('*.h')))
norm = lambda s: re.sub(r'\s+', ' ', s).replace(' *', '*').strip()
for name in names:
    h = re.search(r'JNIEXPORT (\w+) JNICALL (Java_\w+_' + name + r')\s*\(([^)]*)\);', heads)
    c = re.search(r'JNIEXPORT (\w+) JNICALL\s*(Java_\w+_' + name + r')\(([^)]*)\)\s*\{', src)
    ct = [norm(re.sub(r'\w+$', '', p.strip())) for p in c.group(3).split(',')] if c else []
    ht = [norm(p) for p in h.group(3).split(',')] if h else []
    print(('EQUAL ' if h and c and h.group(1) == c.group(1) and ht == ct else 'DIFFERENT ') + name + ' (' + str(len(ht)) + ' params)')
PY
echo "== gcc master m3maskviewport.c"
gcc $F native/master_m3maskviewport.c -o out/lib/libm3maskviewport.so 2> out/gcc.log && echo "gcc OK diagnostics=$(grep -cE 'warning:|error:' out/gcc.log) exports=$(nm -D --defined-only out/lib/libm3maskviewport.so | grep -c ' Java_')"
echo "== case 10 origin contract M3ViewportFrameContract (#1766, 3f4e51ad80a) on master Java + native"
for vm in default xint; do o=(); [ $vm = xint ] && o=(-Xint)
  java $JO "${o[@]}" -cp out/classes jdk.m3.collections.M3ViewportFrameContract 2>&1 | tail -1 | sed "s/^/$vm java: /"
  java $JO -Xcheck:jni "${o[@]}" -Dm3.native.path=$D/out/lib/libm3maskviewport.so -cp out/classes jdk.m3.collections.M3ViewportFrameContract --native > out/frame-$vm.out 2> out/frame-$vm.err; echo "$vm native: $(tail -1 out/frame-$vm.out) jni_warnings=$(grep -ci warning out/frame-$vm.err)"
done
echo "== case 14 origin contract M3MaskViewportWindowContract (d0cec971780, live on master) on master native"
for vm in default xint; do o=(); [ $vm = xint ] && o=(-Xint)
  java $JO -Xcheck:jni "${o[@]}" -Dm3.native.path=$D/out/lib/libm3maskviewport.so -cp out/classes jdk.m3.collections.M3MaskViewportWindowContract > out/window-$vm.out 2> out/window-$vm.err; echo "$vm: $(tail -1 out/window-$vm.out) jni_warnings=$(grep -ci warning out/window-$vm.err)"
done
echo "== case 14 origin instrumented-JNI test m3_mask_viewport_regions.c (live on master)"
cp native/m3_mask_viewport_regions.c out/tree/src/test/native/; cp native/master_m3maskviewport.c out/tree/src/main/c/m3maskviewport.c
gcc -std=c17 -O2 -Wall -Wextra -I$JDK/include -I$JDK/include/linux out/tree/src/test/native/m3_mask_viewport_regions.c -o out/regions-master 2> out/regions-master.log && echo "against master: compiled $(out/regions-master)" || echo "against master: COMPILE FAILED $(grep -m1 -oE 'error: .*' out/regions-master.log)"
cp native/m3_mask_viewport_regions.c out/tree2/src/test/native/; sed 's/Java_com_m3_collections_/Java_jdk_m3_collections_/' native/d0cec971780_m3maskviewport.c > out/tree2/src/main/c/m3maskviewport.c
gcc -std=c17 -O2 -Wall -Wextra -I$JDK/include -I$JDK/include/linux out/tree2/src/test/native/m3_mask_viewport_regions.c -o out/regions-origin 2> out/regions-origin.log && echo "against origin d0cec971780: $(out/regions-origin)" || echo "against origin: COMPILE FAILED $(grep -m1 -oE 'error: .*' out/regions-origin.log)"
echo "== #2547 held M3MaskViewportNative (pr1766) as one method set with master's kernel"
if javac -J$JO --release 21 -proc:none -Xlint:all,-module -Werror -d out/held held/jdk/m3/collections/*.java > out/held.log 2>&1; then echo "held compiles"; else echo "held: $(grep -m1 -oE 'error: .*' out/held.log)"; fi
echo "master projectFramePage $(javap -J$JO -p -s -cp out/classes jdk.m3.collections.M3MaskViewportNative | grep -A1 'projectFramePage' | tail -1 | sed 's/^ *//')"
echo "projectFramePage declarations: held $(grep -c 'static native int projectFramePage' held/jdk/m3/collections/M3MaskViewportNative.java), master $(grep -c 'static native int projectFramePage' master/jdk/m3/collections/M3MaskViewportNative.java)"
echo "== pinned-pointer use (GetIntArrayElements) master vs origins"
for f in master_m3maskviewport.c 3f4e51ad80a_m3maskviewport.c d0cec971780_m3maskviewport.c; do echo "$f GetIntArrayElements=$(grep -c GetIntArrayElements native/$f) GetIntArrayRegion=$(grep -c GetIntArrayRegion native/$f)"; done
