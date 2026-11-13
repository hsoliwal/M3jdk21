# M3-GVAS strict virtual-address contract — Java/JNI receiver candidate

Copyright 2026 Hitesh Soliwal and contributors. Apache-2.0.

**Hard invariant:** every *admitted, cooperating* process maps the same immutable canonical backing image at the **same numerical virtual address**, e.g. `0x600000000000`. An unavailable, conflicting, misaligned or different address fails admission. File/physical-page sharing or equivalent canonical IDs alone DO NOT satisfy this strict contract. There is no automatic relocation fallback.

- Linux: `mmap(PROT_READ, MAP_SHARED | MAP_FIXED_NOREPLACE)`; verify return exactly equals requested pointer. Never use destructive `MAP_FIXED`.
- Windows: `MapViewOfFileEx(FILE_MAP_READ, requested)` with allocation-granularity validation. Failure to obtain exact pointer rejects admission. Windows runtime and compiler proof remains pending.
- Java: `M3FixedAddressMapping.open(image, address, SHA256, nativeLibrary)` validates the exact JNI-returned direct buffer pointer, immutable-image digest, and ownership. Published views are read-only and process-lifetime; no unmap-after-publication.
- Proof: `python3 m3/verify-fixed-address-jni.py` compiles Java 21 JNI and checks two live JVMs with distinct GC/JIT modes, the exact address, `/proc/<pid>/maps` `r--s` mapping, offset 0 and the exact **same inode**. Collision and SHA mismatch reject admission.

This is a bounded native image substrate, **not** proof of global interception of arbitrary processes and **not** a replacement for java.lang.String, HotSpot oop handles, or application collection references. All joining processes must reserve the agreed slot early enough that the OS can honor it; if not, they must refuse. The trusted canonical publisher must keep backing bytes and length unchanged throughout the process lifetimes; SHA verification before publication does not prevent an external privileged writer from corrupting a mapped file later. Physical pointers are not cross-process Java object references. At present the Java ByteBuffer size limit applies; >2 GiB must be handled by a separately qualified segmented mapping recipe.

**Proof completed:** Linux x86_64, Java 21.0.12.1, C11 `-Wall -Wextra -Werror`, JNI `-Xcheck:jni`, two JVMs (`-Xint`, `-Xbatch`), identical `0x600000000000`, inode and `r--s` permissions, collision refusal, bad hash refusal. Reproducer in this PR packet. **Not run:** actual Maven/OpenRewrite SDK application and fixed-point, Windows compile/runtime, integrated HotSpot String+JNI/JTreg and full reactor; keep draft. Source seal and exact target hashes are in Synexia recipe resources.
