<!-- SPDX-License-Identifier: Apache-2.0 -->
# Qualification of the sixteen donor payloads

The qualification records described below live in the canonical Synexia recipe
crate at `synexia-openrewrite-recipes/recipes/m3-cpu-tools-jmod-20261018/qualification`.
These records qualify the payload inputs listed in `DONOR_CHANGES.tsv`.
The receiving build must repeat its own JMOD and linked-image gates after actual
recipe materialization. Payload-module execution alone is not a JMOD shipping
or byte-reproducibility claim.

## Observed results

| Gate | Observed result | Exact record |
| --- | --- | --- |
| All sixteen JPMS modules compiled independently with `javac --release 21 -Xlint:all,-module -Werror` | PASS; no diagnostics in the corrected isolated harness | `donor-module-compile-v2.json` |
| All sixteen retained `main --self-test` entry points | PASS | `donor-module-compile-v2.json` |
| Independent fixture on stock Temurin 21.0.12.1+1, `-Xmx128m` | PASS; 105,059 assertions | `donor-fixtures-v2.log` |
| Same fixture on actual existing M3JDK21 fastdebug image | PASS; 105,059 assertions | `donor-fixtures-m3-runtime.log` |
| Same fixture on the same M3JDK21 image in interpreter mode, `-Xint` | PASS; 105,059 assertions | `donor-fixtures-m3-interpreter.log` |
| `javap -public -s` parity after receiving-name normalization | PASS for all 21 public types in sixteen modules | `donor-public-api.json` |
| Exact raw donor `TopologicalOrder.java` compilation | Expected refusal due to its array cursor declaration | `donor-topology-original-compile.log` |

The stock runtime identified itself as `Temurin-21.0.12.1+1`, build
`21.0.12.1+1-LTS`. The actual M3 runtime identified itself as
`21-internal-adhoc.root.M3jdk21-runtime`, fastdebug. The latter was an already
built M3JDK image; it executed the compiled CPU modules on its module path.
This records actual M3 runtime execution without claiming a fresh full JDK
rebuild or a separate proof of every String storage operation.

The fixture is checked in as `fixtures/M3DonorToolsTest.java` and uses a fixed
seed of 20261018. Its successful output is:

```text
M3_DONOR_TOOLS_PASS assertions=105059 tools=16 graph_exhaustive=531 graph_random=600 priority_operations=30000
```

## Independent oracles and failure cases

- Exhaustive topology admission covers all 531 directed graphs with zero
  through three vertices, including self-loops. Another 600 randomized graph
  cases check ordering and weighted distances; reversed edge ordering checks
  deterministic output. A 4,096-vertex chain checks iterative traversal.
- The shortest-path oracle is BigInteger Bellman-Ford, including weights near
  `Long.MAX_VALUE`, exact-sentinel paths, unreachable vertices, and overflowing
  sums. It does not reuse the candidate's CSR or heap implementation.
- Thirty thousand randomized priority-queue operations compare object identity,
  score ordering, and stable tie behavior with a JDK `PriorityQueue` oracle.
  Signed zero, equal-score ties, and extreme finite scores are included.
- Five hundred randomized UTF-16 text pairs include NUL, CR/LF, combining
  marks, valid surrogate pairs, and isolated surrogates. Exact search is checked
  by a `startsWith` scan; edit distance by a full-matrix oracle; replacement
  spans by target reconstruction; n-grams and frequencies by independent maps.
- Normalization, word breaks, and regex offsets/groups are checked against the
  corresponding linked JDK contracts, including empty and zero-width regex
  matches and zero result caps.
- Four hundred randomized postings corpora compare an independent map/set
  oracle and verify document deduplication, sorted postings, defensive results,
  immutable terms, and independence from later source-container mutation.
- Ten thousand random varints plus eight boundary values compare BigInteger
  LEB128 framing and round trips. Empty, null, overlong, trailing, and
  unterminated encodings are rejected.
- One thousand interval cases compare half-open overlap with an integer-point
  oracle. Two hundred statistics corpora compare with a two-pass reference and
  check that refused nonfinite samples do not change state.
- SHA-256 checks include standard empty and `abc` vectors and exact UTF-8 bytes
  for a supplementary character. Nulls, negative bounds, invalid vertices,
  negative weights, and reversed spans have explicit refusal cases. Shape
  checks inspect public reflection metadata for prohibited
  retained map/collection owner fields; they do not access private values.

The fixture exhausts only its stated small topology domain. Randomized checks
provide broader evidence, not an exhaustive proof for all Java inputs. No
latency, allocation rate, retained-heap, native/JNI, or speedup claim is made by
these results.

## API baseline and preserved refusal evidence

Fifteen exact fetched donor implementations compiled without editing. The raw
sixteenth donor, `TopologicalOrder`, has a concrete compilation defect. For its
public-API comparison only, a separately labeled declaration witness changes
that array cursor declaration to a scalar cursor. The API report calls this
`DECLARATION_REPAIRED_API_WITNESS_RAW_DONOR_REFUSED`; it never reports the raw
donor as a passing baseline. The witness source remains under
`qualification/topology-declaration-witness`.

The initial `donor-module-compile.json` records a harness failure: broad source
directory enumeration included the concurrently added aggregate CPU module,
and shared module-path scans could observe a sibling halfway through
compilation. The corrected v2 harness takes its fixed sixteen-module inventory
from `DONOR_MODULES.tsv` and gives each self-test an isolated completed module
path. The initial refusal evidence remains present. No source acceptance gate
was relaxed to obtain the v2 result.
