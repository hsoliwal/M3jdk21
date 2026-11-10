# Running the bounded recipe spectrum

This Maven task qualifies the existing FILE-local atomize, patternize and document recipes in
`hsoliwal/M3jdk21`. It uses the existing `MemJava` compiler and source-snapshot recipe. Product
source, JDK public interfaces, JNI ABI and the default migration-recipes coverage gate remain outside
its mutation scope. See [DESIGN.md](DESIGN.md) for the reviewed baseline and retained failures.

The publication baseline is master `da958d00d24154c0db87beca0ec80a7df2b43b73`. The immutable
100-file corpus remains pinned to `e8fdab9a7abc4b0273438ce9e5d0ffd6255b1247`; those files are
unchanged at the publication baseline. This task preserves the current shared snapshot helper,
whose one-argument constructor selects Java LST mode. Its newer explicit PlainText opt-in is not
used by this qualification lane.

## Ordered reproduction

Use Python 3, Java 21, Maven 3.9 or later, and the Checkstyle 12.3.1 all-in-one JAR. The recorded run
used Java 21.0.2 and Maven 3.9.12. Set `M3_CHECKSTYLE_JAR` to that JAR before running the following
Bash commands from the repository root. Every command must succeed before the next one runs:

```bash
set -euo pipefail
: "${M3_CHECKSTYLE_JAR:?Set this to the Checkstyle 12.3.1 all-in-one JAR}"
M3_SPECTRUM_ROOT="$PWD"
M3_SPECTRUM_OWNER="$M3_SPECTRUM_ROOT/m3/tooling/migration-recipes"
M3_SPECTRUM_TASK="$M3_SPECTRUM_OWNER/tasks/atom-spectrum-20261005"
M3_SPECTRUM_LOGS="$M3_SPECTRUM_TASK/target/verification"
mkdir -p "$M3_SPECTRUM_LOGS"
python3 "$M3_SPECTRUM_TASK/verify-static.py" \
  --corpus-root "$M3_SPECTRUM_ROOT" --out "$M3_SPECTRUM_LOGS" \
  > "$M3_SPECTRUM_LOGS/01-static.log" 2>&1
java -Xmx192m -jar "$M3_CHECKSTYLE_JAR" -c "$M3_SPECTRUM_TASK/checkstyle.xml" \
  "$M3_SPECTRUM_OWNER/src/main/java/com/m3/rewrite/atom" \
  "$M3_SPECTRUM_OWNER/src/main/java/com/synexia/rewrite/M3HashPinnedJavaSnapshotRecipe.java" \
  "$M3_SPECTRUM_OWNER/src/test/java/com/m3/rewrite/atom" \
  > "$M3_SPECTRUM_LOGS/02-lint.log" 2>&1
mvn -B -ntp -f "$M3_SPECTRUM_TASK/pom.xml" compile \
  > "$M3_SPECTRUM_LOGS/03-compile.log" 2>&1
mvn -B -ntp -f "$M3_SPECTRUM_TASK/pom.xml" test-compile \
  > "$M3_SPECTRUM_LOGS/04-test-compile.log" 2>&1
mvn -B -ntp -f "$M3_SPECTRUM_TASK/pom.xml" surefire:test \
  > "$M3_SPECTRUM_LOGS/05-all-tests.log" 2>&1
python3 "$M3_SPECTRUM_TASK/verify-runtime.py" \
  --static-receipt "$M3_SPECTRUM_LOGS/SOURCE_SHA256.tsv" \
  --corpus-root "$M3_SPECTRUM_ROOT" --out "$M3_SPECTRUM_LOGS" \
  > "$M3_SPECTRUM_LOGS/06-runtime-audit.log" 2>&1
```

The focused POM compiles eight existing production owners: the seven atom-package classes and
`M3HashPinnedJavaSnapshotRecipe`. It compiles and executes every test in the existing atom package,
including the retained 100-file generated atomizer corpus and original 54-expression project. The
current source closure contains twelve test sources and 39 JUnit cases, including both newly
upstream atom-package test classes. Its
OpenRewrite 8.17.1, JUnit 5.10.2, compiler 3.13.0 and Surefire 3.2.5 versions match the owning module.
The JUnit fork keeps a 768 MiB heap bound. Checkstyle is an explicit preceding gate; the focused POM
does not bind it or duplicate the default module's JaCoCo gate.

For an isolated source export, use its absolute path for both Python `--corpus-root` arguments and
add `-Dm3.spectrum.corpusRoot=/absolute/path/to/export` to all Maven commands. The export must contain
the exact manifest paths and bytes. A hash mismatch fails and requires a reviewed manifest refresh.

## Three distinct proof lanes

| Lane | Inputs | Executed checks | Limit |
| --- | --- | --- | --- |
| Generated recipe programs | 324 primitive-int expressions, 225 boundary pairs, all six leaf orders | Fresh parsing/type validation, strict compiler, frozen original and independent arithmetic oracle, effects/exceptions, class/member surface, opaque text, common fixed point | Finite admitted primitive-int domain |
| Individual-file source survey | 100 actual files: 50 java.base, 25 javac, 25 M3 | Exact size/SHA-256/Git-blob pins; parser/type admission; inventory and fixed-point checks only for admitted files | Refused files remain gaps; parsing and no-op results do not establish behavior |
| Real M3 source group | The same 25 pinned M3 files supplied together | Strict attribution, strict compilation of original sources, real PrefixZ on 515 UTF-16 inputs against an independent direct calculation | Original M3 sidecar behavior; no transformed real-source or JDK integration claim |

The individual-file survey retains 15 no-op fixed points, 14 parse refusals and 71 type refusals in
the recorded environment. The group lane resolves the 20 M3 attribution gaps by supplying their
already pinned sibling sources; it does not erase the original survey results. The PrefixZ corpus
contains all 511 binary strings of length zero through eight plus four NUL/surrogate cases.

`src/test/resources/com/m3/rewrite/atom/spectrum/CORPUS.tsv` is relative to the owning module. The
100 real files are read directly from the checkout and are never copied into the shipped recipe
resources. Their existing OpenJDK or M3 licensing headers stay with their original files. LeetCode,
HackerRank and GeeksforGeeks catalogue references do not authorize copying implementation code.

The generated receipts report every intermediate source hash and a complete zero-change sweep;
pass-limit exhaustion cannot qualify a candidate. Negative fixtures reject altered arithmetic,
class modifiers, invalid Java, source-pin drift and recipe preimage drift. Comment fixtures cover
seven syntax locations, with both block and line comments before the original return semicolon.

## Reusable recipe crate

The `atom-pattern-spectrum` classpath crate supplies two sorted path/preimage/postimage/template
rows to the existing `M3HashPinnedJavaSnapshotRecipe`. `M3AtomCommentRecipeTest` runs that actual
recipe against immutable baseline resources, proves exact output, refuses drift in either owner
and reparses the output before checking idempotent replay. It materializes candidates only under
`target/materialized/atom-pattern-spectrum`. The final audit binds those candidates, the resource
manifest and the actual compiled production owners to the same output hashes.

The earlier `atom-comment-custody` crate remains an intermediate failure/repair record. Use the
complete `atom-pattern-spectrum` crate for the final two-owner result. The snapshot owner itself
has not changed in this task.

For subsequent LLM work, first declare the bounded task and immutable source/contract pins. Add a
named crate and meaningful positive, refusal and fixed-point fixtures; evolve the existing recipe
when an executed failure warrants it. Materialize the reviewed recipe result, then rerun the
ordered gates. Do not repair individual corpus files or update the frozen original to make a
candidate pass. The owning module's default verification, OpenJDK configure/make/jtreg and native
tests retain their separate authority; this focused task cannot claim their success.
