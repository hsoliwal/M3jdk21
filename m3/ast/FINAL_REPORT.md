<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3AST explicit L0-L5 receiver report

Candidate implementation installed under independently built `m3/ast` only.
The algorithm reuses the source-sealed, first-party Apache-2.0 Synexia
`MIndexASTHierarchy` at the exact receipt bound in `SOURCE_BORROW.tsv`.
No Synexia runtime/service dependency, AST parser, per-atom Entry objects,
new JNI functions or `java.base`/HotSpot source changes are introduced.

L0 per-file facts/atoms, L1 file, L2 package, L3 module, L4 project, L5
repository are now explicit, deterministic and backed by flattened primitive
handle/mask lanes and per-scope coordinate ranges.

The built-in collection family mask supports 13 categories but is not evidence
that all collection classes or JDK interfaces are implemented, audited, or
optimized. `collection_source_census.py` inventories the real M3 collection
source tree as `NOT_ATOMIZED_BY_THIS_CENSUS` until compiler provenance is added.

Local selected-source strict Java21 proof: two repeated receiver compilations;
39,550 checks / 2,000 synthetic source files / 17,967 canonical occurrence
coordinates; stable 64-hex root. Synexia crate additionally verifies exact
source replay, negative controls and namespace-equivalent receipts.

**DRAFT / HOLD**: Maven/OpenRewrite SDK actual run, real MIndex AST receipt
integration JUnit, full JDK reactor/jtreg/HotSpot, native/GC and independent
review are NOT_RUN. No promotion claim or universal speedup.
