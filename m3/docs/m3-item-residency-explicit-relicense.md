<!-- SPDX-License-Identifier: Apache-2.0 -->
# Authority record: item-residency engine (EXPLICIT_RELICENSE_PORT)

This record is filed under `m3/docs/m3jdk21-porting-invariant.md`, mode `EXPLICIT_RELICENSE_PORT`: direct source reuse inside `src/**` needs recorded authority from every necessary rights holder, under target-compatible terms with correct target headers. The machine-readable policy row is in `m3/docs/synexia-intake-policy.tsv`.

## Decision

- **Rights holder:** Hitesh Soliwal, the author of every file listed below and the owner of the Synexia sources ported from.
- **Date:** 2026-10-13, relayed by the session coordinator.
- **Relayed text:** "the new residency engine files … are dual-licensed `EPL-2.0 OR Apache-2.0`. The user is the rights holder."

What the decision means for each case:

1. **Engine files of SWT #122** (`org.eclipse.swt.internal.residency`, `PackedHeapGrowth`, `FileSwapStore`, `SwapPageBinaryOpsSelector`): `SPDX-License-Identifier: EPL-2.0 OR Apache-2.0`, "Copyright 2026 Hitesh Soliwal and contributors". Recorded in the package's `package-info.java`.
2. **Ported Synexia swap files** (`SwapStore`, `TieredSwapCache`, `SwapBudgetPlane`, `SwapPressureController`, `SwapPageBinaryOps`, `JavaSwapPageBinaryOps`, `ParityCheckedSwapPageBinaryOps`): they stay Apache-2.0 with their original notices.
3. **Swing reuse:** the files enter M3jdk21 as Apache-2.0 code in the independent module `m3/ports/swing-viewport` (`INDEPENDENT_APACHE_MODULE`). The route is the hash-pinned rename in `m3/tooling/residency-rename/`.
4. **Copies into `java.desktop` internals (`src/**`):** these are an explicit relicence of the listed files to GPL-2.0 with the Classpath Exception. The rights holder authorizes that relicence for every file below, including the ported Synexia files, which he also owns. Each copy must carry the OpenJDK GPLv2+CPE header and keep a provenance line naming this record and the source commit.
5. **No pre-existing SWT file changes licence.**

## Source

hsoliwal/eclipse.platform.swt, PR #122, branch `m3/item-residency-20261013`, commit `136df9c62ba5555bfb33e7906f2760dc171ac9db`. The Synexia origin of the ported files is hsoliwal/com.synexia `46e296ad537c6ecbd62b9783874e8f1714e278a9`.

## Files

Each row gives the source SHA-256 at the source commit. The SHA-256 after the rename is in `m3/tooling/residency-rename/RENAME_MANIFEST.tsv`.

| Source path (under `bundles/org.eclipse.swt/Eclipse SWT/common/org/eclipse/swt/internal/`) | Licence at source | Source SHA-256 |
|---|---|---|
| `PackedHeapGrowth.java` | EPL-2.0 OR Apache-2.0 | `3c2e690bf022e2f60dc9fa92be14c71fa236096673c7c2c90238b1a2f14b81f0` |
| `residency/ItemBitSet.java` | EPL-2.0 OR Apache-2.0 | `6170200e14d5833800b1b3de296366c45981125a7799e15133d51538409ff97b` |
| `residency/ItemDirectory.java` | EPL-2.0 OR Apache-2.0 | `6b9051ddda785e44c658cd8cfcb7dcc2c6e88ba0d0eb698e4096b20daa00670a` |
| `residency/ItemLanes.java` | EPL-2.0 OR Apache-2.0 | `381e91c28ecf93b2742da5c73edc8e0fff9e79fe19cdc3ab4c319a777d8ee115` |
| `residency/ItemRequests.java` | EPL-2.0 OR Apache-2.0 | `b4f8ef3162a416a7b6ade181f81aac37197a72e36e319873f8ed8c3c86ebcb87` |
| `residency/ItemResidency.java` | EPL-2.0 OR Apache-2.0 | `45fb6ccfb8335d1f8046fd6ce6cd4115dddea18a7ca807c94d16de6c9c2c47b3` |
| `residency/ItemSlots.java` | EPL-2.0 OR Apache-2.0 | `a854f72ceb73786e0c7ebb9710f1a2ee39c12f10a83161eadd7d0c00ad09cb62` |
| `residency/ItemSparseTable.java` | EPL-2.0 OR Apache-2.0 | `3b4a83f87197173d2910619cf708f00c6de1af6e27060bdbc2de1797b6c7dd77` |
| `residency/ItemSwapPressure.java` | EPL-2.0 OR Apache-2.0 | `a60780a72097a3f878576fbc599458fdd10b09dbadae0d0144e7d04d9b52c619` |
| `residency/ItemSwapSpill.java` | EPL-2.0 OR Apache-2.0 | `6d05e459cad5b505fb4ef2180466433b51189c8910ddef77e04b9e37646dd1da` |
| `residency/ItemTreeShape.java` | EPL-2.0 OR Apache-2.0 | `5579d994635ba0a8baf43a7f30fa32556a2b176d26e045dd725155131cce08eb` |
| `residency/package-info.java` | EPL-2.0 OR Apache-2.0 | `15be0c8781f1a7b4542531401048053dcfd3c9618ef7e2fa394d67a5de766d48` |
| `swap/FileSwapStore.java` | EPL-2.0 OR Apache-2.0 | `548ee0380e781dacc895296d2102a1fc764c4f2c38e7a0140a81ebd40ce4cb7d` |
| `swap/JavaSwapPageBinaryOps.java` | Apache-2.0 (ported Synexia) | `a24e9e86773764b91602c487b4191d77a43c1a75622906187f9f7074b53afdd2` |
| `swap/ParityCheckedSwapPageBinaryOps.java` | Apache-2.0 (ported Synexia) | `65fa456138f788b7cc4116ff8c93f338ac5fac5315b5867686216248f7c03660` |
| `swap/SwapBudgetPlane.java` | Apache-2.0 (ported Synexia) | `c3513051f0413555e65ea1e0e092695c15b1da3cd06191d9522eb885bbb5cf47` |
| `swap/SwapPageBinaryOps.java` | Apache-2.0 (ported Synexia) | `acfdf4fbfe464bffe25a6acb7771aebeaf286f4f05307a91acbb8d430381eedf` |
| `swap/SwapPageBinaryOpsSelector.java` | EPL-2.0 OR Apache-2.0 | `02fcf7bc935fe3da0b2175396f9315e7d6afafc841d9425b754ba534c91e64ba` |
| `swap/SwapPressureController.java` | Apache-2.0 (ported Synexia) | `2a3bafa7ce31a85b46f677898918cd5ceab66a8e3982e4a1b60c470c8752a89d` |
| `swap/SwapStore.java` | Apache-2.0 (ported Synexia) | `31aa99f00b772c506c4501dfeb281e367714d7518d78f981b6ac31f3ed47b3b0` |
| `swap/TieredSwapCache.java` | Apache-2.0 (ported Synexia) | `ce0934d116dd67b5fd60aae53345309e09edc449ab7ccec391cb75ebe568cffe` |

## Review status

The authority is recorded. Under the policy, admission into `src/**` stays `REVIEW_REQUIRED` until the target copy shows three things: the GPLv2+CPE header, the provenance line, and a hash link back to this table. No `src/**` file is changed by this record.
