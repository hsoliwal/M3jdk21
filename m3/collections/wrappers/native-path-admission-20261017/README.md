<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 exact native-source admission, product receiver

**Runtime/product repository:** `hsoliwal/M3JDK21`.
**Convergence and canonical Maven/OpenRewrite source:** Synexia
[PR #12008](https://github.com/hsoliwal/com.synexia/pull/12008),
source-first `com.synexia.m3.M3Jdk21ExactNativePathAdmission20261017`.

The installed code retains its original Java public/protected APIs and
native symbols. One guarded private text-source path is admitted:
`m3/collections/src/main/native/collections/m3_int_filter_jni.c`.
It is already present as a SHA-256/ABSENT-before native source in the
M3 filtered MAT receiver, from the Synexia first-party Apache-2.0 donor.
No other module JNI, C++, Java, native pointers or GC/refcount behavior
is admitted.

**Before**: pinned text provider Git blob
`2db1257a40d21be06ba2a67da87aff0311183809`; it rejects the proper
module-local C17 source at manifest admission.

**After**: reviewed Git blob
`8bd6f399b298f364a41d53391ba2e72da4c5c635`; it uses only a
literal path identity Boolean in the existing private textPath method,
retaining the old `src/main/native/**`, traversal, SWT and structured
text rules unchanged.

### Serial recipe-first application

1. Canonical Synexia Java LST exact before/after templates, SHA-256
   manifest, original source custody, problem-category donor choice and
   `Recipe.run` fixed point/refusal proof are committed FIRST.
2. This M3JDK21 receiving branch mirrors those exact canonical recipe
   files and source acceptance test under the **existing isolated**
   `m3/collections/wrappers/workflow-pin-20261017/recipe` Maven module
   to avoid modifying the separately pinned wrapper recipe tree.
3. A target-side JUnit `M3IndexFilterNativePathContractTest` enumerates
   allowed and refused other JNI/header/traversal/native paths.
4. Only after the recipe receipt, receive the exact reviewed after
   Git blob into the original pinned-engine provider path.
5. The dedicated CI builds real Maven/OpenRewrite JUnit, uses the same
   original JNI manifest from the wrapper recipe, and executes unchanged
   signed-int Java/C17 parity under Serial, Parallel, G1, ZGC and Shenandoah.

No arbitrary source-address reference counts or new HotSpot collector
features are inferred from this source-loader correction. All
`verify_donation.py`, installed source-sets, full reactor and
JDK/jtreg/JCK proof remain independently authoritative. No CI result
should be called passing until the exact target-head executed job
completes and its logs are inspected.
