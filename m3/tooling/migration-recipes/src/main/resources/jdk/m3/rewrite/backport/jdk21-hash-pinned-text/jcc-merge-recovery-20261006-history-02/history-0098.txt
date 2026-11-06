# T02 correction to the unexecuted V3 draft

The exact bounded T01-to-T02 delta introduces no blocker. The earlier static report and its seals remain unchanged as evidence for the unexecuted T01 draft; they are not a claim that T01 executed or that its mutable original file locations still contain T01. The two exact original files now survive under `raw-custody-v3/draft-T01/` with their originally reviewed hashes.

| Artifact | T01 SHA-256 | T02 SHA-256 |
| --- | --- | --- |
| `raw_custody_v3.py` | `80f45f4a1d2e0cd49199e6885c62beff39a326aef5739e8890da5e9d991e2412` | `203f44729f860620949ec9e6aa7c5f2c7d766de2e406548ceb6797797bfdcc33` |
| `verify_publication_v3.py` | `4cc60517ad06d1c3590059e43f353c59ad751cb74601bc65735a7df9259edcbf` | `904a2105f04a12454a2d97c13a5bef6764e18ae28a2fe680e06ca0e6e14a6e93` |

The helper contains exactly two corrections. First, `bound()` now checks that the original supplied file path is a file and is not itself a symlink before resolving it, and then checks the resolved path remains inside the publication work directory. This corrects the old check's loss of the original path's symlink property. It is a leaf-path symlink check and resolved containment check, not an assertion that every ancestor component is nonsymlinked.

Second, the verification-commit body must now equal the JSON parsed from the retained `original_response.structuredContent.content`, and that wrapper must report `isError=false`. The helper still separately checks the exact expected commit SHA, tree and sole be92 parent. This closes the previous internal consistency gap between the supplied parsed body and its retained wrapper. It does not independently authenticate a tool call; actual captured-call provenance remains root-owned evidence.

The main verifier's only change is its expected helper SHA-256, updated to the exact T02 helper. Its two immutable invalid-UTF8 exceptions, six required direct bodies, normalized-wrapper/path/ref/size checks, strict byte/Git/SHA checks, full payload/tree/parent verification, pre-branch mode and final `PUBLISHED_CUSTODY_VERIFIED` semantics are otherwise byte-for-byte unchanged. No source qualification or receiving acceptance is added.

`CUSTODY_INPUT.json` remains `384ce843f2e4545ad1c40d7ee58abbc471b5a6d1cbed89bea82dcff0479ce6a5`; `PROTOCOL.md` remains `cdf3826e3725acded05b38031b70d9e7a8f8579540b38d65a2a8e38ce64209f5`. The parent owns the complete helper algorithm and actual four-create/four-read/17-tree evidence audit. This addendum reviews only the exact corrections and updated integration pin.

Six inputs were rehashed without change during this review. The preserved draft hashes match the original static report. Source parsing and text comparison were performed without importing or executing either owner program. No remote action, owner edit, broad payload audit or historical suite review occurred.
