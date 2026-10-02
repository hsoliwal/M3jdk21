# MIndex-to-M3 owner-port slice

**Partial implementation, not completed migration or a replacement JDK.** This
branch ports six existing production owners and adds an explicit Java 21 facade.
The root `m3/docs/name-mapping.json` remains the sole migration naming/lineage map.
Start future work with that map and `RESUME.md`.

## Execute the actual acceptance slice

From the target repository root, using a complete Java 21 JDK:

```sh
java m3/migration/Verify.java .
```

This compiles with `--release 21 -Xlint:all -Werror`, runs the recipe/manifest/
inventory tests, tests the port and an independently compiled pinned source
preimage, exercises named-module consumption, and replays/rolls back the port in a
temporary directory. It makes no downloads and never replaces an installed JDK.
It exits nonzero on a failing command and prints its retained command log path.
The preimage test verifies six authenticated-source content hashes; it is not a
complete Synexia reactor build or an ancestry claim. Existing P0 download gates
are not replaced or silently bypassed.

Maven integration is provided, but was **not executed locally** (Maven unavailable):

```sh
mvn -f m3/migration/pom.xml verify
mvn -f m3/migration/pom.xml compile exec:java -Dexec.args="plan TARGET_ROOT SOURCE_CHECKOUT TARGET_ROOT"
```

The second command's first TARGET_ROOT is the recipe bundle; source and target must
be separate checkouts. `plan` is read-only; `apply` is explicit and create-only.
The source checkout HEAD and each source file hash must match the map. `--snapshot`
permits file-hash-only inputs explicitly, never implying checkout ancestry.

This is a class-backed exact-source Maven-invocable recipe, **not an OpenRewrite
Recipe subclass**, not a compiler transformer, and not repository-wide enforcement
of the recipe-first invariant. Maven remains tooling outside the OpenJDK build.

## Layer and compatibility decisions

The module is `com.mthree.indexstring`, exporting `com.synexia.indexstring` and
`com.m3.indexstring`. The compatibility classes retain their original public names.
Do not install this module alongside the original same-package source owners:
that creates duplicate classes or split packages. The new module name avoids a
strict compiler module-name lint failure; Java package names are unchanged.

`M3Text.fromString`, `of`, `fromLegacy`, `asLegacyView`, retained concatenation,
UTF-16 slices, repeat, comparisons, hash/search, regex, encoding and output methods
use the existing frozen/joined storage owner. The facade owns no text payload.
Mutable input arrays are copied by the frozen owners. Mutable outputs allocate
independent arrays. Stock `String` output is a deliberate materialization boundary.

Legacy `.of(...)` preserves content-and-segmentation canonicalization. Additive
`.ofRetained(...)` normalizes empty and adjacent same-owner ranges and does not
replace a supplied owner merely because its text is equal. These are deliberately
distinct identity policies in the same interner. IDs are process-local, non-reused
on overflow, not persistent OS coordinates or `String.intern` identities.

## Costs and remaining storage limits

Flat directories bound structural depth, not allocation or memory usage. Joining
walks segment descriptors. Partial boundary ranges can additionally scan UTF-16
units to compute hashes; adjacent coalescing computes powers of 31, and canonical
bucket lookup adds collision/owner-comparison work. Slicing a joined view retains
the entire original body, including owners outside a tiny logical slice.

Sequential comparison/search uses segment cursors. Exact fallback search is
O(n*m + s), where n is text length, m pattern length and s participating segments.
Random `charAt` uses a binary search of cumulative segment ends. Encoding uses the
existing whole-text encoder with replacement policy, not independently encoded
fragments. No encoding cache or cryptographic-hash composition is introduced.

Weak interning is **not a configured hard memory budget**. Equal-content distinct
owners can share a fingerprint bucket and incur linear bucket work. No OS file
publication/generation loader, external-truncation defense or multi-process
lifetime acceptance is added here. Package-private direct-buffer admission remains
a trusted boundary, not proof that arbitrary read-only memory is immutable.

## Permanent enhancement workflow

The schema describes multi-reference mappings and optional lineage events; this
first executable recipe accepts one source and one target per atom. Splits,
merges, deletion/tombstones and reverse ports require reviewed mapping changes.
Do not confuse schema support with automatic transformation support.

`ManifestValidator` checks pins, paths, target/resource hashes, dependencies,
receipt-covered production hashes and unmapped additions in this port's source
subtree. `--complete` deliberately refuses while pending families/gates remain.
It does not yet enforce whole-repository remote freshness or resolve Java symbols.
`SourceInventory` parses all supplied Java/resource paths with the JDK compiler,
records declarations/imports/diagnostics and refuses symlinks. It performs no type
attribution; imports are candidates, not a proven external dependency graph.

`EnhancementPlan` compares mapped source/target hashes with the last recorded
baseline, marks conflicts and deletions, and computes downstream dependency closure.
It is read-only, does not prove semantic equivalence, does not validate new checkout
ancestry, and does not automatically process multi-reference mappings or new files.
Run a fresh complete inventory before planning a future port.

## Publication and provenance

Only the user-authorized production owner closure and its related migration work
are ported from the private source to this public target. Source SPDX notices and
pinned content hashes are preserved. No unrelated private fixtures, credentials,
datasets or challenge-site code are included. Existing OpenJDK notices and build
files remain authoritative; this addition does not relicense OpenJDK.

See `COVERAGE.md`, `FEATURE-MATRIX.md`, `PROVENANCE.md`, `PERFORMANCE.md` and
`receipts/local-java21.json`. Tests and receipts describe exact local code-file sets,
not a complete JDK image, hosted CI, Windows/native conformance or all three routes.

## Generated declaration reports

The versioned `source-closure-inventory.json.gz` and `target-closure-inventory.json.gz`
are deterministic gzip-compressed JSON, not executable archives. The canonical map
records both compressed and uncompressed SHA-256 values. The supplied review bundle
also contains their plain JSON forms. They enumerate the six admitted production
source files and eight port files only, not the full source repository.

To refresh, run `SourceInventory` against an exact source root with an output outside
that root. Preserve the original report for three-way review; do not overwrite it
with an unpinned checkout. Standard `java.util.zip.GZIPInputStream` reads the stored
reports. The gzip round-trip was verified byte-for-byte before publication.
