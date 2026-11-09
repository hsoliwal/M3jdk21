<!-- SPDX-License-Identifier: Apache-2.0 -->
# p2-057-20261107

census2 P2-057 (crate `m3-long-pathmask-bridge-20261018`, com.synexia `d5ad5090d87e`) became applicable when #2649
restored `M3IndexPathMasks.selectLongJava` (merge-loss case 05). #2649 recorded it as HOLD_CI_PIN because
`.github/workflows/m3-long-index-path-jni-20261018.yml` pins `M3LongIndexPathMasks.java` and compiles it alone. This
change does both in one PR:

* **P1** `M3LongIndexPathMasks.select` keeps its own admission (`validateShape`, `validateVisitedOffsets`) and
  diagnostic and delegates the two-pass kernel to `M3IndexPathMasks.selectLongJava`. The postimage is the crate
  candidate (package-normalised) plus one `//FLLDCIM` anchor line.
* **P2** the workflow's identity pin moves from `278f63e3bdc6` to P1's blob `58c87920ae0f`, and
  `M3IndexPathMasks.java` joins its paths filter and its strict `javac` line.

| file | content |
|---|---|
| `DECISION.tsv` | per leaf: decision, citations, evidence |
| `apply.py` | recipe P2_057_LONG_PATHMASK_BRIDGE v1: hash-pinned anchored edits, `--leaf`, `--check`, refuses drift and a pin that is not P1's postimage blob |
| `EDITS.tsv` | file, before/after sha256, leaf, decision, citations, rule (written by apply.py) |
| `oracle/M3LongPathBridgeBehaviorProbe.java` | the crate's behaviour probe (package-normalised), 1259 checks |
| `tools/workflow-local.py` | runs a workflow job's `run:` steps locally as GitHub's bash shell does |
| `tools/p1-gates.sh` | -Werror javac, self-test, behaviour tape on master and candidate (default, -Xint), one mutant |
| `GATES.tsv` | gates with numbers |

Replay: `python -I apply.py --check`; `TMPDIR=<tmp> JAVA_HOME=<jdk21> bash tools/p1-gates.sh <out> <master m3/collections>
<candidate m3/collections>`; `python3 tools/workflow-local.py <workflow.yml> <checkout> <runner-temp>` with JDK 21 and
Maven on PATH.
