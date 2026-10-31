# T02 correction to the unexecuted V3 draft

The exact bounded T01-to-T02 delta introduces no blocker. The earlier static report and its seals remain unchanged as evidence for the unexecuted T01 draft; they are not a claim that T01 executed or that its mutable original file locations still contain T01. The two exact original files now survive under `raw-custody-v3/draft-T01/` with their originally reviewed hashes.

| Artifact | T01 SHA-256 | T02 SHA-256 |
| --- | --- | --- |
| `raw_custody_v3.py` | `a96f1aec811774467092a09cd750d47785fd4dd5482ce792ffac93c38deb9f5b` | `45fe2a39cc936f94e20cf0051f3844e6d6b5ba59b87b2d3ca175bb60917603be` |
| `verify_publication_v3.py` | `7138709c360728c17c701391f303a02e21e6e1334b29bc3f67f6c9702f7440f4` | `45d1e83ba81dfaed9434fddb7df9f4962b7fbbb8cb1c9b29f90754c8fcf38baa` |

The helper contains exactly two corrections. First, `bound()` now checks that the original supplied file path is a file and is not itself a symlink before resolving it, and then checks the resolved path remains inside the publication work directory. This corrects the old check's loss of the original path's symlink property. It is a leaf-path symlink check and resolved containment check, not an assertion that every ancestor component is nonsymlinked.

Second, the verification-commit body must now equal the JSON parsed from the retained `original_response.structuredContent.content`, and that wrapper must report `isError=false`. The helper still separately checks the exact expected commit SHA, tree and sole be92 parent. This closes the previous internal consistency gap between the supplied parsed body and its retained wrapper. It does not independently authenticate a tool call; actual captured-call provenance remains root-owned evidence.

The main verifier's only change is its expected helper SHA-256, updated to the exact T02 helper. Its two immutable invalid-UTF8 exceptions, six required direct bodies, normalized-wrapper/path/ref/size checks, strict byte/Git/SHA checks, full payload/tree/parent verification, pre-branch mode and final `PUBLISHED_CUSTODY_VERIFIED` semantics are otherwise byte-for-byte unchanged. No source qualification or receiving acceptance is added.

`CUSTODY_INPUT.json` remains `22b58a9e97fd6bb4ebebc6a89dc13e368107c10429bff59191914a5e639c81df`; `PROTOCOL.md` remains `cdf3826e3725acded05b38031b70d9e7a8f8579540b38d65a2a8e38ce64209f5`. The parent owns the complete helper algorithm and actual four-create/four-read/17-tree evidence audit. This addendum reviews only the exact corrections and updated integration pin.

Six inputs were rehashed without change during this review. The preserved draft hashes match the original static report. Source parsing and text comparison were performed without importing or executing either owner program. No remote action, owner edit, broad payload audit or historical suite review occurred.
