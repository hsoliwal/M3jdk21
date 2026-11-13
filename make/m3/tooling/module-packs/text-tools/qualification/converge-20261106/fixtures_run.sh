#!/bin/bash
# fixtures_run.sh <label> : run M3JDK 21's own text-tools proofs exactly as build.py does (modes, flags, pass markers)
# against g3-<label> (modules + natives from gate3.sh). Prints PASS/FAIL per proof.
cd "$(dirname "$0")"
L=$1; W=g3-$L; J="C:/Program Files/Java/jdk-21/bin"
MP="$(cd $W && ls -d cls/jdk.m3.* | tr '\n' ';')"
BASE=(--patch-module java.base=cls/java.base --module-path "$MP" --add-modules jdk.m3.text,jdk.m3.io,jdk.m3.structured -Djava.library.path=nat)
run() { # marker, logname, args...
  local marker=$1 log=$2; shift 2
  (cd $W && "$J/java" "${BASE[@]}" -cp fx "$@" > $log 2>&1)
  if grep -q "$marker" $W/$log; then echo "PASS $L $log"; else echo "FAIL $L $log: $(grep -m1 -E 'Exception|Error' $W/$log | cut -c1-140)"; fi
}
run "M3_NATIVE_TEXT_PARITY_PASS" differential.log M3NativeTextDifferential
for mode in native java; do
  run "M3_TEXT_KERNEL_REGRESSION_PASS mode=$mode" kernels-$mode.log --add-opens jdk.m3.text/jdk.m3.text=ALL-UNNAMED \
    -Dm3.text.native.disable=$([ $mode = java ] && echo true || echo false) M3TextKernelRegression $mode
done
for mode in line-load-denied literal-property-denied literal-load-denied; do
  run "M3_OPTIONAL_NATIVE_FALLBACK_PASS mode=$mode" fallback-$mode.log --add-opens jdk.m3.text/jdk.m3.text=ALL-UNNAMED \
    -Djava.security.manager=allow -Dm3.text.native.disable=false M3OptionalNativeFallbackRegression $mode
done
