<!-- SPDX-License-Identifier: Apache-2.0 -->
# Current M3JDK21 collection source census (non-admission)

This is **M3JDK21**, an integrated product JDK—not an M3 overlay. This packet is an original-product qualification tool, not a runtime collection API or a migration recipe. Canonical source text and replay recipe belong only to [Synexia #13224](https://github.com/hsoliwal/com.synexia/pull/13224), commit `b8a5b2817af8711a8702d06289396293aeb2afcb`, recipe `com.synexia.m3.Jdk21CollectionSourceCensus20261024`.

## Exact generated original product tools

| Product path | Canonical Git blob |
| --- | --- |
| `audit.py` | `99d7bc41f024ba24f388c683444a474d23a7b622` |
| `test_audit.py` | `8ff6c68c71ad0942141c315bb25ccfdda6553714` |
| `.github/workflows/m3-current-source-census-20261024.yml` | `f3978d4553044d0cf180d7f3abad2fbd6201dcdc` |

No runtime collection implementation, native library, JNI export, `java.util` API, dependency or existing source-manifest/provenance file is changed in this receiving pass.

## Evidence boundary

The historical `installed-source-enum-set-20261014.tsv` manifest contains **99 rows**, of which 96 name Java files; 94 are under `m3/collections/src` and two are separate verification probes. On the reviewed product head, the current `m3/collections/src` includes **291 production Java files plus 374 test Java files**, a **665-source** present-day inventory. Independent successor source receipts may qualify additional owners, so absence from one old manifest **does not prove code loss or unqualified delivery**.

`audit.py` preserves the historical manifest and its SHA-256 and reports exact current SHA-256 facts: historical matches, mismatches, missing paths and paths absent from the historical list. It rejects duplicate JSON keys, malformed manifests, symlinked Java files and requested HEAD mismatches. The output unconditionally states `INVENTORY_ONLY_NOT_QUALIFIED` and `promotion_allowed=false`. It never changes Java sources, native libraries, historical manifests, mapping entries or supersession authority.

## Execution

On a Java 21 (or other Python-capable) source checkout:

```sh
python3 -m unittest discover -s m3/collections/qualification/current-source-census-20261024 -p 'test_audit.py' -v
python3 m3/collections/qualification/current-source-census-20261024/audit.py . --expected-head "$(git rev-parse HEAD)" --output m3/collections/target/current-source-census.json
```

The exact-head GitHub Actions workflow does this with explicit PR-head checking, then uploads the derived report as an artifact. A successful *inventory job* is not success for full Java/JNI release admission. The original `verify_donation.py`, full JNI/Maven recipe, vendor build, installed-JDK image, jtreg/JCK and performance gates remain independent.

**Qualification truth:** Five synthetic Python contract/refusal cases, Python syntax, and YAML structure passed locally; three received Git blobs were compared byte-for-byte against Synexia. **Pending:** actual full-current-source census execution on the installed repo head, real OpenRewrite Text SDK, full module/native/JNI tests, and JDK image proof. Keep draft and do not merge based on inventory alone.
