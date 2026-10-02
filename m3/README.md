# M3 immutable text foundation — stage P0

The source-bound migration now also includes a stock-JVM retained-text facade in
[`ports/text`](ports/text/README.md), reusing Synexia's existing Frozen/Joined
owners, and three-way enhancement replay in [`recipes`](recipes/README.md).
Their separate tests do not admit transparent String/HotSpot integration.
The permanent naming map is `docs/name-mapping.json`; read the migration resume
document and the private source inventory before future ports. The continual
optimization goal remains active across milestones.

This draft adds an independently built Apache-2.0 module alongside the pinned OpenJDK 21 tree. It does **not** modify `java.lang.String`, HotSpot, the installed JDK, or existing MIndex/MatIndex code. It is an additive storage substrate, not a String replacement or a compatibility facade.

Original contributions: Hitesh Soliwal <hsoliwal@gmail.com>. OpenJDK retains its existing licenses; see `NOTICE` and `LICENSE` for this separate subtree. No unresolved-license donor sources or external dictionary were imported.

## Build and test

Set `M3_JDK` to a private JDK 21 directory and run `./m3/build.sh`. The script compiles the named module, executes deterministic tests under normal JIT, interpreter, noncompact Strings and C2-focused modes, then builds an original 13-word English **fixture** image. This is not a production English lexicon. `m3/build/` is ignored. The built JDK image can be used as `M3_JDK` after a native OpenJDK build.

Use `python3 m3/recipes/apply.py --check` to verify the source-bound manifest. The same deterministic recipe can install these exact new files into a separate checkout at the pinned runtime baseline using `--target PATH`. It refuses changed inputs or non-identical existing destination files. `python3 m3/recipes/test_recipe.py` tests repeatability and rejection of corrupted inputs.

## Implemented contracts

- `LocalM3Arena` copies caller char/byte arrays into private immutable backing. It is an owner/identity allocator, **not yet a local interner**. It retains no backing cache and imposes no hidden global lifetime.
- `LocalM3StringPiece` records ownership identity, generation, record ID, encoding, UTF16 code-unit offset and length. It strongly owns backing lifetime, including bytes outside a subrange. Numeric IDs are never reusable cache-slot addresses; IDs are descriptive, not access credentials. A new arena gets a new UUID, and its record sequence refuses wraparound.
- `M3StringPiece.join` allocates a flat leaf-reference directory and cumulative integer ends; it copies no character bytes. Nested joins flatten the directory and empty leaves are omitted. Inputs are sealed immutable piece implementations. `flatten()` explicitly creates a normal String at an API boundary; it is not zero allocation.
- UTF16_LE leaves preserve all Java char code units, including unpaired surrogates. LATIN1 leaves interpret bytes unsigned. No unspecified charset transcoding occurs.
- `SharedLexiconImage` opens a versioned file READ_ONLY, checks dimensions and SHA-256, snapshots primitive range/checksum metadata and can explicitly warm the mapping after bootstrap. Each process can map the same file; no network or static String-constructor hook exists. Missing/corrupt shards fail locally and callers can use a local arena fallback.
- Record materialization copies bytes and checks their captured record hash before publishing a local piece. Thus a file change cannot mutate an already published piece. Direct shared-file backing for immutable Strings is **not** implemented: a read-only mapping does not stop another process from changing/truncating the file. Source image files must be managed immutable files; this is not a hostile-file sandbox or OS sealing implementation.

Image bounds are explicit: at most 64 MiB per mapping and 65,536 records. Primitive metadata payload is `4*n + 4*n + 32*n = 40*n` bytes, at most 2,621,440 bytes, plus array/object headers, image identity and mapping/VM metadata. This is **not** an all-in JVM memory limit. There is no pressure-aware cache yet; active pieces and independently opened images can keep additional memory alive.

The image writer uses CREATE_NEW and never overwrites a mapped file. Build output is made read-only. File channels close after mapping; mapping lifetime follows reachability, with no unsupported forced unmap. A future shared canonical backing layer needs a stronger publication/storage protocol before String integration.

The newly available Synexia PR 7388 is reconciled in `docs/synexia-reconciliation.md`; its existing Frozen/Joined/pool owners remain authoritative for Synexia. P0 types are provisional format experiments, not competing replacement APIs.

See `docs/stages.md`, `docs/name-mapping.json`, and `evidence/constructor-pool-retirement.md`. No speedup, complete compatibility, or full jtreg pass is claimed.
