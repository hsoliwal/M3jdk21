<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 #2064 declared-dependency quarantine: chronological landing recovery

Target preimage: `2028b1fb5f422c14e024da14b8e5408dde539de6` (post [#2070](https://github.com/hsoliwal/M3jdk21/pull/2070)).
Historical source: [#2064 patch](https://github.com/hsoliwal/M3jdk21/pull/2064.patch)
and exact historical head `c0d2553d69a925a0072856f679dc854ccf9f1400`.
Private canonical recipe:
[Synexia #13780](https://github.com/hsoliwal/com.synexia/pull/13780)
`M3HashPinnedTextSnapshotRecipe`.

## Why this recovery is required

Historical #2064 is Git-ancestry-contained in `master`
(`ahead_by=0`); GitHub closed it *without* a per-PR merge record after #2070.
The original 5 generated target paths were not the active master tree:
three new files were absent; the POM and README retained their original preimages.
This is an exact **five-target postimage replay**, not a second implementation
or a claim of new Git ancestry.

## Source custody

| Target | Exact original #2064 postimage Git blob |
| --- | --- |
| `.github/workflows/m3-no-external-consumer-deps.yml` | `81c237f93da86aafa026099b9c799d0d99b09537` |
| `m3/collections/interop/README.md` | `2f7104f1a694e0392713a4fd62b0883ebba3d650` |
| `m3/collections/interop/pom.xml` | `3fc5cc0f0bee520675fea18a379c96b288c99084` |
| `m3/collections/verification/no_external_runtime.py` | `aa7baed2aa9e4ff2aa675771def00a09ccc863f3` |
| `m3/collections/verification/test_no_external_runtime.py` | `2886c5d68444ee34e54a3352d1400a2e4fdb21a4` |

The two live receiving preimages were re-read from actual master and matched
the canonical exact inputs:
- `m3/collections/interop/pom.xml`: `5cb39eba409bc2c676f02245529d0621be8f5c2d`
- `m3/collections/interop/README.md`: `cd46111080b6c8a0e55fc7a1ff53f9463697c687`

The three add-only paths did not exist in target master at the pinned preflight.
All five original source blobs were read back from #2064 and matched exactly.
Every other original target path retains its Git object unchanged.

## Executed bounded proof

- Python3 `py_compile` passed for both exact original script blobs, verified by
  independent local `git hash-object` output.
- Original `python3 -m unittest discover -s m3/collections/verification
  -p test_no_external_runtime.py -v`: **11 run, 0 failures/errors**.
- Actual current `m3/arrays/pom.xml` and `m3/collections/pom.xml` direct
  dependency census: only JUnit test-scoped and first-party `com.m3` compile.
- Original interop postimage POM is XML and retains all 4 direct provider
  compile requirements: Guava, Eclipse Collections, Spring Core and Apache Commons.
  Its new `maven-deploy-plugin` explicit `skip=true` prevents normal Maven deploy.
  No dependency is silently reclassified to `provided` or `optional`.
- New source packet contains zero Java, native, JNI or vendor source edits.
  No M3JDK21 runtime dependency is added to either core project.

## Explicit qualification limits

This scanner validates **declared direct** Maven dependencies only. It does
not resolve the effective transitive Maven dependency graph, shaded artifacts,
classloading, service loaders, actual published artifacts, source-vendored
provider contracts, or universal zero-runtime-dependency closure.

The provider-bearing interop build remains an *optional research module*
and is **HOLD for consumer publication** until licensed per-file source inlining
and M3 array/index/mask/JNI behavioral parity are qualified.

`mvn verify`, actual deploy/plugin behavior, full modified-JDK,
jtreg/JNI/ABI, and hosted exact-head Actions were NOT executed for this
recovery and remain separate gates. Do not automatically equate recipe-test
success with product-runtime acceptance.
