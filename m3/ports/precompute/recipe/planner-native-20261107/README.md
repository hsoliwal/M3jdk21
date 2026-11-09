<!-- SPDX-License-Identifier: Apache-2.0 -->
# Recipe planner-native-20261107: the planner family's JNI kernels in m3_precompute

The two C kernels that `planner-family-20261107` deferred now join the module's `m3_precompute` JNI library. They come
from their newest donor lineages, walked through the crate manifests (each crate's before sha256 is the previous
link's after sha256):

| Target (`src/main/native/src/`) | Donor (com.synexia develop `78cf2ad4c7e5`, hash-pinned-text) | JNI entry points (`Java_jdk_m3_precompute_M3NativePrecomputeOps_*`) |
|---|---|---|
| `m3_precompute_pair_jni.c` | `m3-primitive-pair-empty-index-20261103/precompute_jni.c.txt` (`90a95e2de2ab`; 4 links after the main path) | `nativeAbiVersion`, `lookupPairsNative` |
| `m3_precompute_tuple_jni.c` | `m3-frozen-tuple-jni-alias-20261101/tuple_lookup_jni.c.txt` (`7b06eefa1d92`; 1 link after the main path) | `tupleLookupAbiVersionNative`, `lookupTuplesNative` |

`relocate.py` (RELOCATE_PLANNER_NATIVE v4, derived from `m3/arrays/recipe/bitmatrix-20261106` v3) renames the JNI
symbol prefix (JNI row) and writes the M3JDK header. It accepts `.c.txt` postimages and closed one-line SPDX comments.
It appends one `target_sources(m3_precompute ...)` block to `src/main/native/CMakeLists.txt`; that registration is the
only build-file change. `M3NativePrecomputeOps` (relocated earlier) loads `m3_precompute` and keeps the Java path as
the semantic oracle: without the library, or with `-Dm3.precompute.disableNative=true`, the Java path runs.

## Gates

`native-gate.sh` runs on Linux. The run recorded in GATES.tsv used WSL Ubuntu 24.04, gcc 13.3, cmake 3.28 and
Temurin 21. The donor side comes from `prepare-donor.sh`, run on the host that holds the donor clone. The gate checks:

- the `javac -h` header of the donor host against the relocated host (4 prototypes);
- gcc of each kernel with the module's flags (`-Werror -Wconversion -Wshadow -Wpedantic`), with the header passed
  through `-include`;
- the module's CMake build, ctest, and `nm -D` (the 4 symbols);
- the strict planner dump with the native kernels against the Java fallback, under `-Xcheck:jni`, on the JIT and under
  `-Xint` (outputs must be byte-identical);
- two mutant libraries, each of which must be caught.

`verify.sh` (Windows or Linux) runs the recipe fixed point, the Maven package, the naming plan and the class-identity
gate. No Java file changes, so every class file must stay identical.
