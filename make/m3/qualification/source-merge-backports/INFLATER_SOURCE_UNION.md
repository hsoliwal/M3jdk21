# Additive Java 21 InflaterOutputStream source fold — exact-head contract

**State:** SOURCE-CUSTODY CANDIDATE / PRODUCT TEST HOLD. This leaf is added only to the existing isolated review branch of M3JDK21 PR #2376. It must not modify `master` or the original protected wave5 receiver.

The existing five-file first-party backport inventory two-parent source merge (#2261) is preserved at commit `869c049caed170903c837a2a238478ea101c79c3` with final evidence head `8e05579cc5fb2eee6084782abce6e45e0658f47a`.

Incoming first-party Java/JDK backport donor: original M3JDK21 [#2264](https://github.com/hsoliwal/M3jdk21/pull/2264), exact head `095bd738c377fa597fe95fe94a5029651e94d4bf`, source ancestor `315cd18d0092f70c212223bb6af1f4366591ed1b`. Upstream attribution: OpenJDK JDK-8369181 `openjdk/jdk@e58e576f80cd35728bac6738474c4090737c16a8`, GPLv2 with Classpath Exception. Preserve upstream attribution and license; no other donor source copied.

## Source audit completed before merge
Every incoming path has been inventoried. The existing `InflaterOutputStream.java` receiving blob equals donor common-ancestor blob **exactly**, `7b08d5b45385caaecec8d14ceee6c3eb3ec3a4f9`. The two remaining donor paths are ABSENT on the receiving tree. Original donor Git tree proves all three postimage files use mode `100644`:

| Path | Receiving preimage | Donor postimage |
|---|---|---|
| `src/java.base/share/classes/java/util/zip/InflaterOutputStream.java` | `7b08d5b45385caaecec8d14ceee6c3eb3ec3a4f9` | `19a56deb5977c64e16d3e8593fb74dc8444e90eb` |
| `test/jdk/java/util/zip/InflaterOutputStream/Jbs8369181Oracle.java` | ABSENT | `0aa7b0d58ac0f8dfb4aabec750b313def068d09b` |
| `make/m3/backports/qualification/JDK-8369181_INFLATER_FINISH_RECEIPT.md` | ABSENT | `84009aa9d0c0176b3684a50799458073ab84514a` |

Original Java owner is 9,043 bytes; reviewed donor postimage is 9,329 bytes. This explicitly intended, scoped behavior correction preserves all public Java signatures/JNI exports. It records a private `defaultInflaterClosed` state after owned `finish()` and throws `IOException("Inflater closed")` on invalid subsequent writes, preserving caller-owned Inflater lifetime. No other JDK API or native source may be changed during this source fold.

## Atomic fold instruction
Only after writing this contract, construct a Git tree from the current exact PR #2376 head with the **three existing donor Git blob IDs**, never a textual reimplementation. Create one genuine two-parent commit: first parent = this documentation commit on the receiving branch, second parent = original donor commit `095bd738c377fa597fe95fe94a5029651e94d4bf`. Advance only this isolated review ref with `force=false` and the exact first-parent expected-head lease. Verify both parent SHAs, first-parent 2 additions + 1 modification and no removals, donor byte/mode identity of all three files, unchanged five backport inventory files, and receiving/default branch unchanged.

## Qualification gates
This is NOT production API contract qualification. Donor #2264 separately reports strict Java 21 `--patch-module`, RED baseline, and 27/27 ordinary, interpreted and compiled tests. Those are donor-history statements, not newly executed tests of this combined source tree. **HOLD** canonical Maven/OpenRewrite Recipe.run, fixed-point/refusal, actual M3JDK21 Java21/jtreg/JUnit, JNI/HotSpot/JMOD, native API/ABI, full CI and runtime performance until independently executed on the final combined head. No protected-branch promotion or bulk-PR completion claim.

LeetCode, HackerRank and GeeksforGeeks are algorithm test categories only, not code donors for this JDK fix.
