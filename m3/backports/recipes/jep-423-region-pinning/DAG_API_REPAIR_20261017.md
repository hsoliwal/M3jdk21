# JEP 423 — backport DAG Java 21 API restoration (2026-10-17)

**State: target-specific one-off source-bound adaptation; proof pending.**
No JEP 423 HotSpot product code changes, no result promotion, no test removal.
Synexia remains the canonical owner of reusable transformation semantics.
This is the receiver's exact current-owner Java compiler repair, not a
replacement recipe engine.

## First reproducible stopped gate

Previous real hosted materialization run:
- GitHub Actions run: 39007584120, job: 117323098849
- Step: `4b - Bootstrap source-vendored Maven DAG dependencies`
- `migration-recipes`, `synexia-jdk-a3-recipes`, and `a3` compiled.
- `backport-dag` stopped at exactly three Java errors.
- Upstream JEP 423 source lineage/donor FILE atom generation passed;
  source-built HotSpot and jtreg were not reached.

## Before → after (current target source, not Git tree replacement)

| Target | Original Git blob | Proposed Git blob | Specific correction |
| --- | --- | --- | --- |
| `m3/tooling/backport-dag/src/main/java/com/m3/tooling/dag/M3A3BackportPreparation.java` | `19548080f59680e72ba3b3c2d67d50c8d86f34c3` | `933d9b8ca4555a614a81197f92fb653cd0053133` | Disambiguate nested-record `path` helper; refuse old receipt-less API; require and forward the canonical `Verified` mastery object; reject mismatched or duplicate A3 receipts |
| `m3/tooling/backport-dag/src/main/java/com/m3/tooling/dag/M3PacketDagComposer.java` | `657bd4b2658069618559aa3c3d6ab1aacf55bd61` | `ffbbefba26df65fc2bbffea2a3b044aeb87a34f0` | Resolve dependency String IDs via `checkedPacket.require(id)` before mapping to full packet DAG atom node IDs |
| `m3/tooling/backport-dag/src/test/java/com/m3/tooling/dag/M3A3BackportPreparationTest.java` | `7a19605b396127988a7b3d5926505c30af42438d` | `07294a4eb61ed603ffb03ddd44dfccacbfab7b11` | Preserve existing mixed native/Java and refusal tests, supply fixture mastery like the current A3ApplyTest, and prove missing/null mastery refuses without any writes |

Source identity: M3JDK21 `master@970a34350ebdf6a3f1c5ff5acc8445d0f3515f55`,
followed by the exact separate Synexia Cir recovery PR #1009. This repair
does not alter the contents of a single `src/hotspot/**` or `test/hotspot/**`
file and keeps all OpenJDK GPLv2/header terms unchanged.

## Proof still required

- `mvn -B -ntp -f m3/pom.xml -pl tooling/backport-dag -am clean verify`;
- named `M3A3BackportPreparationTest` and `M3PacketDagComposerTest`,
  including negative mastery-refusal and packet dependency edges;
- source-tree readback and fixed point (no generated source mutation);
- rerun JEP 423 62-FILE workflow from the exact repaired branch;
- Matched fastdebug HotSpot image and G1/JNI/JFR jtreg;
- separate review of any broader scope promotion.

A three-argument caller no longer gets the A3 recipe implicitly: the old
signature remains for source/binary linkage but **throws before touching any
target/source**. A verified, canonical mastery receipt is mandatory at the
four-argument entry. Existing A3 tests construct synthetic verified objects
strictly as test fixtures, not as production admission receipts. The
`A3Apply.run` contract itself remains unchanged.

Do not mark `CURRENT_TREE_RECEIPT.tsv` PASS or merge from this packet alone.
