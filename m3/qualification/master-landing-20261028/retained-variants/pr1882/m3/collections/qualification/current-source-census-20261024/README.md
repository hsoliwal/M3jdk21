<!-- SPDX-License-Identifier: Apache-2.0 -->
# Current M3JDK21 collection source census (non-admission)

This is **M3JDK21**, an integrated product JDK—not an M3 overlay. This packet is an original-product qualification tool, not a runtime collection API or a migration recipe. Canonical source text and replay recipe belong only to [Synexia #13224](https://github.com/hsoliwal/com.synexia/pull/13224), commit `8068da16c8cf86be7f925bbd7ba5aa46032d5626`, recipe `com.synexia.m3.Jdk21CollectionSourceCensus20261024`.

## Exact generated original product tools

| Product path | Canonical Git blob |
| --- | --- |
| `audit.py` | `99d7bc41f024ba24f388c683444a474d23a7b622` |
| `test_audit.py` | `b1fa399afc9d631360eb1fd7b9d620f7648f612c` |
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


The accepted updated test now explicitly tampers with a **duplicate** historical source row *and recomputes the manifest packet SHA-256* before requiring the original path-uniqueness guard to reject it. All five fixture tests passed again locally. The previous test image is superseded only by the exact canonical test source; other owner files and historical manifests remain unchanged.

## Exact Git HEAD and source-index successor

The original three-target census recipe remains immutable in
[Synexia #13224](https://github.com/hsoliwal/com.synexia/pull/13224).
New canonical two-target successor recipe
`com.synexia.m3.Jdk21CollectionCensusGitHeadBinding20261024`, committed
to Synexia `8cf142474ce4a3839cf2e9b6d1410bef5a67ca80`, receives only
the original product `audit.py` and `test_audit.py` changes.

| Exact original product owner | Before Git blob | Exact received postimage |
|---|---|---|
| `audit.py` | `99d7bc41f024ba24f388c683444a474d23a7b622` | `8a0debdbbc041add745b0ceb99bc6aaf3aacce47` |
| `test_audit.py` | `b1fa399afc9d631360eb1fd7b9d620f7648f612c` | `50b0cc48deca377aac81930950d14e4217058110` |

The report still returns `INVENTORY_ONLY_NOT_QUALIFIED` and
`promotion_allowed=false`. For `--expected-head`, it additionally
records `checked_head_sha` and `exact_git_java_index_count`, requires
all Java source paths to match `git ls-files` (untracked/missing Java
is refused), rejects staged/unstaged changes to tracked Java sources
or the historical manifest/packet, and checks Git HEAD again after
scanning. These are evidence integrity checks, not source-promotion
authorization.

The original five Python tests remain; the updated Python suite now
has **seven** synthetic tests, including HEAD mismatch, tracked mutation
and untracked Java refusal. They passed locally, along with strict
Python compilation. The actual GitHub current-head census workflow
and real OpenRewrite Text SDK fixed-point tests are independently
required and are **not yet qualified**.

No Java collection implementation, native source, JNI symbol, original
historical source receipt, source module ABI, or JDK release gate was
changed. Keep PR #1882 DRAFT/HOLD while full Java/JNI, installed-JDK,
jtreg/JCK and source-receipt successor admission remain unresolved.
