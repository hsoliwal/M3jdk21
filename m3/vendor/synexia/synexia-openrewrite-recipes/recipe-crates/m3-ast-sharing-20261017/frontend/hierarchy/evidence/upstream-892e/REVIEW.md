# Additive upstream leaf-index owner review

The exact reviewed `MIndexASTLeafIndex.java.txt` is canonical source from `hsoliwal/com.synexia` commit `892e3f6c51d78bc0948e8a8cf736055b4c70eafa`, path `synexia-mindex/runtime/src/main/java/com/synexia/mindex/ast/MIndexASTLeafIndex.java`. Git blob: `fbce9f518890cd144bd4576e97031f017563e07f`. SHA-256: `d60d18ce634d0a935727bc29fb9dbc41f06e8284760cdae7b93f1586ebf613d9`. Its bytes and license are unchanged. It was inspected after hierarchy source qualification and is not a compiled or modified product of this packet.

| Contract | Existing upstream leaf index | Qualified lazy hierarchy |
| --- | --- | --- |
| Levels | Actual L0 facts, L1 file, L2 package, L3 module, L4 project, L5 repository | The same authoritative containment levels |
| Input | Detached source SHA, atom IDs and state words supplied by the caller | Full source/context/provider/schema/phase facts plus an existing frozen AST supplier |
| Representation | Immutable primitive row/edge/atom lanes; contiguous physical windows | Immutable qualified-address plan with primitive child offsets/ordinals and separately cached provider vectors |
| Selection | Required/forbidden masks return stable physical offsets | Provider boundary may consume a detached selection projection; no state-mask engine is added |
| Construction | Eager bounded-by-Java-array-size rollup construction over supplied facts | Explicit configured graph/work budgets; publish performs metadata work, acquisition resolves lazy facts |
| Invalidation | Content digests of supplied containment inputs | Local dependency closure, full context identity, precise changed-address/ancestor seals and stable unchanged record IDs |
| Cache/storage | No partial master or persistent store | Reuses existing partial master/cache and leased TieredSwapCache/SwapStore frames |

The new upstream class is relevant existing functionality and must not be described as merely a single-file selector. Its physical state/offset projection is a plausible provider input or output. It does not by itself replace the qualified lazy resolution and dependency cache contract. No adapter is claimed or generated in this finite packet; integrating one would need its own source-qualified API and tests, especially for atom-owner/context admission and malformed UTF16 identity handling. No finding here changes the existing global JDK receiving sequence or asserts semantic-equivalence proof.
