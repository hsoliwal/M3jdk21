# M3 fixed-address Java/JNI boundary refinement

Copyright 2026 Hitesh Soliwal and contributors. Apache-2.0.

Canonical concept/recipe custody is Synexia; this exact capsule is a receiving
proof packet for M3JDK21. It extends the original M3FixedAddressMapping from
Synexia #11849 / M3JDK21 #966. Both source preimages are still byte-identical in
master `970a34350ebdf6a3f1c5ff5acc8445d0f3515f55`; no replacement mapping owner,
`String` class, shared counter table or public/JNI signature is introduced.

## Meaningful behavioral atoms

1. **Native file-name admission.** Linux no longer passes JNI modified UTF-8 to
   open(2). The native helper calls the existing JDK UTF-8 encoder, checks every
   relevant JNI exception, rejects embedded NUL and releases temporary memory.
   ASCII, Hindi and supplementary character paths execute the same mmap owner.
   This is a UTF-8 Linux profile, not a claim for arbitrary native file encodings.
   The existing Windows UTF-16 branch is unchanged and unexecuted here.
2. **Unpublished mapping rollback.** A digest provider RuntimeException/Error or
   Image construction failure releases the verified, not-yet-published mapping.
   Normal digest mismatch uses the same cleanup. Cleanup IOException is suppressed
   onto the admission failure. Publication is deliberately outside rollback:
   unmapping after a partially successful registry insertion could leave a live
   Image referring to unmapped memory. Full publication-OOM recovery is not claimed.
3. **Bounded process proof.** The new verifier uses bounded readiness reads and
   child cleanup. Two simultaneously alive JVMs must agree on the exact base,
   device, inode, offset, mapped extent and read-only shared permissions. Equal
   inodes alone are explicitly insufficient.

The immutable-publisher, process-lifetime and Integer.MAX_VALUE contiguous-image
limits of the existing owner remain. Read-only mapping/SHA admission does not
protect against a later external rewrite or truncation. No arbitrary-process
address reservation, OS sealing, cross-process reference count, zero-copy Java
object model, global heap change or performance speedup is claimed.

## Reproduce through the existing Maven/OpenRewrite engines

```
python3 m3/gvas/boundaries-20261017/prepare.py
mvn -B -ntp -f m3/gvas/boundaries-20261017/recipe/pom.xml \
  -Dm3.gvas.output=/tmp/m3-gvas-sdk-output test
python3 m3/gvas/boundaries-20261017/prepare.py \
  --install-sdk-output /tmp/m3-gvas-sdk-output
python3 m3/verify-fixed-address-boundaries.py
```

The task reuses **unchanged** M3HashPinnedJavaSnapshotRecipe and
M3HashPinnedTextSnapshotRecipe, already delivered in another M3 receiver at
`11dde1eac33cea2a8b90baba8c155eabec341144`. Their exact Git blobs are
`d58ca50dfaeb60f581b64f43545e688aca5c6530` and
`2db1257a40d21be06ba2a67da87aff0311183809`. Frozen copies are build tooling only;
Synexia retains canonical family ownership. Current canonical Synexia can have
additional capabilities; no newer engine is silently downgraded.

prepare.py authors reviewed resources, **not** SDK results. The Maven suite must
actually execute before product source installation. Two Java targets are parsed
as Java LST; C and Python remain sealed text whose semantics require their real
compilers/runtime tests. Exact outputs, second-pass no change, missing/drifting/
occupied/non-Java/duplicate refusal and partial replay are tested. Fix the capsule
or fixture and rerun; never repair generated Java/C output by hand.

The standalone Maven module is an isolated recipe proving surface; it is not a
new default-reactor build, installed JDK, native ABI or runtime dependency.
The source workspace can point m3.snapshot.providers at its existing canonical
Java owners for an independent source-provider qualification; that is a separate
receipt from the exact frozen receiver-provider run.

## Executed local proof and explicit holds

OpenJDK 21.0.12.1 on Linux x86_64; Java21 strict lint, generated JNI header parity,
C17 -Wall -Wextra -Werror, fatal UBSan and -Xcheck:jni. Eight single-process cases
and two simultaneous child cases passed **130 Java assertions** plus Python
mapping identity checks, at `0x600000000000`. Both exact original defects were
reproduced independently: supplementary file-name open failure, and occupied
address after an injected digest failure. Receipts include the source seals and
failed-baseline logs. Two fresh successful full runs were executed locally.

An initial ABI test pre-included jni.h before _GNU_SOURCE, hiding GNU open flags.
The test compiler unit was corrected to include the generated header after the
unchanged implementation's feature-test setup. No flags or compiler checks were
weakened; the original and candidate JNI headers match exactly.

Hosted Maven/SDK, Windows native build/runtime, full reactor, matched HotSpot
image, jtreg/JCK, compiler lowering, VM reference counters and the broader
memory-pressure/ownership programme are not certified by these local tests.
Keep the PR draft until each applicable gate actually executes. Pending and
zero-step workflow records are not passes.

CATEGORY_REVIEW.tsv extends the shared eight-column algorithm ledger. The
existing M3CountedCatalogue reader from the wrapper receiver can search it; no
new search engine or copied challenge solutions are introduced. JDK encoding,
OS mapping APIs and existing first-party code are reused; external projects are
comparison/category references, not silently relicensed source imports.
