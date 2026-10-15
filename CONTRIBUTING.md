# Contributing to M3JDK21

M3JDK21 retains the upstream JDK contributor guidance below. For M3-specific changes,
start with [AGENTS.md](AGENTS.md), the [porting invariant](m3/docs/m3jdk21-porting-invariant.md),
and [existing naming map](m3/docs/name-mapping.json). This checklist explains how to
prepare a review; it does not change those authorities or any license.

## Before editing

1. Record the target base SHA, branch and relevant source/test paths. Inspect path history
   backward to the last qualified baseline and forward through successors. A merge or a
   historical passing result is not evidence for a changed tree.
2. Identify the existing owner, public API/ABI and observables: exceptions, Unicode,
   equality/identity, ordering, concurrency, serialization, ownership and release.
   Reuse an existing guide and mapping row rather than creating a second authority.
3. Classify the change: standalone module, reusable donor recipe, exact target adaptation,
   runtime integration, documentation, or evidence. Review the
   [convergence model](m3/docs/SYNEXIA_CONVERGENCE_MODEL.md).
4. For reusable semantics, improve and qualify the canonical Synexia recipe first; receive
   exact pinned output here. Record donor revision/path/blob, recipe identity, target
   preimage/postimage, notices and target-only adaptations. Do not independently edit
   a sealed mirror or add a Synexia runtime dependency to `java.base`.
5. Preserve the ordered receiving phases. A standalone collection proof does not promote
   the JDK collection backend. Missing provenance or qualification is an explicit pending
   disposition, never a reason to weaken a gate.

## First build: prerequisites, outputs and test selection

The [build/test routes](m3/docs/native-build.md#current-checkout-verification-routes)
cover separate artifacts. Before running one, check the shell, JDK selection and
output locations below. These are source-derived instructions, not new passing results.

### Prepare the shell and JDKs

The [foundation script](m3/build.sh) uses Bash `mapfile`, arrays and process
substitution. Use Bash 4+ and make sure `sha256sum`, `find`, `sort`, `mktemp`,
`tee`, `cmp`, `chmod` and `mv` are available. The older system Bash on macOS
and a plain Windows command prompt do not supply this environment unchanged.
General OpenJDK host support does not establish support for every M3 helper.

Use isolated JDK installations and distinguish these variables:

| Variable | Consumer | Meaning |
| --- | --- | --- |
| `M3_JDK` | `m3/build.sh` | JDK used to compile and run the separate foundation |
| `M3_BOOT_JDK` | `m3/build-openjdk.sh` and native harnesses | Bootstrap JDK; the wrapper requests a private JDK 21 |
| `M3_TEST_JDK` | M3 runtime harnesses | Complete candidate image with matching Java and HotSpot code |

Setting one does not set the others. The foundation calls `javac`, `jar` and
`java` from `M3_JDK/bin`, rather than selecting them from `PATH`.

For native builds, follow the platform-specific
[compiler requirements](doc/building.md#native-compiler-toolchain-requirements),
[build tools](doc/building.md#build-tools-requirements) and
[external libraries](doc/building.md#external-library-requirements).
Use a short checkout path without spaces on a local disk, as described in the
[source checkout guidance](doc/building.md#special-considerations). Reserve space
for the image, objects, logs and test results before starting.

### Find the output from this run

| Entry point | Outputs to inspect |
| --- | --- |
| `m3/build.sh` | `m3/build/com.m3.text.jar`, `m3/build/classes/`, `m3/build/tests/` and `m3/build/logs/` |
| Foundation fixture generation | `m3/build/english-image-path.txt` names the content-addressed fixture; `m3/build/logs/artifacts.sha256` records hashes |
| `m3/build-openjdk.sh` | `m3/build/logs/native-configure.log`, `m3/build/logs/native-build.log` and `build/CONFIG/images/jdk/` |

The foundation uses `set -euo pipefail`: a compile or test failure stops later
steps, including fixture publication. An old JAR or fixture left in the output
directory does not establish that the latest command completed. Check the exit
status and invocation logs. Preserve logs before rerunning; the scripts reuse
their log filenames.

The [native wrapper](m3/build-openjdk.sh) passes fixed configure options and
does not forward additional command-line arguments. It defaults to five make
jobs; `M3_JOBS` changes that count. Use the documented OpenJDK configure/make
route when you need custom configure flags.

### Select the same image for the harness and jtreg

To make jtreg discoverable by the wrapper's configure invocation, set `JT_HOME`
to the directory containing `lib/jtreg.jar` before building. A direct configure
invocation can instead use `--with-jtreg=/absolute/path/to/jtreg`; see
[test configuration](doc/testing.md#configuration).

After building, substitute the actual configuration name for `CONFIG` below:

```sh
export M3_TEST_JDK=/absolute/path/to/this-checkout/build/CONFIG/images/jdk
"$M3_TEST_JDK/bin/java" -version
make CONF_NAME=CONFIG test TEST="test/jdk/java/lang/String/M3StringCanonicalDagTest.java test/jdk/java/lang/String/M3StringStreamsTest.java"
```

`M3_TEST_JDK` selects the M3 harness image; it does not select the configuration
used by `make test`. Use the same
[explicit configuration](doc/building.md#using-multiple-configurations) for both.
The separate [scripted jtreg lane](m3/runtime-integration/run-jtreg.sh) requires
`JTREG_JAR`, `M3_TEST_JDK`, `M3_BOOT_JDK` and built native test libraries; its
default native-library path is Linux x86-64. A focused test selection remains
separate from full JDK, platform and performance qualification.

## Documentation and source comments

Document inputs, units, return/refusal behavior, exceptions, owner lifetime, thread-safety
and compatibility boundaries where the implementation establishes them. Link the symbol
or executable test rather than treating a design aspiration as an implementation fact.
Keep historical receipts dated and immutable in meaning; add a current-status explanation
instead of silently relabeling the old result.

Javadoc and native comments inside hash-sealed source are source changes too. Reconcile
such edits through the owning recipe and refresh affected receipts; a comment-only change
can invalidate exact blob checks. This is why a guide correction need not rewrite sealed
Java/C++ files. Preserve per-file copyright, license and change notices.

## Review and regression evidence

Use the [build/test routes](m3/docs/native-build.md#current-checkout-verification-routes)
for the affected artifact. In a PR, include:

- exact base and candidate SHAs, changed owners and recipe/provenance links;
- the command, toolchain, OS/architecture, runtime flags and selected image;
- separate PASS, FAIL, BLOCKED and NOT_RUN results, with logs and test selection;
- fixed-point, changed-input refusal and rollback evidence for recipe changes;
- relevant String/Unicode/regex, collection, JNI/GC or native regression cases;
- CPU, retained heap and native/process memory evidence for resource claims;
- excluded surfaces, known failures and remaining promotion gates.

A documentation-only PR should check links, shell examples and source claims, and state
that native builds, SDK tests, jtreg and performance measurements were not run unless
actually executed. A parsed example or source invariant is not runtime qualification.
A production PR must additionally run the applicable exact-head build and behavior gates.
Do not alter an oracle, delete a failing test or claim the entire JDK from a focused pass.

For a publication candidate, recheck the base and final diff, then open a draft PR with
bounded claims. Keep publication, merge and release decisions distinct.


# Contributing to the JDK

Please see <https://openjdk.org/contribute> for how to contribute.
