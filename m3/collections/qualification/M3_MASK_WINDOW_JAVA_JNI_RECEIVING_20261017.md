<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 bounded int/long array mask-window receiver

Target owner: `m3/collections` separate Apache-2.0 library. Canonical recipe and catalogue: `hsoliwal/com.synexia`, `M3Jdk21MaskWindowJni20261017` (exact commit pin in RECEIVER.tsv).

This receiver modifies only `M3IndexMaskWindow.java` and the preexisting `m3_index_mask_compactor.c`. It does not change java.base/HotSpot, object references, public java.util APIs, or other masked collection factories. Existing sorted views and indexes consume the same primitive cursor/mask conventions; no new per-entry payload objects.

The previously declared `scanNative(int[])` is now exported; new `scanLong(long[])` Java/JNI stays bounded to caller-provided buffers. Output is stable and duplicate-preserving, all geometry is checked before writes. The native code uses JNI regions only; no pinned heap pointers survive calls. `NATIVE_REQUIRED` never silently falls back.

Selected Java21 verified on Linux x86-64: `javac --release 21 -Xlint:all -Werror`, 15,000 randomized seeds and >14.8 million Java/JNI checks under `-Xcheck:jni`; exact target source build, Maven/OpenRewrite SDK, jtreg, image ABI, native multi-platform, and performance gates remain open. See verification script and source-hash manifest.
