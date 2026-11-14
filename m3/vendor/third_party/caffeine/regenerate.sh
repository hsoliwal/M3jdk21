#!/usr/bin/env bash
# Regenerate the build-generated sources of com.github.ben-manes.caffeine:caffeine:3.1.8
# exactly as upstream's Gradle tasks `generateLocalCaches` + `generateNodes`
# (caffeine/build.gradle.kts @ tag v3.1.8) produce them.
#
# Upstream tasks (caffeine/build.gradle.kts), both JavaExec on the `javaPoet` source set:
#   generateLocalCaches: mainClass com.github.benmanes.caffeine.cache.LocalCacheFactoryGenerator
#                        args build/generated-sources/local
#   generateNodes:       mainClass com.github.benmanes.caffeine.cache.NodeFactoryGenerator
#                        args build/generated-sources/nodes
#   classpath = javaPoet runtimeClasspath: javapoet 1.13.0, guava 32.1.2-jre (+ failureaccess
#     1.0.1, jsr305 3.0.2, checker-qual 3.33.0, error_prone_annotations 2.18.0, j2objc 2.8),
#     commons-lang3 3.13.0, google-java-format 1.17.0 (gradle/libs.versions.toml)
#   jvmArgs = DisableStrongEncapsulationJvmArgs (gradle/plugins/.../ProjectExtensions.kt)
#   toolchain = JDK 11 (release.yml JAVA_VERSION=11); this script runs JDK 21, javac --release 11.
#
# Clock pin: each generator stamps Year.now(ZoneId.of("America/Los_Angeles")) into the
# license header of every generated file. The release build ran 2023-08-09 (tag commit
# 2023-08-09T04:46:32Z; Maven Central upload 2023-08-09T06:13:33Z), i.e. year 2023. A tiny
# -javaagent (source embedded below) redirects only that call to
# Year.now(Clock.fixed(PIN_INSTANT, zone)); no other generator bytecode is changed.
# PIN_CLOCK=0 runs the generators unpinned (output then differs only in the header year).
#
# Usage: regenerate.sh [--check]   (--check: regenerate + verify hashes, do not touch generated/)
# Env:   JDK21_HOME, WORK, M2_REPO, PIN_INSTANT, PIN_CLOCK, JAVAPOET_SRC (optional local copy of
#        caffeine/src/javaPoet at $COMMIT; still hash-verified)
set -euo pipefail

COMMIT=b0723da5976ebb52069f6b0cccfcf44186c3fdf3   # refs/tags/v3.1.8 of github.com/ben-manes/caffeine
RAW="https://raw.githubusercontent.com/ben-manes/caffeine/${COMMIT}/caffeine/src/javaPoet"
CENTRAL="https://repo1.maven.org/maven2"

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JDK21_HOME="${JDK21_HOME:-C:/Program Files/Java/jdk-21}"
WORK="${WORK:-K:/m3-work/tmp/caffeine-gen-regen}"
M2_REPO="${M2_REPO:-K:/m3-work/m2-build}"
PIN_INSTANT="${PIN_INSTANT:-2023-08-09T04:46:32Z}"
PIN_CLOCK="${PIN_CLOCK:-1}"
CHECK_ONLY=0; [[ "${1:-}" == "--check" ]] && CHECK_ONLY=1
export TMP="${TMP:-K:/m3-work/tmp}" TEMP="${TEMP:-K:/m3-work/tmp}"
unset JDK_EA   # LocalCacheFactoryGenerator/NodeFactoryGenerator skip google-java-format if JDK_EA=true

native() { if command -v cygpath >/dev/null 2>&1; then cygpath -m "$1"; else printf '%s' "$1"; fi; }
sha() { sha256sum "$1" | cut -d' ' -f1; }
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';' ;; *) SEP=':' ;; esac

# path (relative to caffeine/src/javaPoet)<TAB>sha256 of upstream generator inputs at $COMMIT
INPUTS="java/com/github/benmanes/caffeine/cache/Feature.java	fc6773f28427cf3f644c3619095cfc216b22d4e113fc4dae33f39f7aaadc3ecf
java/com/github/benmanes/caffeine/cache/LocalCacheFactoryGenerator.java	290b13779249722143e6e3adf7986739c219161939c1e8e5bd0fb1e02e050ac9
java/com/github/benmanes/caffeine/cache/NodeFactoryGenerator.java	1fcf6bfee1dd58fb40fe527a6279d179861fa93d2d1733085392f424ca54afa6
java/com/github/benmanes/caffeine/cache/Specifications.java	086c31c2dfe04d1e6416d7e4977ab51749af1b0aa4850712547f50a77361a06b
java/com/github/benmanes/caffeine/cache/local/AddConstructor.java	d8f9e923301b95afaaf546b3e5b233e74d5724ef269670681a6e8d306720e81d
java/com/github/benmanes/caffeine/cache/local/AddDeques.java	205e79b9874d1a5239b093fe4c640d5a04091cc7b00fe368127afaf0741a797d
java/com/github/benmanes/caffeine/cache/local/AddExpirationTicker.java	0a193318683f109dd0bf05ef379160eaf57aa2a6644cc5539244742847b8c6af
java/com/github/benmanes/caffeine/cache/local/AddExpireAfterAccess.java	babe28a81b753850dbe154f9ad49e9e57aac80ecfb6b929d0b63f24cd3df9f40
java/com/github/benmanes/caffeine/cache/local/AddExpireAfterWrite.java	8edd568e15a7f69082746be1ffcf8f97864abfd0415c1446ccc238fa12dab9c0
java/com/github/benmanes/caffeine/cache/local/AddFastPath.java	dd0874873b7317fd85f844550742a216762dad6bf938eeddce1abaad2f37e3c7
java/com/github/benmanes/caffeine/cache/local/AddKeyValueStrength.java	ea4b8f4c599d86d556e80d81099f034a40264d3eb1beb7e8fd123c78224ca064
java/com/github/benmanes/caffeine/cache/local/AddMaximum.java	81540a3939514cc53514472df3a7c78bd92dbc05d61aa258b3ac0225289d06d3
java/com/github/benmanes/caffeine/cache/local/AddPacer.java	f1f76404834637207a84ab102a812083dc1832ce157d44512489bb8fed5976b0
java/com/github/benmanes/caffeine/cache/local/AddRefreshAfterWrite.java	6fa889b1b7d0623f695f2d9e10339848a9763f57150ff7be6e72eea6a9f8582e
java/com/github/benmanes/caffeine/cache/local/AddRemovalListener.java	32a42e1fd3e9af3fbd64416873d478440c0fa83b327c75e0cfe0395e48454cb0
java/com/github/benmanes/caffeine/cache/local/AddStats.java	7bedbfbf65143ad463bf18f69e909ca6dfd929ca60126fdb34e9ea98b16662a1
java/com/github/benmanes/caffeine/cache/local/AddSubtype.java	137c4d4e9f959ff5c793e050d87cfeacce84c4a3893a315af65c7b191a02b442
java/com/github/benmanes/caffeine/cache/local/Finalize.java	6f5709dd296523e490f88ffd1d89dd5e04ae9f30e17fe738f22db74235e0eb7c
java/com/github/benmanes/caffeine/cache/local/LocalCacheContext.java	1f663db1aaddb41979895eb14554b4a715494d286d4ec73e97dfa22180dd2bba
java/com/github/benmanes/caffeine/cache/local/LocalCacheRule.java	edf5e84f4971aa5de0561d9267caf03ba3e17d55aa6181f83e519dcda01d5fc3
java/com/github/benmanes/caffeine/cache/local/package-info.java	2721a6b285803aaa72d629c9758c65fd44d11be50e61ad9dfbc00937a22c7f67
java/com/github/benmanes/caffeine/cache/node/AddConstructors.java	cd151a91aea97881ddb0da7991030ed05e821f8f8bc905db4d1e92d9685d906b
java/com/github/benmanes/caffeine/cache/node/AddDeques.java	7e5b201f2608043fb71134832d3743fff3785372def1a4df31be999a1bc02d56
java/com/github/benmanes/caffeine/cache/node/AddExpiration.java	0890769029b688494e3b1595a8690186831312bd8e6be8b3bc79ac22377c0601
java/com/github/benmanes/caffeine/cache/node/AddFactoryMethods.java	ee900f5f11100115129fe7e9dd76ac29543f717e4ae3151c7b213d4af990e12e
java/com/github/benmanes/caffeine/cache/node/AddHealth.java	7e0e85d8bad27921670b9293b8dd6562c775f58b2db1a06a0db73d29dc7f8f39
java/com/github/benmanes/caffeine/cache/node/AddKey.java	48806fc751422e52f14c5ac443bc29df100eb969c73c5b34275f7e90c899ba6f
java/com/github/benmanes/caffeine/cache/node/AddMaximum.java	58293597e783bd789219c0b3824a860681725a89b02720176ebe6d4c655e7894
java/com/github/benmanes/caffeine/cache/node/AddSubtype.java	a39e330b8ab29a83e8ba0748ed9bb4612783d3767fd75276c005969a8e92fdea
java/com/github/benmanes/caffeine/cache/node/AddValue.java	13da1f12e8f1e736202196e598408832ed26e3d18d2c84d123cfdc14fbb980ee
java/com/github/benmanes/caffeine/cache/node/Finalize.java	6887e9cf759342c0605e5f30bc493cfc88a6e60a3db8ab3b645f3f1ae60f0e36
java/com/github/benmanes/caffeine/cache/node/NodeContext.java	840141365fc8eb852e84690abadd43359e77df91ed31c9885c04e54b3435a38b
java/com/github/benmanes/caffeine/cache/node/NodeRule.java	dd7360a29590ff6d3eba8de37b4d11b925b9db877ef346fb792c381dbb372b43
java/com/github/benmanes/caffeine/cache/node/package-info.java	041fff6c46bfb5f8c39bd06554bd780263ac5a7ff4a0d02f9454bfbe026dd7b0
java/com/github/benmanes/caffeine/cache/package-info.java	d3c9e66bab87d360c9dc002f7958b6955073ae4a8310cb496dc1304bb5b00757
resources/license.txt	b680d03dda6b7b4b10a6f580dcf3cd143551fe47f1728bd96dc7f512aa30ce3e"

# Maven layout path<TAB>sha256 of the generator runtime classpath (+ asm for the clock pin)
GEN_DEPS="com/squareup/javapoet/1.13.0/javapoet-1.13.0.jar	4c7517e848a71b36d069d12bb3bf46a70fd4cda3105d822b0ed2e19c00b69291
com/google/guava/guava/32.1.2-jre/guava-32.1.2-jre.jar	bc65dea7cfd9e4dacf8419d8af0e741655857d27885bb35d943d7187fc3a8fce
com/google/guava/failureaccess/1.0.1/failureaccess-1.0.1.jar	a171ee4c734dd2da837e4b16be9df4661afab72a41adaf31eb84dfdaf936ca26
com/google/code/findbugs/jsr305/3.0.2/jsr305-3.0.2.jar	766ad2a0783f2687962c8ad74ceecc38a28b9f72a2d085ee438b7813e928d0c7
org/checkerframework/checker-qual/3.33.0/checker-qual-3.33.0.jar	e316255bbfcd9fe50d165314b85abb2b33cb2a66a93c491db648e498a82c2de1
com/google/errorprone/error_prone_annotations/2.18.0/error_prone_annotations-2.18.0.jar	9e6814cb71816988a4fd1b07a993a8f21bb7058d522c162b1de849e19bea54ae
com/google/j2objc/j2objc-annotations/2.8/j2objc-annotations-2.8.jar	f02a95fa1a5e95edb3ed859fd0fb7df709d121a35290eff8b74dce2ab7f4d6ed
org/apache/commons/commons-lang3/3.13.0/commons-lang3-3.13.0.jar	82f528cf718c7a3c2f30fc5bc784e3c6a0a10b17605dadb9e16c82ede11e6064
com/google/googlejavaformat/google-java-format/1.17.0/google-java-format-1.17.0.jar	631ba54c39f6c20df027dc1420736df2e5e43c581880efdd1e46ddb4ce050e3e"
ASM_DEP="org/ow2/asm/asm/9.5/asm-9.5.jar	b62e84b5980729751b0458c534cf1366f727542bb8d158621335682a460f0353"

# upstream DisableStrongEncapsulationJvmArgs (google-java-format needs javac internals)
OPENS=(--add-exports jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED
  --add-exports jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED
  --add-exports jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED
  --add-exports jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED
  --add-exports jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED
  --add-exports jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED
  --add-exports jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED
  --add-exports jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED
  --add-opens jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED
  --add-opens jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED
  --add-opens jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED)

rm -rf "$WORK"; mkdir -p "$WORK/javaPoet" "$WORK/classes" "$WORK/agent/src/pin" "$WORK/agent/classes" \
  "$WORK/project" "$WORK/out"

# 1. upstream generator sources at the pinned commit, hash-verified
#    (JAVAPOET_SRC=<dir holding caffeine/src/javaPoet contents> copies instead of fetching)
while IFS=$'\t' read -r path want; do
  mkdir -p "$WORK/javaPoet/$(dirname "$path")"
  if [[ -n "${JAVAPOET_SRC:-}" ]]; then cp "$JAVAPOET_SRC/$path" "$WORK/javaPoet/$path"
  else timeout 120 curl -sSfL -o "$WORK/javaPoet/$path" "$RAW/$path"; fi
  got="$(sha "$WORK/javaPoet/$path")"
  [[ "$got" == "$want" ]] || { echo "INPUT_MISMATCH $path $got" >&2; exit 2; }
done <<< "$INPUTS"

# 2. build-time tool jars from Maven Central into $M2_REPO, hash-verified
fetch_dep() {
  local path="$1" want="$2" jar="$M2_REPO/$1"
  if [[ ! -f "$jar" ]]; then mkdir -p "$(dirname "$jar")"; timeout 120 curl -sSfL -o "$jar" "$CENTRAL/$path"; fi
  [[ "$(sha "$jar")" == "$want" ]] || { echo "DEP_MISMATCH $jar" >&2; exit 2; }
  printf '%s' "$(native "$jar")"
}
GEN_CP=""
while IFS=$'\t' read -r path want; do GEN_CP+="$(fetch_dep "$path" "$want")$SEP"; done <<< "$GEN_DEPS"
ASM_JAR="$(fetch_dep ${ASM_DEP%%$'\t'*} ${ASM_DEP##*$'\t'})"

# 3. compile the javaPoet source set (upstream: options.release = 11) + resources
find "$WORK/javaPoet/java" -name '*.java' | LC_ALL=C sort | while read -r f; do native "$f"; echo; done \
  > "$WORK/javapoet-sources.txt"
timeout 300 "$JDK21_HOME/bin/javac" -J-Xmx512m --release 11 -encoding UTF-8 -nowarn \
  -cp "$GEN_CP" -d "$(native "$WORK/classes")" "@$(native "$WORK/javapoet-sources.txt")"
cp "$WORK/javaPoet/resources/license.txt" "$WORK/classes/license.txt"

# 4. clock-pin agent (only rewrites Year.now(ZoneId) in com/github/benmanes/caffeine/cache/**)
cat > "$WORK/agent/src/pin/PinClockAgent.java" <<'JAVA'
package pin;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.time.Clock;
import java.time.Instant;
import java.time.Year;
import java.time.ZoneId;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Build-time clock pin for the caffeine JavaPoet generators. The generators stamp
 * {@code Year.now(ZoneId)} into the license header of every generated file; this agent
 * redirects exactly that call (in com/github/benmanes/caffeine/cache/** only) to
 * {@code Year.now(Clock.fixed(<instant>, zone))} so a rerun reproduces the year of the
 * upstream release build. No other generator bytecode is touched.
 * Usage: -javaagent:pin-clock-agent.jar=2023-08-09T04:46:32Z
 */
public final class PinClockAgent {
  private static final String OWNER = "pin/PinClockAgent";
  private static final String DESC = "(Ljava/time/ZoneId;)Ljava/time/Year;";
  private static volatile Instant instant;

  private PinClockAgent() {}

  public static void premain(String args, Instrumentation inst) {
    instant = Instant.parse(args);
    inst.addTransformer(new Transformer());
  }

  /** Replacement for {@code Year.now(ZoneId)} at the rewritten call sites. */
  public static Year now(ZoneId zone) {
    return Year.now(Clock.fixed(instant, zone));
  }

  private static final class Transformer implements ClassFileTransformer {
    @Override
    public byte[] transform(ClassLoader loader, String name, Class<?> redefined,
        ProtectionDomain domain, byte[] bytes) {
      if (name == null || !name.startsWith("com/github/benmanes/caffeine/cache/")) {
        return null;
      }
      ClassReader reader = new ClassReader(bytes);
      ClassWriter writer = new ClassWriter(reader, 0);
      int[] hits = {0};
      reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
        @Override
        public MethodVisitor visitMethod(int access, String mName, String mDesc,
            String signature, String[] exceptions) {
          MethodVisitor mv = super.visitMethod(access, mName, mDesc, signature, exceptions);
          return new MethodVisitor(Opcodes.ASM9, mv) {
            @Override
            public void visitMethodInsn(int op, String owner, String n, String d, boolean itf) {
              if (op == Opcodes.INVOKESTATIC && "java/time/Year".equals(owner)
                  && "now".equals(n) && DESC.equals(d)) {
                hits[0]++;
                super.visitMethodInsn(Opcodes.INVOKESTATIC, OWNER, "now", DESC, false);
              } else {
                super.visitMethodInsn(op, owner, n, d, itf);
              }
            }
          };
        }
      }, 0);
      if (hits[0] == 0) {
        return null;
      }
      System.err.println("[pin-clock] " + name + ": Year.now(ZoneId) call sites pinned="
          + hits[0] + " instant=" + instant);
      return writer.toByteArray();
    }
  }
}
JAVA
timeout 120 "$JDK21_HOME/bin/javac" -J-Xmx256m --release 11 -cp "$ASM_JAR" \
  -d "$(native "$WORK/agent/classes")" "$(native "$WORK/agent/src/pin/PinClockAgent.java")"
printf 'Premain-Class: pin.PinClockAgent\n' > "$WORK/agent/MANIFEST.MF"
( cd "$WORK/agent" && timeout 120 "$JDK21_HOME/bin/jar" --create --file pin-clock-agent.jar \
    --manifest MANIFEST.MF -C classes . )

# 5. the two upstream JavaExec launches (cwd = project dir, relative output args as upstream)
JVM_OPTS=(-Xms32m -Xmx1g -XX:+UseSerialGC "-Djava.io.tmpdir=$(native "$TMP")" "${OPENS[@]}")
[[ "$PIN_CLOCK" == "1" ]] && JVM_OPTS+=("-javaagent:$(native "$WORK/agent/pin-clock-agent.jar")=$PIN_INSTANT")
RUN_CP="$(native "$WORK/classes")$SEP$GEN_CP$ASM_JAR"
for task in "LocalCacheFactoryGenerator local" "NodeFactoryGenerator nodes"; do
  set -- $task
  ( cd "$WORK/project" && timeout 300 "$JDK21_HOME/bin/java" "${JVM_OPTS[@]}" -cp "$RUN_CP" \
      "com.github.benmanes.caffeine.cache.$1" "build/generated-sources/$2" )
  # codeGen source set = union of generated-sources/local + generated-sources/nodes
  ( cd "$WORK/project/build/generated-sources/$2" && find . -type f ) | while read -r f; do
    [[ -e "$WORK/out/$f" ]] && { echo "COLLISION $f" >&2; exit 3; }
    mkdir -p "$WORK/out/$(dirname "$f")"; cp "$WORK/project/build/generated-sources/$2/$f" "$WORK/out/$f"
  done
done

# 6. verify output byte-for-byte against GENERATED_SOURCES.json files[] (paths relative to
#    generated/; and no extra files)
( cd "$WORK/out" && find . -type f | sed 's|^\./||' | LC_ALL=C sort | xargs sha256sum | sed 's/ \*/  /' ) \
  > "$WORK/actual.sha256"
echo "GENERATED $(wc -l < "$WORK/actual.sha256") files -> $WORK/out (pin_clock=$PIN_CLOCK)"
if [[ -f "$HERE/GENERATED_SOURCES.json" ]]; then
  python -I -c 'import json,sys
d=json.load(open(sys.argv[1],encoding="utf-8"))
print("\n".join(sorted("%s  %s"%(f["sha256"],f["path"]) for f in d["files"])))' \
    "$(native "$HERE/GENERATED_SOURCES.json")" | tr -d '\r' > "$WORK/expected.sha256"
  diff <(LC_ALL=C sort -k2 "$WORK/expected.sha256") <(LC_ALL=C sort -k2 "$WORK/actual.sha256") \
    || { echo "OUTPUT_MISMATCH" >&2; exit 3; }
  echo "VERIFIED $(wc -l < "$WORK/expected.sha256") generated sources against GENERATED_SOURCES.json"
else
  echo "NO GENERATED_SOURCES.json next to this script; verification skipped" >&2
fi

# 7. install into the overlay
if [[ "$CHECK_ONLY" -eq 0 ]]; then
  rm -rf "$HERE/generated"; mkdir -p "$HERE/generated"; cp -r "$WORK/out/com" "$HERE/generated/"
  echo "INSTALLED $HERE/generated"
fi
