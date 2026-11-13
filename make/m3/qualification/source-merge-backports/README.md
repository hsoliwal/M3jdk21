# Source-custody merge: JDK backport inventory (3 November 2026)

**State:** ADDITIVE_SOURCE_STAGING / PRODUCT_HOLD. This is not a `master`, JDK-release, or protected-branch merge.

**Exact first-parent receiving source:** `hsoliwal/M3jdk21:m3/jdk21-backport-wave5-20261103-gpt6@f89362c62fdedb166d8a156972686bb1f28e91ec`, the reviewed #2361 source-superset branch.
**Exact second-parent donor:** original `hsoliwal/M3jdk21#2261@c30ba3f8124b09633f28d339b37cc42e0931d580`; draft transport #2368 points to that same immutable head.
**Common source ancestor:** `315cd18d0092f70c212223bb6af1f4366591ed1b`.

## Verified preimage and donor object inventory
The receiver's complete existing five source files were retrieved and found byte-for-byte identical by Git blob identity to the donor's common-ancestor copies. All five donor postimages are pre-existing Git blob objects in the SAME repository, mode `100644`. They do not overlap the receiving #2361 changed paths. No new Java method, C/JNI native export, public API, or serialization contract is added by this bounded intake.

| Path | Receiver/common Git blob | Exact donor Git blob |
|---|---|---|
| `.github/workflows/m3jdk21-upstream-inventory.yml` | `7a6fd8bae568b50f4beee19025f447ff80334ddd` | `3a3e0d9d835323a6151536731addfaef4d54a541` |
| `make/m3/backports/compatibility_queue.py` | `4296619d4edf1be471f5cf3cf9bf249642bdacea` | `cbdf02fe4b1232c1c52e35e705d32a14ecd72582` |
| `make/m3/backports/inventory.py` | `9b85f31fd88a06744df041241baf67116c24b2bf` | `f81f9f09f87bd91d5479aed1554439d6b5b056d3` |
| `make/m3/backports/test_compatibility_queue.py` | `5d6272318a29f2052f5bb27e8dc6ecbfcb7cb3b1` | `e5adab00788f868e5d72e68d533b65d6cc6ab3aa` |
| `make/m3/backports/test_inventory.py` | `4c1b1ed3f3ac22d61580cb86fef923a877f46457` | `244af69095a850fc67cfe23f5188e1cf91b3e88e` |

## Merge procedure
One normal, **non-force, exact-head-leased Git two-parent commit on this new isolated review branch**: first parent is this documentation commit descending from original receiving tree, second parent is original donor commit. Compose its Git tree solely by copying the five pre-existing donor file blobs/modes into the existing first-parent tree (no reserialization, no destructive ours/theirs conflict). Everything outside these five paths is **identical** to first parent by tree inheritance. After commit, independently compare first-parent → merge and donor file identities, and confirm first-parent + donor ancestry; refuse any drift.

## Admission stops
Original provider JDK21 backport inventory test suite, Python verifier, Maven/OpenRewrite Recipe.run/Result.getAfter fixed point, ABI/HotSpot/JNI/jtreg, current full CI, and combined product runtime **NOT RUN** for this new source union. Do not merge into `master` or the protected staging ref until gates pass. All unrelated original PR refs, history and workflow source are retained, with no deletions or forced branch update.

Third-party sources: none copied. LeetCode/HackerRank/GeeksforGeeks remain algorithm category/test taxonomy only and are not used as licensed donor implementation bodies.
