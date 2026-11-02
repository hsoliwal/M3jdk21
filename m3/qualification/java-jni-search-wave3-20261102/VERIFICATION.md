<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 exact Java21 token-cursor/JNI source-union verification

2026-11-02. **Integration branch merged, default `master` NOT merged; all Java21/JNI runtime gates HOLD.** This is a bounded additive successor to the previous seven-PR [#2337](https://github.com/hsoliwal/M3jdk21/pull/2337), not complete fleet-wide verification.

## Source parents, Git commits and exact changed-path proof

- Original receiving #2337 head `9b139f3720d12242c9ac7f160ab5da443f4474c5`, original base `master@ce005d25b29fb6246e013bffb462afd5ed070362`. Its complete compared source delta is **64 paths (36 added, 28 modified, zero removed)**.
- Donor [#2341](https://github.com/hsoliwal/M3jdk21/pull/2341) exact head `1f07831c0146facf16dfc051d38c23023f8f14b2`, containing its original one-lane and multi-lane token cursor source histories. Its master source delta is **38 paths (37 added, 1 modified, zero removed)**; changed-path intersection with receiver's 64 paths is **empty**.
- Documentation-first receiving branch commit `906a16a24679be4d04ce91eec14eab2c8da72dc7`, preserving the exact original #2337 tree plus the [source merge contract](README.md).
- GitHub-native integration transport [PR #2343](https://github.com/hsoliwal/M3jdk21/pull/2343) was merged with exact donor-head SHA guard and method `merge` (not squash/rebase). Verified actual two-parent commit `8ad8fb63bf879d55983c580189b3d9132fe79009` with parents `[906a16a24679be4d04ce91eec14eab2c8da72dc7, 1f07831c0146facf16dfc051d38c23023f8f14b2]`, tree `cfa987bb2307d4b47cd2374cbd9bd940c506552d`. The new integration branch ref was independently read back.
- Complete exact `compare(previous receiving commit → merge)` filename/status set matched `compare(frozen master → donor head)` **38/38**, **0 missing, 0 extra, 0 renamed, 0 deleted**, with only `m3/ports/regex-cursor/src/main/java/com/m3/regexcursor/M3TokenCursor.java` modified. No receiver source paths overwritten by an unrelated donor.
- The Java owner `M3TokenCursor.java` had **10 public/protected declaration lines** on both frozen master and the merged result; their ordered signature text was identical. This structural signature witness does not certify bytecode/serialization/JNI event behavior.

## Exact source Git-blob witnesses (all PASS)

| Witness | Expected Git blob on original donor and merged tree |
|---|---|
| `M3TokenCursor.java`, from #2341 | `5d03f64b310c2cc9ed232cfb91ce8eea0e0e509a` |
| `M3StaticGrammarLexiconJni.c`, donor test native source | `ace33d49abd1974f1e7a35c13070bc0e704d7562` |
| `VInt.java`, existing packed Java codec from #2337 | `159069a2bb959868253df3fdfee2bf024878a992` |
| `m3_packed_codec.c`, existing packed C/JNI codec from #2337 | `1813a024d4c2ce514cebe4928ab4379b7950ff94` |
| Packed sizing receipt, existing #2337 | `055a4b29e7003bdd8afc3185960fcc0f402508af` |

All five were fetched from their respective original parent and actual merged tree. Both Git blob identities and complete source bytes matched.

## Already-contained codec ancestry — no redundant source merge

[M3JDK21 #2325](https://github.com/hsoliwal/M3jdk21/pull/2325) at head `1edbbda245dea9a616feb37ecea43ef4c0ef050c` is a **genuine ancestor** of the original #2337 head. Git compare `#2325 → #2337`: **46 ahead, zero behind**. Its three source/receipt Git blobs matched those already in #2337 exactly. Rewriting those already-integrated Java/JNI codec files or creating a duplicate parent would be semantically unnecessary.

## Search categories and unresolved release conditions

LeetCode/HackerRank/GeeksforGeeks taxonomy distinguishes first-match, overlapping counts, ordered all-offsets, lower/upper bound, and token-window search. They provide problem shapes and independent oracles, not unrestricted copyrighted solution-source rights. Original Synexia Java/JNI Maven/OpenRewrite recipe and license-pinned GitHub donors remain authoritative.

**NOT EXECUTED:** actual module Java21 compilation, source SDK JUnit fixed-point/refusal, original `M3TokenCursor` single/multi-lane/seek tests, JNI `-Xcheck:jni` grammar and direct buffers, native ABI/JMOD binary compatibility, full M3JDK21 reactor and platform/runtime benchmark. This draft must not merge into `master` while those gates are missing. Public Java/JDK source/binary/API/observable contract remains locked.
