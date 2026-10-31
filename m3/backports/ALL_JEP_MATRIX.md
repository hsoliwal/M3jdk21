# Complete released-JEP source and acceptance matrix — 2026-10-17

**Scope**: all 85 *released* feature JEPs in JDK 22–27, mapped to the
experimental Java 21 M3JDK21 runtime. All original JDK 21 features remain
baseline product behavior; they are not "backported" a second time.

The upstream denominator, its release authority, full 14,948 post-21 commit
history, and existing per-JEP Maven/OpenRewrite/HotSpot programs are already
owned by \`m3/backports\`. This packet does **not** replace or copy any of
those authorities or authorize wholesale JDK source transplant.

## Executed source/proof join

Run from the M3JDK21 repository root:

\`\`\`sh
python3 m3/backports/all_jep_matrix.py --root . \
  --out m3/backports/target/all-jep-matrix.tsv \
  --json-out m3/backports/target/all-jep-matrix.json

python3 -m unittest discover -s m3/backports -p 'test_all_jep_matrix.py' -v
mvn -B -ntp -f m3/backports/pom.xml verify
\`\`\`

\`all_jep_matrix.py\` is a **read-only joined projection**, reusing the
existing owners \`release_jep_authority.py\`, \`jep_source_witness.py\`
(existing draft M3JDK21 #1007), and \`jep_residue.py\`. It has no source
mutation, repository migration, GitHub network access, build-bypass, parser,
scheduler or competing recipe engine.

It requires exact reconciliation of every JDK22–27 official
\`RELEASE_JEP_AUTHORITY.tsv\` JEP to a unique sorted
\`JEP_CATALOGUE.tsv\` row, a unique source witness and a unique pending
residue when the JEP is undecided. It rejects duplicate/missing rows,
cross-release witness collisions, unknown policy entries and missing
supersession targets.

Each JEP receives:
- release, title, domain, **problem-family** category;
- current file/recipe packet and SHA-256 source witness state;
- original compatibility priority or \`UNREVIEWED\`;
- explicit **implementation gate** and next action;
- domain-specific required Maven, ABI, HotSpot, GC, JNI/JVMTI, JFR,
  crypto, AOT/CDS, protocol, compiler, jtreg and second-pass proofs.

All 85 output rows remain \`NOT_AUTHORIZED_BY_MATRIX\`. A source file
hash match cannot prove complete JEP source coverage, successful Java21
compilation, jtreg, runtime/JNI parity or performance; it must never
automatically authorize import or report a JEP complete. Rejected language,
preview/compatibility held and superseded JEPs remain separately stated.

The CSV/TSV is deterministic and preserves physical tab/newline separators.
The JSON summary contains a digest of the exact TSV bytes and counts by
release, current witness state and proof gate. Outputs are confined to
\`m3/backports/target\` using the **existing** source-witness output
fence: product files, aliases and hard-linked outputs are refused.

This is the all-JEP **execution admission layer**, not 85 completed
backports. Existing recipe/file passes must still materialize each eligible
donor's Java/C/C++/JNI atoms from immutable OpenJDK commits, then qualify
the real JDK21 source, build, jtreg, runtime and fixed point.

## Donors and algorithm review

The OpenJDK donor is the pinned upstream JEP/JBS source under its original
per-file license (usually GPLv2 with applicable Classpath Exception when
designated). Never relicense OpenJDK product files as Apache-2.0.
The already-present \`ALGORITHM_CATALOGUE.tsv\` organizes algorithm
shapes and comparable LeetCode/HackerRank/GeeksforGeeks references; challenge
sites provide taxonomy and test cases **only**, not source-copy rights.

The new \`problem_family\` routes every JEP into a coarse category
(language, JDK tools, GC/object layout, JIT, native interop, library,
crypto, JFR, networking, AOT/CDS, concurrency or compatibility) for
further per-file algorithm catalogue review. Coarse classification is not
an algorithm selection or proof of superiority.

## Phase and acceptance law

- **Canonical Synexia recipe**: \`com.synexia.m3.All85JepSourceProofMatrix\`
  using the existing Maven/OpenRewrite \`M3HashPinnedTextSnapshotRecipe\`,
  not a hand-edited M3 target.
- **Target receiver**: \`hsoliwal/M3jdk21\` branch stacked on source
  witness draft PR #1007; product source untouched.
- **Acceptance**: exact hashes, OpenRewrite SDK candidate replay/fixed
  point and refusal JUnit, target Python unit suite, Maven verification,
  independent Java21 compile/test/HotSpot/JNI correctness per qualifying
  JEP. Never infer acceptance from inventory alone.
- **Priority**: original M3 String-first receipt and Java21 grammar/API
  baseline remain in force; high-risk JEPs are not skipped, but receive
  full dependency/contract evidence before merge.
