# M3JDK21 filtered and sorted stored-representative receiver

**Product base:** M3JDK21 [PR #1028](https://github.com/hsoliwal/M3jdk21/pull/1028), exact head `1dfca30fb4b5dae03ec5dc719a796cf9f6b664fd`. Do not rewrite or merge the parent. Canonical authored source and recipe: [Synexia #12009](https://github.com/hsoliwal/com.synexia/pull/12009), named recipes `com.synexia.m3.StoredRepresentativeOwnerRepair20261017` and `com.synexia.m3.Jdk21StoredRepresentativeRepair20261017`, existing Maven/OpenRewrite `M3HashPinnedJavaSnapshotRecipe`, exact SHA-256 preimages/postimages. Runtime ownership stays in `com.m3.collections`, not an external dependency. First-party Apache-2.0 rights and attribution preserved.

## Before vs after (actual source change)

| Scenario, using comparator-case-insensitive TreeMap/TreeSet | Before on parent #1028 | After on this branch |
| --- | --- | --- |
| Stored `ADMIN` hidden by lowercase predicate, query `admin` | `contains` incorrectly **true** | Correctly **false** |
| Stored map key `ADMIN` hidden, `get("admin")` | Can return hidden value | Returns **null** |
| Mutable filtered Map `put("admin", positive)` into hidden stored `ADMIN` | Can mutate row that remains hidden | Refuses before mutation |
| Mutable Map `putAll({"ADMIN":-1, "admin":2})` with key-dependent value gate | Can validate aliases separately and partially create an invisible final row | Validates effective retained first key for all rows, refuses before writes |
| Stored lowercase `admin`, lookup uppercase `ADMIN` | Could hide valid actual key | Correctly visible |
| `IdentityHashMap` Set with equals-equal distinct stored references | May test lookup alias instead of stored object | Resolves actual owned reference first |

All public method signatures, comparator ordering, range/descending views, strict read-only mutation refusal and user predicate policies remain unchanged. Sorted/Navigable sources use JDK comparator navigation, `O(log n)` expected with TreeMap/TreeSet. **Generic Collection/Set membership becomes `O(n)` when necessary to resolve real owned members**, so benchmark it against any existing hash fast path before claiming an overall speedup. `putAll` may allocate a temporary TreeMap of only keys to prevalidate comparator collisions; never retains per-element wrappers, indexes or a second backing store.

No new JNI ABI, object pointer or per-item JNI predicate callback: reuse already-authorized numeric `M3IndexMaskCompactor` for separate primitive batch problems. Exact four received postimages are sealed in `verify_receiver.py`, and `verify.sh` compiles/executes real Java21 sources in normal and interpreter mode. Full Maven/OpenRewrite Recipe.run SDK qualification, full M3JDK collections reactor, GC/JNI/integration and matched JDK image are separate, still-pending promotion gates. **Do not merge/mark production-ready on this focused oracle alone.**

## Fully source-sealed installation receipt

The historical original installed-source ledger and the preceding six-file
filtered/sorted overlay remain immutable. A further **canonical Synexia TEXT
recipe** `com.synexia.m3.Jdk21StoredRepresentativeMetadata20261017`
(pinned with the two Java recipe crates in Synexia #12009) changes precisely
`m3/collections/verify_donation.py` and creates
`m3/collections/recipe/stored-representative-20261017/manifest.tsv`.
The new manifest replaces exactly two already-approved source SHA-256 values
and admits precisely two new test files. Both bytes are read back from the
source-bound text postimages; no direct handpatch of emitted runtime code.

`test_receipt.py` **executes the actual resulting donation verifier**
rather than merely checking snippets. It checks the six original source rows,
two exact replacements and two additions against the current installed files,
and proves altered receipt bytes, stale preimages, occupied test names, and
symlink receipts fail closed. A source seal without real full-module JUnit/JNI
is still not whole-library or JDK acceptance.
