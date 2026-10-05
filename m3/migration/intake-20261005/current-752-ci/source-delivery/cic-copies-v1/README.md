The Cic increment is fully represented by exact source and delivery bytes: **18 source paths plus 26 outer portable paths**, extending the immutable 261-path census to **305 distinct declared source paths**. The 44 new paths total 922,267 bytes. All 26 outer copies total 792,012 bytes. No whitelisted Cic body is missing.

The source 18 comprises 14 exact task files, the admission, and three authoring/qualification/spec records. The runner inventory was always 14; the earlier 15-file arithmetic estimate was incorrect and required no runner change. Four source metadata/admission files are mapped to exact loose portable aliases; the 14 task files map to their exact `cic-authoring-v1/` logical source entries. The supplemental audit also records active receiving and candidate-evidence byte matches.

All 100 logical files in the Cic archive were reconstructed and verified from their hash-addressed chunks, including the admission and all task bytes. Parts, members, shards, chunks, full files and top-file identities were checked. The unchanged append helper independently verified the selected inner files and all physical source/delivery copies while producing the companion.

| Artifact | SHA-256 |
|---|---|
| Prior known 261 companion | `41230714370f2c21ecc2738f4e0ea156ccc076c10a03cad3300e76a30456d771` |
| Source18 whitelist | `8dc5abcff5a3168bcb7f4fc918cb45dc60e91142676b9d37f4f8efd4937db1fe` |
| Portable26 whitelist | `f88ab53834f00226c4cfe0e567b753580fff53f6f8d2d1d02c93942734fbbe80` |
| Portable26 M3 mapping | `c76b34dd8acb1060e9a812329d1e4a640e56650d15231758c325b65677c4feed` |
| Cic bundle index | `0513d1819d9c6f49031049846c48a544f8c44325a2c69d0da36c546124fb3f80` |
| Exact source 18 mapping | `18d82beebf3cce690e96b0870f5009636f3298ea868875db10010557ca744549` |
| Append plan | `f4f5bc1f83c312aa9e6aa2ca49b55fc2e6c377021556e0fc6adef859ded064c3` |
| Known305 companion | `4e6b41c9f13931272dbd82f18c8a53b179b0f1293f135bb722bb8056f642d084` |
| Supplemental delivery audit | `bf20f0bf23f47e4b1f3cd581423d106fdc50c56ce53c7cb4d01e10a351031dce` |

There are **two additional provenance-envelope omissions** outside the declared 305 source paths: the enclosing `CIC-PUBLICATION-WHITELIST.json` and `PORTABLE-CIC-WHITELIST.json` documents themselves. Their exact bytes are absent from the current candidate and Cic loose/inner bundle entries. `delivery-audit.json.publicationAdditions` selects the original immutable files, totaling 26,255 bytes, for these receiving paths:

| New provenance-only receiving suffix beneath `m3/migration/intake-20261005/current-752-ci/` | SHA-256 | Bytes |
|---|---|---:|
| source-admission-evidence/source-whitelists/CIC-PUBLICATION-WHITELIST.json | `8dc5abcff5a3168bcb7f4fc918cb45dc60e91142676b9d37f4f8efd4937db1fe` | 11585 |
| source-admission-evidence/source-whitelists/PORTABLE-CIC-WHITELIST.json | `f88ab53834f00226c4cfe0e567b753580fff53f6f8d2d1d02c93942734fbbe80` | 14670 |

These two rows are separate from the first census's 25 pending selections. The chain carries those original 25 unchanged; a final transport selector must read the two new envelope rows from the supplemental audit as well. The declared-source total 305 intentionally excludes these enclosing metadata documents. Counting every selected physical envelope path is a separate publication inventory.

`coverage-append.json` is the known 305 chain entry. `source-mapping.json` supplies one exact delivery per source 18 path. `delivery-audit.json` retains all source 18 active/evidence/inner/outer matches, the 100-file archive inventory, exact producer/bundle/receiver-manifest seals, and the two additions.

Candidate-v4 matches are observations of sealed bytes, not a PASS claim. No receiving result was inferred while its full suite was running. Earlier 234 and261 censuses, all proofs, source/candidate files, and archives remain unchanged. Only read-only research collection ran; no producer or consumer gate was invoked.
