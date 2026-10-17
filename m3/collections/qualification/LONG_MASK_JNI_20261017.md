# M3JDK21 JNI64 mask selection — sealed successor receiving, 2026-10-17

**Status:** nine source-bound receiver atoms committed and exact source-template
Git blobs read back; actual Maven, C17/JNI runtime and full JDK acceptance are
not yet green. This does not change `java.base`, HotSpot or the JDK21 API.

## Recipe/source ancestry

- Canonical donor `hsoliwal/com.synexia`, first-party Apache-2.0.
- Recipe PR [Synexia #12002](https://github.com/hsoliwal/com.synexia/pull/12002)
  (stacked on [#11995](https://github.com/hsoliwal/com.synexia/pull/11995)).
- Target branch is stacked **additively** on wider wrappers
  [M3JDK21 #1030](https://github.com/hsoliwal/M3jdk21/pull/1030).
- Target original three JNI/Java atoms match original M3JDK21
  `master@970a34350ebdf6a3f1c5ff5acc8445d0f3515f55`.
- Named recipe: `com.synexia.m3.Jdk21LongMaskJni`.
- The canonical `M3Jdk21LongMaskJniRecipe` composes
  `M3HashPinnedJavaSnapshotRecipe` (two Java files) and
  `M3HashPinnedTextSnapshotRecipe` (one C JNI file, six metadata files).
  Historical and new input SHA-256 are recorded in two original source
  manifests and one dated metadata manifest.

## Java/JNI implementation

`M3IndexMaskCompactor.selectLong(long[], byte[], int)` remains the exact
public reference operation. The new overload
`selectLong(long[], byte[], int, Backend)` selects `JAVA` or the explicitly
required `NATIVE_REQUIRED` JNI backend. The latter fails when the native
library is unavailable: there is no implicit fallback or object callback.

The C17 `selectLongNative` implementation generalizes the existing,
proven `int[]` JNI kernel. It makes two bounded 256-element region passes,
counts accepted positions, allocates one exact `jlongArray` and preserves
signed values, duplicates, encounter order and unsigned-byte mask 0..255.
It does not store Java pointers, pin critical arrays or invoke Java
predicates. Inputs must remain stable until the result is returned.
Complexity: O(n) time, O(k) returned array, O(256) native stack scratch.

The existing primitive `M3LongFilteredView`, sorted collection wrappers,
and general `NavigableSet`/`NavigableMap` owners are unchanged. JNI64 is
for **detached primitive coordinate selection**, not live object views.

## Dated additive source inventory

The pre-existing historical receipts remain byte-identical:
- `m3/collections/qualification/mask-selection-postimages-20261016.tsv`;
- `m3/collections/qualification/installed-source-wrappers-20261017.tsv`;
- `m3/collections/synexia-donation.json`.

The new `long-mask-jni-successor-20261017.tsv` has exactly three
`path/preimageSHA256/postimageSHA256` rows and a separately pinned
manifest SHA-256 `73d6521fd92b9d093059f45e39fc28906ca7fbbbafd651e871ec931a1981547f`.
The generic receipt helper accepts those exact three predecessor paths
only and rejects symlinks, reordered rows, extra paths, incorrect hashes
and old-preimage mismatches. The historical source-verification scripts
only substitute the new hash when the predecessor exactly matches;
every unrelated path retains its original hash.

## Required execution and proof

```sh
python3 -m unittest discover -s m3/collections -p 'test_long_mask_successor.py' -v
python3 m3/collections/qualification/verify_mask_selection.py
# Run the existing focused native and Java21 test workflow:
# .github/workflows/m3-collections-mask-receiver.yml
```

The targeted workflow will compile main Java and current self tests with
`--release 21 -Xlint:all -Werror`, compile native C17 using strict
warnings, run original int[] and new long[] differential tests under
`-Xcheck:jni`. The original long-list/deque and source inventory
qualification remain mandatory. The larger wrapper PR has its own
independent full module/JNI proof and must not be silently replaced.

Competitive-programming sources (LeetCode/HackerRank/GeeksforGeeks) only
define reusable problem categories; no challenge-site code is copied.
See Synexia's category/donor/complexity catalogue and recipe JUnit tests.
Keep this PR DRAFT pending actual hosted Java/JNI results and review.
