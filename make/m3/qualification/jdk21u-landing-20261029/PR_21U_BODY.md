Layer 1 of the M3jdk21 everything superset: every jdk21u update fix.

M3jdk21 master was jdk-21 GA (`890adb6410d`, 2023-08-09) with none of the 21.0.x update releases. This merge brings openjdk/jdk21u-dev master `8b241db2b629` (3,463 commits; branch `upstream/jdk21u-dev-20261029`) into M3jdk21 as a real two-parent merge. It adds 3,372 JBS fixes.

**Conflicts: 29 paths, all resolved mechanically or ported** (`RESOLUTIONS_21U.tsv`):

| Pattern | Paths | Rule |
|---|---|---|
| FILE_UPSTREAM_SUPERSEDES | 10 | The M3 blob is an earlier upstream revision of the same path (PQC ML-KEM/ML-DSA family), so take 21u. |
| OURS_HUNK_CONTAINED_UPSTREAM | 4 | Upstream already carries the M3 hunk (`os_bsd.cpp`, `os_linux.cpp`, `SunJCE`, `SunEntries`). |
| Upstream family | 5 | The 21u PQC backport replaces the earlier mainline import (`NamedKeyFactory`, `SHA3`, `KeyUtil`, `SignatureUtil`). Upstream 8329667 revised the 8317771 JTree code (`CAccessibility`). |
| Insertion union | 1 | `java.security`: M3 `allowSystemIn` plus the 21u AIA caIssuer filter. |
| M3-owned | 7 | `.github` CI (GitHub-hosted builds) and `README.md` keep the M3 side. |
| Port | 2 | `String.java` and `AbstractStringBuilder.java`, below. |

**The two ports:**
- 8311906 is ported onto the M3Construction helper and the M3 admission paths.
- 8351000 snapshot reads come first, ahead of the R5-BUILDER lanes.
- The upstream `len <= 0` rule comes first in `regionMatches`.

**UPSTREAM_CONTRACT_DRIFT:** `StringUTF16.compress` no longer returns null; it returns the UTF16 array, and the coder comes from the array length. All six M3 call sites that relied on the null result are converted. One of them sat outside any conflict, so a clean auto-merge would have compiled and produced corrupt strings.

**Gates:**
- `jdk_module_gate.sh`: javac `--patch-module java.base` over the conflicted files plus the 25 `java.lang/M3*` files: **PASS** at `dacc61aee7ec`, no error beyond master `3569c790d5cc`. The first run found 6 duplicate KnownOIDs constants from a clean double insertion; fixed by `dedupe_double_insertion.py` (pattern DOUBLE_INSERTION_AFTER_CLEAN_MERGE), and no other auto-merged file has an excess run.
- The full JDK build and jtreg tier1 (`java/lang/String`, `java/lang/StringBuilder`, `sun/security`) run on the GitHub-hosted CI of this branch.

**Atoms:**
- `hunk_contained_upstream.py`
- `merge_from_resolutions.sh`
- `jdk_module_gate.sh`
- `jbs_pull.sh`
- 6 rows in PATTERNS.tsv

**Next layers:**
- Layer 2: 11,331 JDK 22+ fixes are missing. 9,490 are code, 1,004 are test-only, and 837 are removals or deprecations, which stay as they are in 21.
- Layers 3 to 5: JEPs, then the removed and vetoed features, then vendor features. The donor inventory for these is running.
