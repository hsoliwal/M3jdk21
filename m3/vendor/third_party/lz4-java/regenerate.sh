#!/usr/bin/env bash
# Regenerate the 16 build-generated sources of org.lz4:lz4-java:1.8.0 exactly as
# upstream's Ant target `generate-sources` (build.xml @ tag 1.8.0) produces them.
#
# Upstream target (build.xml lines 133-146) is a forked JVM:
#   classpath = ivy:cachepath org.mvel:mvel2:2.3.2.Final (conf=default, transitive; no runtime deps)
#   classname = org.mvel2.sh.Main
#   dir       = ${src}/build/source_templates
#   sysproperty out.dir = ${build}/java
#   arg       = ${src}/build/gen_sources.mvel
# This script performs that same JVM launch without Ant/Ivy (Ant's `init` target would also
# resolve the junit4-ant test toolchain, which is irrelevant to generation).
#
# mvel2 2.3.2.Final references java.lang.Compiler (removed in JDK 21), so the generator JVM is
# JDK 17 by default. GEN_MODE=jdk21-shim instead runs JDK 21 with a one-class
# --patch-module java.base shim for java.lang.Compiler; both modes produce byte-identical output.
#
# Usage: regenerate.sh [--check]   (--check: regenerate + verify hashes, do not touch generated/)
# Env:   JDK17_HOME, JDK21_HOME, GEN_MODE (jdk17|jdk21-shim), WORK, M2_REPO
set -euo pipefail

COMMIT=78eae489e8163e4df56735a79dd8ef9c4f4ec056   # refs/tags/1.8.0 of github.com/lz4/lz4-java
RAW="https://raw.githubusercontent.com/lz4/lz4-java/${COMMIT}"
MVEL_SHA256=16b3d801a27ab50829f24b20d99938b84b52d9e2cc45dfedfb88ad5c7cad632e
MVEL_URL="https://repo1.maven.org/maven2/org/mvel/mvel2/2.3.2.Final/mvel2-2.3.2.Final.jar"

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JDK17_HOME="${JDK17_HOME:-C:/Program Files/Microsoft/jdk-17.0.19.10-hotspot}"
JDK21_HOME="${JDK21_HOME:-C:/Program Files/Java/jdk-21}"
GEN_MODE="${GEN_MODE:-jdk17}"
WORK="${WORK:-K:/m3-work/tmp/lz4-gen-regen}"
M2_REPO="${M2_REPO:-K:/m3-work/m2-build}"
CHECK_ONLY=0; [[ "${1:-}" == "--check" ]] && CHECK_ONLY=1
export TMP="${TMP:-K:/m3-work/tmp}" TEMP="${TEMP:-K:/m3-work/tmp}"

native() { if command -v cygpath >/dev/null 2>&1; then cygpath -m "$1"; else printf '%s' "$1"; fi; }
sha() { sha256sum "$1" | cut -d' ' -f1; }

# path<TAB>sha256 of upstream generator inputs at $COMMIT
INPUTS="build.xml	7598e2d11b32d876f1dafa58059e63a8a7bcc51f7b438904fc65647c8848efe2
src/build/gen_sources.mvel	a23489d8cf8256ca3664ab6b018e81f3620dcc6617cb2016fd3cf8d7e585b299
src/build/source_templates/compress.template	693420c0ed9ddb964b98b1957c7d68aec50d47e33860b51155378f0490524bb8
src/build/source_templates/compress_hc.template	ea24c57373ce196446533defc4aa4a69a48b9bddbd6d1dd6318a780b45cce9ea
src/build/source_templates/compressor.template	1bd61669604696541fb79c614ff88f50c130697f01fb79ed3887f6192e432841
src/build/source_templates/compressor_hc.template	10f42736526078a71db0d5ce841dd3071efe9db2fa83167fffdeb32c4f1eb0bb
src/build/source_templates/decompress.template	43decb3e3d6e4e6ee9a2763e11139ca792f328113e71f99bd0ea10e8ce206301
src/build/source_templates/decompressor.template	9541bfccd8097c106f0d3ae233f170813aa15bec79830638a8cccaec6f0b72c1
src/build/source_templates/hashtable.template	ea04c3c3656207ce824111f432cec5b38ac4de88121e034102f5b72a29e4d690
src/build/source_templates/xxhash32.template	ca645453140f6b29858e7c6b6ef7352db40670a43923b41fab1a02854f8963e1
src/build/source_templates/xxhash32_hash.template	4720e6559c91809d50ae9a63408ddf20f96bca22305b98f65e236655faa2f47d
src/build/source_templates/xxhash32_streaming.template	acd31214204fd1e9bf69d32ece8c9c99aa1174304e218f84ecbb968ac3d769fb
src/build/source_templates/xxhash64.template	06422f1c1ada058a854e0d42d29227e2123151ba12a8b333e4ded5098303157d
src/build/source_templates/xxhash64_hash.template	c0338f8d43b19ecdb9ee52272f72deeaf56b571d6b85b419d08c4e72c6d020b2
src/build/source_templates/xxhash64_streaming.template	bf5fbcee99e6a0f9044a9c289cec2829877d18896ac2412a22e443216e7e8f8b"

# sha256<2 spaces>path of the expected generator output (relative to out.dir)
EXPECTED="919ae4f0f73e696141a178baf0ed8da3004c5839fefd598c93b5abf2812f330b  net/jpountz/lz4/LZ4HCJavaSafeCompressor.java
7551f968d3fee52c6e7a3ce01ffa6a926aad57f8af0f7de42832252b8fbc17e0  net/jpountz/lz4/LZ4HCJavaUnsafeCompressor.java
28c7cda48e41c4dfefa40afb33dbb5b7df50d8288a664271fb2c7a7488fbef97  net/jpountz/lz4/LZ4JavaSafeCompressor.java
8b548e308ea983b9e7dfd639b594f0a63601f06cb2bf0440778bff7a78c217b6  net/jpountz/lz4/LZ4JavaSafeFastDecompressor.java
45b5214695f6f6821a9ad1d07a1efd6f5e3a62374deb9b21429ad1b1f42633f1  net/jpountz/lz4/LZ4JavaSafeSafeDecompressor.java
b795f6fc7a938a48d0a9d8406cf43a947552e7e556d0138386fcd14253124d29  net/jpountz/lz4/LZ4JavaUnsafeCompressor.java
8abf004fffdf625650268e1a8db69080853495e4e2c27e86d30e5db2c6bae104  net/jpountz/lz4/LZ4JavaUnsafeFastDecompressor.java
f878e721481ed4cbe2709b11cc952fe4590daffbeeb75d97e761520f34d6dd92  net/jpountz/lz4/LZ4JavaUnsafeSafeDecompressor.java
5d402247dfe43ba476cef633577c984ceb1581caef7b40ef3bcee04d18fe3a50  net/jpountz/xxhash/StreamingXXHash32JavaSafe.java
4a9f12d02129430c08a40ee83596f03e1b5b40d85ea51cbd996599c247189f81  net/jpountz/xxhash/StreamingXXHash32JavaUnsafe.java
94211af7fc122f29429ec807f805df5a2c731f69d7fc6e73db746c3a9bafabdd  net/jpountz/xxhash/StreamingXXHash64JavaSafe.java
0b40ebd437dacd639dc8559b32535a7cabc0a2b01f73b6aa4baecfb972180461  net/jpountz/xxhash/StreamingXXHash64JavaUnsafe.java
cd6a83edc1345d8fc211c75bfcab66590890c26a43aa55836fb0f515cc2725f1  net/jpountz/xxhash/XXHash32JavaSafe.java
ba56becf5f6f14caf43e333edd0a16bbb5e45c7853c873388b3c85f3e99b71eb  net/jpountz/xxhash/XXHash32JavaUnsafe.java
d9463f84d3e960db7a689da67f23b5d252413b5276f69555a38146035849cfae  net/jpountz/xxhash/XXHash64JavaSafe.java
4aabf5d0302d69dc17268ce4683cea148057a198009e1c8ef0e25b15cee85230  net/jpountz/xxhash/XXHash64JavaUnsafe.java"

rm -rf "$WORK"; mkdir -p "$WORK/upstream" "$WORK/out"

# 1. upstream inputs at the pinned commit, hash-verified
while IFS=$'\t' read -r path want; do
  mkdir -p "$WORK/upstream/$(dirname "$path")"
  timeout 120 curl -sSfL -o "$WORK/upstream/$path" "$RAW/$path"
  got="$(sha "$WORK/upstream/$path")"
  [[ "$got" == "$want" ]] || { echo "INPUT_MISMATCH $path $got" >&2; exit 2; }
done <<< "$INPUTS"

# 2. generator tool: org.mvel:mvel2:2.3.2.Final (build-time only), hash-verified
MVEL_JAR="$M2_REPO/org/mvel/mvel2/2.3.2.Final/mvel2-2.3.2.Final.jar"
if [[ ! -f "$MVEL_JAR" ]]; then
  mkdir -p "$(dirname "$MVEL_JAR")"; timeout 120 curl -sSfL -o "$MVEL_JAR" "$MVEL_URL"
fi
[[ "$(sha "$MVEL_JAR")" == "$MVEL_SHA256" ]] || { echo "MVEL_JAR_MISMATCH $MVEL_JAR" >&2; exit 2; }

# 3. the upstream <java fork="true"> launch
JVM_OPTS=(-Xms16m -Xmx256m -XX:+UseSerialGC "-Djava.io.tmpdir=$TMP")
case "$GEN_MODE" in
  jdk17) JAVA="$JDK17_HOME/bin/java" ;;
  jdk21-shim)
    JAVA="$JDK21_HOME/bin/java"
    mkdir -p "$WORK/shim/src/java/lang" "$WORK/shim/out"
    printf 'package java.lang;\npublic final class Compiler { private Compiler() {} }\n' \
      > "$WORK/shim/src/java/lang/Compiler.java"
    timeout 300 "$JDK21_HOME/bin/javac" -J-Xmx128m --patch-module "java.base=$(native "$WORK/shim/src")" \
      -d "$(native "$WORK/shim/out")" "$(native "$WORK/shim/src/java/lang/Compiler.java")"
    JVM_OPTS+=(--patch-module "java.base=$(native "$WORK/shim/out")") ;;
  *) echo "unknown GEN_MODE=$GEN_MODE" >&2; exit 2 ;;
esac
( cd "$WORK/upstream/src/build/source_templates" && timeout 300 "$JAVA" "${JVM_OPTS[@]}" \
    "-Dout.dir=$(native "$WORK/out")" -cp "$(native "$MVEL_JAR")" org.mvel2.sh.Main \
    "$(native "$WORK/upstream/src/build/gen_sources.mvel")" )

# 4. verify output byte-for-byte against the recorded hashes (and no extra files)
( cd "$WORK/out" && find . -type f | sed 's|^\./||' | LC_ALL=C sort | xargs sha256sum | sed 's/ \*/  /' ) \
  > "$WORK/actual.sha256"
diff <(printf '%s\n' "$EXPECTED" | LC_ALL=C sort -k2) <(LC_ALL=C sort -k2 "$WORK/actual.sha256") \
  || { echo "OUTPUT_MISMATCH" >&2; exit 3; }
echo "VERIFIED 16 generated sources ($GEN_MODE)"

# 5. install into the overlay
if [[ "$CHECK_ONLY" -eq 0 ]]; then
  rm -rf "$HERE/generated"; mkdir -p "$HERE/generated"; cp -r "$WORK/out/net" "$HERE/generated/"
  echo "INSTALLED $HERE/generated"
fi
