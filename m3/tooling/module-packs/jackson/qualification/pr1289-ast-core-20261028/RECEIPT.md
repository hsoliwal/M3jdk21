<!-- SPDX-License-Identifier: Apache-2.0 -->
# Chronological PR #1289 — exact-source primitive AST core receiver

Target: M3JDK21 `master@92962fbba10cbf13799253a307dc90f4de94b179`
Original: [#1289](https://github.com/hsoliwal/M3jdk21/pull/1289), source HEAD `5ec89ae530defe1c7df70e9b3ffd985b9e21f293`.
This is a partial, *active source* receiver of original immutable Java AST atoms.
No Synexia Maven/OpenRewrite recipe classes, SDK or third-party runtime dependencies
are copied into this public product repository.

## Exact source blob inventory

The existing source-sealed Synexia receiver rules apply. These four *unchanged*
Git blobs have no target-path preimage on the pinned master. They come from the
same exact historical PR, and their original license/provenance headers remain.

| Target path (relative to `jackson/`) | Donor Git blob |
| --- | --- |
| `src/main/java/jdk/m3/jackson/M3AST.java` | `8edfc771a04743a507d5f799bd0b41e91180287e` |
| `src/main/java/jdk/m3/jackson/M3ASTAtom.java` | `59727364b4d30c55326215b326f5d2763c6cd14f` |
| `src/main/java/jdk/m3/jackson/M3ASTBuilder.java` | `0a26b63a780bf303f807e60379864ec905e51f0e` |
| `src/main/java/jdk/m3/jackson/M3ASTPool.java` | `66d9bd48d99423dc648dbe72ded5ace2c147c08e` |

Already on master (not duplicated): `M3JsonLimits.java` blob
`c08f91664245759463c654f244cdf2bfaf19fbb3`.
The four sources depend on the JDK and one another only (plus that existing record).

## Qualifying gates

`.github/workflows/m3-json-pr1289-ast-core-java21.yml` verifies original
Git blob identities, then compiles the original classes plus the existing limits
record and `M3ASTCoreReceiptContract` with
`javac --release 21 -proc:none -Xlint:all -Werror`, then executes the
no-framework behavioral contract. The contract covers primitive hash-consing,
duplicate field occurrences, exact child ordering, source-sealed identity,
structural hashes, one-time sealing, bounds and failure-atomic admission.

The only authored Java target is the additional no-framework *test*; the four
production sources are byte-identical to original #1289. The source is unmodified
rather than reimplemented or normalized. Full Jackson databinding, POM/JMOD,
HotSpot/JNI and the entire JDK build are **not** qualified by this narrow gate.

## Held descendants

Do not apply or merge the original #1289 branch as a whole. Current master owns
newer `M3File.java`, `M3ObjectMapper.java`, `build.py`, `pom.xml`,
`module-info.java` and `README.md`, so a whole-file choice would lose
existing code. Another 38 original paths remain pending individual admission.
These four classes have no normal public construction path until the existing
Jackson mapper's AST APIs have been reconciled, using canonical private
Synexia recipes, actual source/ABI proof and real source-retention checks.

This commit adds source and test only, on a draft review branch. No target
default branch, historical PR head, public recipe owner, API contract, JNI
export or source history is replaced or force-pushed.
