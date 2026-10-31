<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Public names in M3JDK21

**M3JDK21 is the JDK product**, not a `M3 + JDK` facade. The original
`java.lang`, `java.util`, `java.util.concurrent`, `java.nio`, Java desktop,
compiler and other public JDK types retain Java/JDK names and contracts.
A selected faster implementation is admitted *inside* the original class,
not shipped as a competing `M3HashMap`, `M3String`, or `FooV7` public type.

## Placement and names

- **Public API:** established JDK naming, existing descriptors and module
  ownership. A new `java.*`/`javax.*` `M3`-prefixed public type requires
  refusal and a corrected design. No mechanically numbered or dated API names.
- **Internal owners:** existing `jdk.internal.*`, `com.sun.tools.*` and
  JDK-package-private M3 array, index, offset, mask, and native helpers may
  retain names needed for compatibility. Their visibility is not silently
  enlarged. Reuse these owners rather than building duplicate Entry/Node
  storage or a competing public API hierarchy.
- **Proving-ground classes:** existing `com.m3.*` source families have their
  own internal admission path. They do not become JDK public APIs by renaming
  a package or copying a class. Keep experimental code and recipes in Synexia.
- **Tests and CI:** descriptive JDK-style Java test names and stable,
  purpose-based workflow paths. No new date/version suffixes such as
  `-20261022`, `V7`, or `-v2`; chronological history belongs to Git commits,
  manifests and canonical Synexia recipe receipts.
- **Mapping:** `m3/docs/name-mapping.json` is the sole authoritative
  migration/owner registry. Do not introduce new `name-mapping.d` fragments.
  Existing historical files remain readable for provenance until separately
  migrated through tested, source-pinned recipes.

## Canonical recipe ownership

**Only Synexia** (`hsoliwal/com.synexia`) develops, improves, tests,
combines and executes Maven/OpenRewrite transformation recipes, donors,
catalogues and private proof experiments. M3JDK21 and the other public
product forks receive the **results of qualified executed recipes**:
actual original-owner Java/JNI/HotSpot implementations, ordinary product
tests, licensing, provenance and runnable acceptance evidence.

No newly authored recipe crate, OpenRewrite `Recipe.java`, recipe
tests or `META-INF/rewrite` descriptor may be received as a public
product result. Pre-existing historical public recipe source remains
readable for provenance but cannot be modified or further developed in
the public fork. Source-changing successors belong in Synexia.

This exclusion does not block existing original JDK classes, internal
array/index/mask/JNI implementations, stable jtreg test names or
read-only product verification scripts. Recipe execution must be
source-sealed in Synexia and the output separately qualified inside
the actual M3JDK21 SDK, without becoming an M3-on-JDK wrapper.

## Exact change gate

`m3/compatibility/check_public_names.py` checks only newly added, copied,
or renamed Git paths. It refuses unstable path components, new naming-map
fragments, a newly introduced `M3*` Java/JDK public type, and JDK/jtreg
package or type/name disagreement. A modification to a *preexisting* type
cannot justify renaming it: such changes still require source-sealed Maven/
OpenRewrite generation, public ABI, behavioral, JNI/GC and image proof.
No baseline-wide automated rename is performed; that would risk lost APIs,
license notices, reflection consumers and native symbols.

```sh
python3 -m unittest discover -s m3/compatibility -p test_public_names.py -v
python3 m3/compatibility/check_public_names.py --base <base-commit-sha> --head <head-commit-sha>
```

The workflow checks exact PR commit IDs with complete Git ancestry.
This lexical preflight is intentionally narrower than source/ABI proof;
real compiler, jtreg/JCK, runtime/image, resource and native gates remain
mandatory before public JDK promotion. The canonical recipe and donor
catalogue are maintained in `hsoliwal/com.synexia` and only qualified
final product code and appropriately named target tests are received here.
