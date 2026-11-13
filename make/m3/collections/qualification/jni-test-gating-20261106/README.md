<!-- SPDX-License-Identifier: Apache-2.0 -->
# m3/collections JNI test gating (2026-11-06)

Request from the lane owner (com-synexia-45): the default `mvn -f m3/collections/pom.xml test` on a host without the
m3_arrays native library is red (master 0b7482c3263: 1682 tests, 5 failures, 49 errors, 2 skipped,
298 classes, 22 failing classes; the same 22 classes with identical per-class counts at 927e79becaf (1,633 tests,
the first enumeration, #2530), f5a7f80b2c0, cf0429ac1d4 and e65f0f664fa; on 97631687006 and the push base 9b7269e00fd
m3/collections does not compile under its -Werror (#2561), see GATES.tsv push_base_*).
Since #2524 `collections` is a default module of `m3/pom.xml`, so this reddens the default reactor verify as well.

## Pattern copied from m3/search/pom.xml

`m3/search/pom.xml` lines 58-66 exclude the JNI-dependent tests from the default surefire run
(`<excludes><exclude>**/*Native*Test.java</exclude></excludes>`) and lines 70-101 define the profile
`m3-search-native` whose surefire configuration re-includes them with `<excludes combine.self="override"/>` and
passes the native library paths as `systemPropertyVariables` built from `${m3.search.native.dir}` and the
`lib`/`.so` prefix and suffix properties. This directory copies that pattern verbatim into `m3/collections/pom.xml`:

* default build: `<excludes>` listing the 21 JNI classes (EXCLUDED.tsv); collections' test names are not shaped
  `*Native*Test`, so the classes are enumerated instead of globbed;
* profile `m3-collections-native`: `<excludes combine.self="override"/>` plus
  `m3.arrays.native.path=${m3.collections.native.dir}/${m3.collections.native.prefix}m3_arrays${m3.collections.native.suffix}`
  (`lib`, `.so`), the one property every excluded class needs (49 errors: `M3ArrayNative` "M3 array JNI library is
  not loaded"; 4 failures: `M3IndexedOccurrenceArrayNativeTest` "real m3-arrays JNI library path required").
  Build the library first: `cmake -S m3/arrays/src/main/native -B <dir> && cmake --build <dir>` as
  `.github/workflows/collections-convergence.yml` lines 129-134 do, then
  `mvn -f m3/collections/pom.xml verify -Pm3-collections-native -Dm3.collections.native.dir=<dir>`.

Test sources are unchanged except for one literal (below). No test was deleted, no assumption helper was added.

## Enumeration

`tools/failing_classes.py <surefire-reports> FAILING_CLASSES.tsv EXCLUDED.tsv` (recipe SUREFIRE_FAILING_CLASSES v1,
CPU only) parses `TEST-*.xml`, keeps classes with failures or errors, and classifies JNI (UnsatisfiedLinkError or a
"JNI library" message) versus OTHER. FAILING_CLASSES.tsv is the baseline enumeration; EXCLUDED.tsv the 21 JNI
classes carried into the pom (the pom's `<exclude>` list and EXCLUDED.tsv are compared mechanically).

## OTHER (not excluded)

`M3ObjectResidencyTest.residencyAnnotationIsClassRetainedAndExplicit` failed with "class file retains compile-time
metadata": the test asserts the class file contains the descriptor `Lcom/m3/collections/M3Residency;`, but since
the JDK-naming rename the compiled descriptor is `Ljdk/m3/collections/M3Residency;` (strings -a of
`M3ObjectResidencyTest$AnnotatedProjection.class`). This is rename drift in a slash-form descriptor literal that
recipe JDK_NAMING_RENAME does not rewrite, not a Windows residency failure. Fixed by renaming the literal (one line);
the lane owner may revert to an exclusion if the test is meant to stay as it was.

## Gates

See GATES.tsv (re-run on master 0b7482c3263 and, -Werror stripped in scratch, on 97631687006;
supersedes #2530, whose base was 27 commits behind). Logs on the executing host: S:/m3-scratch/eb-conv/collatoms/
(m-<master>-test.log, a5-<master>-default_verify.log, a5-<master>-profile_verify.log, a5-<master>-class_diff.txt,
w-<master>-*.log, wsl_profile_verify_n.log).

## Open items

* masters 97631687006 to 9b7269e00fd do not compile m3/collections under -Werror (M3PrimitiveRoaringIntSet, #2561);
  owner: absorb lane;
* the full reactor `-DskipTests package` fails on this Windows host in m3-fs (`No CMAKE_C_COMPILER`), before
  collections; `-pl collections -am` is green;
* JDK_NAMING_RENAME follow-up: slash-form descriptor literals (`Lcom/m3/...;`) in tests are not covered by the recipe;
* master 0b7482c3263 is not a JDK-naming fixed point (14 rewrites under m3/collections, Javadoc lineage lines of
  absorbed M3Primitive* owners); outside this change, left to the absorb lane.
