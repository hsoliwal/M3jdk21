# M3JDK21 primitive-mask full-selection receiver

The canonical algorithm/recipe lives in private `hsoliwal/com.synexia` PR #12052, commit `7d8d33c8ccdb59309a3d45c33bae7efef9050a39`. Target file `com.m3.collections.M3IndexMaskCompactor` is a *target-owned adapted output*, not a parallel Synexia recipe owner. Public JDK `java.util`, `java.base`, HotSpot, and native JNI implementation remain unchanged.

The byte mask/offset compaction algorithm preserves ordered rows and duplicates and returns detached primitive arrays. New full-selection fast paths avoid redundant flag scans, but still copy output. `Backend.NATIVE_REQUIRED` remains real JNI; there is no silent fallback. Both Java and native retain no per-entry objects or address beyond the JNI invocation. Invalid masks and mismatched dimensions remain rejected before copying.

Run `python3 m3/collections/verification/mask-all-select-20261017/verify.py` on a Java21/Linux/GCC host. The script binds an exact generated SHA-256 source, compiles source and two differential proofs with warnings-as-errors, checks class-file repeat identity, builds the *unchanged original JNI C17* method and runs 11,520 comparisons under `-Xcheck:jni`. This **does not** substitute for the Synexia Maven/OpenRewrite SDK, this module's Maven JUnit, full JDK/image, jtreg, GC, or performance qualifications.

The original unmodified source is target `master@a918e588dd11d5ff7e0a6d4efaa89bb1679a9798`, Git blob `240bbfb62ca949bfbd8abb0061cccad6630f7888`; the C source Git blob is `f1a9488905807e6417dd267e125bfc44dbcc9297`. No new dependency, native ABI, or unverified API change is admitted.
