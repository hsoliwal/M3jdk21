# Common JNI qualification

This directory is a receiving fixture for the canonical Synexia recipe. The
receiving path is `m3/collections/qualification/common-jni-20261020/`. The runner
reads the supplied M3JDK21 source tree and writes only to a dedicated output
directory. It uses Python's standard library, a JDK 21, and C17/C++20 compilers.
It downloads no dependencies and requires no installed Python packages.

## Run against the current source tree

```sh
python3 m3/collections/qualification/common-jni-20261020/run_native_tests.py \
  --candidate "$PWD" \
  --jdk "$JAVA_HOME" \
  --out /tmp/m3-common-jni-current
```

The output directory must be outside the checkout or ignored by Git. The runner
accepts an empty directory or one it previously created; it never clears an
unrelated populated directory. Each run replaces its own `work/` and `logs/`
subdirectories and records the result in `receipt.json`. `--cc` and `--cxx`
override the compilers; their defaults are `CC`/`CXX`, then `cc`/`c++`.

To bind CI to a particular commit, set `EXPECTED_HEAD` or supply
`--expected-head <commit>`. The candidate must be the checkout root, its `HEAD`
must equal the supplied commit, and all checked product inputs and fixture code
must be tracked and unchanged from that commit. Fixtures must be inside the
same checkout in this mode. Local recipe development can omit this option and
qualify generated, uncommitted output by its recorded source hashes.

## Compare an exact baseline

```sh
python3 m3/collections/qualification/common-jni-20261020/run_native_tests.py \
  --candidate /path/to/generated-M3JDK21 \
  --baseline /path/to/exact-before-M3JDK21 \
  --jdk "$JAVA_HOME" \
  --out /tmp/m3-common-jni-differential
```

The baseline must contain the original 13 C owners, their actual Java owner
sources, and `m3_state_relation.h`. A baseline without the new common helper is
supported. The runner compares the Java native declarations and C JNI
signatures, and requires identical Linux mapping/error/cleanup model
transcripts. It also runs the real JVM/kernel checks against both trees.
Without `--baseline`, the receipt explicitly records that the before/after
differential was not run; current-only CI does not claim baseline equivalence.

## What the checks establish

| Check | Input and scope |
| --- | --- |
| JNI ABI and ownership | All 13 actual C owners and their 13 actual Java owners. The runner extracts native declarations from those Java files, generates headers with `javac -h`, and independently compiles each C file with its generated header forced in. Every candidate owner must include the same product `m3_jni_util.h`. |
| Linux mapping model | 24 deterministic cases execute the actual `M3StringPool.c` against intercepted system calls and a JNI function table. These cover exact mapping, wrong-address cleanup, occupied-address failure, direct-buffer allocation failure, descriptors, file sizes, alignment, lengths, exception lookup, exception throwing, and unmapping. |
| Hint and Apple branch models | The same 24 cases compile the actual owner after suppressing `MAP_FIXED_NOREPLACE`, or forcing its Apple preprocessor branch after host headers are loaded. Both require non-destructive hint flags and exact-address acceptance. |
| Shared exception helper | The actual header executes three JNI-table cases in C17 and again in C++20: successful throw, failed class lookup, and failed `ThrowNew` with local-reference cleanup. It preserves the caller's pending-exception policy. |
| Microsoft C keyword branch | `MSVC_KEYWORD_MODEL` forces `_MSC_VER` on the host C compiler, verifies the actual preprocessed helper selects `static __inline`, and runs the same three helper cases. It uses no Windows SDK or Microsoft compiler. |
| Real JVM and Linux kernel | The actual production mapping functions are registered to a test-only class through `RegisterNatives`. A stock JDK runs eight checks under `-Xcheck:jni`, including an occupied anonymous sentinel whose bytes must survive the rejected mapping and an exact-address file mapping/unmapping round trip. Production symbol names remain unchanged. |

There are 27 JNI methods across the 13 owners. Generated ABI classes contain
only native declarations extracted from actual Java files; these classes test
the ABI and do not replace full owner compilation or Java semantic tests.
Each C owner compiles independently so a missing dependency or mismatched JNI
signature cannot be hidden by another translation unit. The existing full
collection Java/JNI workflow remains responsible for its broader contract suite.

The receipt records SHA-256 for each checked C source, Java source, product
dependency header, common helper, fixture, and JDK JNI header. It records
commands and their exit status, compiler/JVM versions, normalized logs, and
hashes for generated headers, ABI classes, objects, executables and libraries.
Absolute input/output locations are normalized in recorded commands and logs;
there are no wall-clock timestamps. The runner checks input hashes again before
reporting success, so concurrent source changes cannot silently contaminate a
passing receipt.

## Evidence boundaries

Real execution currently requires Linux. The Apple branch uses Linux headers
and deterministic syscall models; it is not an Apple SDK build or macOS/iOS
kernel test. The missing-`MAP_FIXED_NOREPLACE` model is not a run on an older
Linux or Android kernel. `MSVC_KEYWORD_MODEL` is a host compiler model of the
Microsoft C declaration branch; it is not a Windows build. No Windows, Android,
or iOS runtime is exercised.
The checked stock JVM uses the real product C functions through fixture-only
registration; it is not an installed source-built M3JDK21 image. Those platform
and whole-runtime qualifications remain separate gates.

## Existing CI integration

The proposed receiving change extends `.github/workflows/m3-collections.yml`:
it adds the relevant `java.base` source paths to both path filters, runs this
current-only gate in the existing exact-head Linux job after its pinned JDK
setup, and includes the dedicated runner output in that job's always-retained
artifact. It preserves the existing Java and all-native contract step and does
not create another workflow or claim unexecuted platform coverage.
