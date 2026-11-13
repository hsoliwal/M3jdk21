# M3JDK21 ordered-convergence receiver (Java 21)

**State:** executable, read-only receiving gate. No JDK runtime mutation or JNI/jtreg
qualification is implied. Canonical Synexia source recipe:
https://github.com/hsoliwal/com.synexia/pull/11983, using its existing
Inline → Atomize → Patternize → Comment engine from Synexia PR #11952.
Policy companion: Synexia PR #11979.

## Nine stages, in order

| Stage | Authority and evidence |
| --- | --- |
| 1 Inventory | Source preimage and target preimage exact SHA-256, bound to Git blobs |
| 2 Dated additive convergence | Source JGit ancestry/history/API gate, target exact HEAD and baseline ancestor and deletion check |
| 3 Catalogue by problem category | Canonical category-root hash, taxonomy only and no source-copy authority |
| 4 Inline | Reuse Synexia mechanical source recipe |
| 5 Atomize | Reuse Synexia file-level atomizer |
| 6 Patternize | Reuse Synexia category/semantic patterns |
| 7 Comment | Reuse Synexia idempotent semantic memory |
| 8 Compile/test gates | Target compiler, jtreg, JNI, API/ABI, GC/JIT/CDS/runtime remain PENDING |
| 9 PR | No promotion without reviewed target proof; always PENDING at receiver stage |

The implementation is m3/compatibility/M3OrderedConvergenceReceiver.java.
It consumes Synexia's M3_ORDERED_CONVERGENCE_HANDOFF_V1 TSV, requiring a
separately provided handoff SHA-256 and exact donor Git revision.
It validates the original donor Git blob and source bytes, the exact candidate
SHA-256, the nine source stage rows, M3JDK21's target HEAD/working-file
preimage and history. The result is evidence only, not copied or adapted runtime
code.

## Build and test on M3JDK21

~~~sh
mkdir -p build/m3-ordered-receiver
javac --release 21 -Xlint:all -Werror -d build/m3-ordered-receiver \
  m3/compatibility/M3OrderedConvergenceReceiver.java \
  m3/compatibility/M3OrderedConvergenceReceiverTest.java
java -cp build/m3-ordered-receiver M3OrderedConvergenceReceiverTest
~~~

The isolated tests use disposable source and target Git repositories and
adversarial tampering cases; they do not claim to run real Synexia/OpenJDK
production tests.

## Produce the reviewed Synexia handoff

With a clean checkout of the exact Synexia source and complete relevant refs,
build/qualify the Synexia Maven plugin first, then run its
m3-ordered-convergence goal for one Java source file:

~~~sh
mvn -B -ntp \
  -Dm3.ordered.path=<source-relative-java-file> \
  -Dm3.ordered.base=refs/remotes/origin/develop \
  -Dm3.ordered.head=HEAD \
  -Dm3.ordered.refs=<required-donor-refs> \
  -Dm3.ordered.maxCommits=256 \
  -Dm3.ordered.sourceLicensePolicy=SYNEXIA_FIRST_PARTY_APACHE2_V1 \
  com.synexia.maven:ai-maven-plugin:m3-ordered-convergence
~~~

The license flag requires *reviewed first-party Apache-2.0 ownership*. It
must not be used for unverified third-party, GPL-derived, OpenJDK or
unclearly licensed source. Without that explicit classification the source
packet says UNREVIEWED and the JDK receiver rejects it. A recipe's license
does not relicense its generated postimage or the target OpenJDK file.

The source goal exports a JSON full report, nine-stage
M3_ORDERED_CONVERGENCE_HANDOFF.tsv, candidate source beneath candidate/,
and a review-gate note under target/m3-scale/ordered-convergence/. A held
source cannot emit an admitted handoff.

## Receive one exact target file

The target Java entry point takes ten arguments, in this order:

~~~text
M3OrderedConvergenceReceiver
  1 absolute Synexia repository checkout
  2 absolute M3JDK21 repository checkout
  3 full path to M3_ORDERED_CONVERGENCE_HANDOFF.tsv
  4 SHA-256 of the entire handoff TSV (reviewed independently)
  5 full SHA-1 Synexia HEAD commit
  6 path to the exact Synexia candidate file
  7 target-relative existing Java/C/C++/JNI/HotSpot source path
  8 SHA-256 of unchanged target file preimage
  9 full SHA-1 target baseline-ancestor commit
 10 NEW receipt directory outside the M3JDK21 source tree
~~~

The program refuses missing/reordered stages, false PASS claims for
build/PR, mismatched metadata roots, unsafe paths, symlinks, dirty Git files,
source revision drift, source/candidate SHA drift, target preimage drift,
a non-ancestor baseline, observed target file deletion, and overwriting a
prior output directory.

The new output consists of M3JDK_ORDERED_RECEIVING.tsv and
PR_PROMOTION_HOLD.md; both explicitly mark build, jtreg, JNI/native parity,
JIT/GC/CDS, runtime, API/ABI, license review and PR gates PENDING. There
is no source write, merge, rebase, checkout, ref update or push.

For broad historical retention use the existing m3/history audit; for
real source substitution and fixed-point proof use the existing
m3/tooling/migration-recipes Synexia consumer, M3 A3 and
target-owned JNI/HotSpot/JDK suites. The per-file receiver does not
replace these authorities.

**Completion boundary:** installing this mechanism on M3JDK21 does not
mean every JDK file was reviewed or that the Java runtime was optimized.
Each file, capability and target PR still needs exact acceptance evidence.
