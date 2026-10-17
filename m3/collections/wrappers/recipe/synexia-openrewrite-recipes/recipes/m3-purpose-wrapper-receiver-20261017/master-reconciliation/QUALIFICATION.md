<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Qualification: current-master collection wrappers

## Exact scope and executed result

The canonical recipe was qualified first, then executed through the actual OpenRewrite SDK against an isolated checkout of M3JDK21 master `970a34350ebdf6a3f1c5ff5acc8445d0f3515f55`. The final packet generated exactly **48 files: 32 typed Java targets and 16 native/build/catalogue/custody text targets**. Its second SDK pass produced no changes, and the actual Git changed-path set matched the manifest exactly. The final installed wrapper Maven build, its Java/JNI tests, and the retained collection acceptance gate all passed locally.

This source work stacks on the existing Synexia purpose-wrapper family at `829fd9499112d979f139d1b9c95871d6bb64ddaa` (PR #11949). Current master and the historical generic receiver PR #992 have different trees. Only the reviewed missing owners and exact primitive snapshot refinement are received; current master is not replaced with the historical branch.

| Sealed input | SHA-256 |
| --- | --- |
| Java transformation manifest | `31d3d4c4de0ec47610bc04f54d3edf1ad94388b6ccf01beb7abf7d9702668248` |
| Text transformation manifest | `0eda022d1413dce7c291961df4005fe4aa0211c966e13c635b48eff83b3cd94c` |
| Current installed collection inventory | `989adce813571579836646a0e2c1527e3bf0aef531af0c16476dabb9e5911a5b` |

The Java/Text snapshot engines and their two scope providers remain byte-identical to the canonical parent. The new binding and application adapter select their existing capabilities. This is **SDK_EXECUTION**, not a text-only kernel replay.

## Executed gates

| Gate | Result and evidence |
| --- | --- |
| Canonical Maven/OpenRewrite qualification | 10 tests; no failures/errors/skips. Exact typed postimages, unrelated source preservation, ordering and two-worker independence, refusal before source writes, partial replay, fixed point, and catalogue search. `evidence/recipe-sdk.log.txt`. |
| SDK receiving | 48 generated files, zero second-pass changes, no unrelated changed paths. `evidence/SDK_EXECUTION.tsv`, `sdk-apply.log.txt`, `installed-qualification.json`. |
| Installed wrapper Maven proof | 90 explicitly selected package units plus the product module descriptor discovered by javac; 23 wrapper proof Java sources; 18 tests with no failures/errors/skips. `evidence/wrapper-java-jni.log.txt`. |
| Retained collection acceptance | 91 main + 34 existing test sources; 156 JUnit tests with no failures/errors/skips; both standalone self-tests; module requires only `java.base`; exact packaged license/provenance, native parity and allocation checks. `evidence/legacy-qualification.json`, `legacy-verification.log.txt`. |
| Metadata refusal/custody regressions | Five tests passed. `evidence/metadata-unit.log.txt`. |
| Received and retained JNI peers | Generated-header ABI, strict C17, fatal UBSan, and `-Xcheck:jni` all passed. Four native logs and `native-exports.txt` are retained. |

The installed wrapper suite includes the retained 313,767-check purpose-wrapper corpus, 862,420-check map corpus, 7,068-check snapshot-fusion corpus, 7,683-check cursor integration corpus, 2,432-check primitive donor corpus, and independent 258,021-check Java primitive oracle. The new filtered traversal regressions execute seven cases with 27 assertions; the recovered map composition proof executes 2,444 checks across six real owner profiles. These finite assertion counts describe their individual corpora; they are not a claim of independent statistical samples or universal API coverage.

Native execution reports 166,656 map parity checks (41,478 native calls, six refusals), 314 run-prefix boundary checks, 45 retained map-cursor checks, and 260,838 primitive Java/native index-parity checks. The production primitive mask selector is unchanged. The two received C peers are verification-only; Java predicates remain in Java.

## Corrections were made in recipe inputs

The candidate regressions distinguish old and corrected behavior. Six of the seven focused filtered/flattened traversal cases fail on the exact earlier owners and all seven pass on the candidate. Old map composition lacks the nine recovered typed factories, and the old size counter overflows its documented saturation contract. The native premature-return mutant admits a later invalid run and fails the retained boundary oracle. Public API comparison preserves the existing generic owner signatures; recovered overloads and primitive snapshots are additive.

Integration exposed two proof-wiring defects before publication. The original wrapper POM rejected the established `com.m3` module-name warning; its template now reuses the retained verifier's `-Xlint:all,-module -Werror` policy and explicit classpath test compilation. The original native command pre-included generated headers but omitted their include search directory; its script template now supplies that directory for the unchanged donor's own header include. Both corrections were made in canonical templates, resealed and qualified. The entire corrected recipe was replayed into a fresh master checkout. No generated target file was hand-patched.

## Catalogue and source custody

The existing eight-column algorithm catalogue retains historical decisions and adds dated category comparisons for saturated counts, lazy flattening, raw map queries, typed composition, primitive sorting and bounded native validation. Literal case-insensitive search is provided by the Maven adapter. Every one of the 125 receiving module Java paths has a file-pass disposition: nine admitted/refined runtime owners and 116 preserved master sources. Preserved paths are not marked as optimized or exhaustively reviewed.

The successor inventory repairs two stale hashes and declares 23 files already present on master, then admits the eight generic additions. Its 130 inputs comprise 125 module Java sources, two original standalone probes and three original native/license inputs. The prior inventory and historical JSON entries remain intact.

All implementation bodies selected for receiving are first-party Apache-2.0 Synexia/M3 source, with original notices retained. The map combines two same-license source revisions while retaining the later projection cursor. Exact donor paths, commits, Git blobs, adaptations and rejected candidates are in `source-review.json.txt`, the category data, and the appended file-provenance ledger. The existing collection NOTICE is preserved under `donor-notices/`. GitHub/Guava comparisons and LeetCode, HackerRank and GeeksforGeeks problem statements supply category/contract ideas; no challenge submission or tutorial implementation bodies are copied.

## Measurement and acceptance limits

The recorded raw-query allocation experiment used six alternating fresh JVMs per variant, a JDK `LinkedHashMap` with 4,096 entries, 80 warmup calls and 64 measured calls per operation, and disabled escape analysis. `hashCode` and a missing `containsValue` query fell from 3,155,968 to 2,048 measured bytes per 64 calls; `size` fell from 4,608 to 2,048 and `isEmpty` from 6,656 to 2,048. The candidate still allocates 32 bytes per call in this probe. Callback traces match the earlier owner exactly, SHA-256 `3f88ccf87b350d935459e21f0697e8d5f4e0532a65560b3ad3cae40f56338c24` over 252,336 characters. Measurements, commands and raw observations are retained in the source-review resource.

Whole-process CPU observations include JVM startup, setup, warmup, all four operations and teardown. Neither those observations nor the allocation probe establishes normal-JIT application speed. The bounded run-search peer's 4,096-byte C array scratch excludes its frame, JVM/JNI bookkeeping and Java input arrays.

The source-pinned mirror is installed by the separate custody packet only after this canonical source revision exists. Its SDK qualification and final published Git blob checks are reported separately. Hosted source and target workflows must execute at their actual published heads; historical or queued runs do not count as passes. Full source-derived reactor, installed-JDK, jtreg/HotSpot and whole-JDK performance acceptance were not run here. String remains the active ordered phase, and no phase advancement is claimed.
