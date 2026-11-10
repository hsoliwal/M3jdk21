# A3 atom/pattern spectrum qualification

## Source and scope

This task extends the existing M3JDK21 atom/pattern recipe laboratory. The publication baseline is
`hsoliwal/M3jdk21` master `da958d00d24154c0db87beca0ec80a7df2b43b73`, Git tree
`548f3d7c434c49c3989fc722f34ebba2ba9f0c5a`. The initial qualification and immutable real-source
corpus use `e8fdab9a7abc4b0273438ce9e5d0ffd6255b1247`, Git tree
`7d03f7bd4aab860c74cb436c082eca50d87c8391`; the corpus files remain byte-identical at publication.
The existing owners are the seven
`com.m3.rewrite.atom` classes and the package-private `MemJava` test compiler. Reuse those owners;
do not introduce another transformation engine or modify OpenJDK product sources.

The authoring namespace is `m3/tooling/migration-recipes`. This task changes test/recipe resources,
focused proof wiring and task evidence only unless an executed regression identifies a production
recipe defect. Any such repair requires a reviewed exact-source OpenRewrite snapshot crate.
Root files, product APIs, JNI ABI, dependency versions, the default reactor, and the existing
0.99 line/branch coverage thresholds stay locked. Preserve unrelated upstream production fixes;
do not overwrite their owners with older source images.

## Qualification lanes

The generated Java21 lane evaluates the actual atomize, patternize and document recipes in all six
orders. A frozen original program and an independently written arithmetic model remain the oracle
for every intermediate candidate. Inputs enumerate boundary values and admitted operators without
random sharding. A bounded pass count must observe a complete zero-change sweep; merely exhausting
the pass budget cannot pass. Compilation, effects/exceptions, declared class/member signatures,
opaque comments/literals/text blocks and fresh idempotent replay are separate checks. Deliberately
wrong code and source/manifest drift must be rejected.

The repository corpus lane pins at least 100 real Java source files from this exact M3JDK21 commit,
including simple APIs, collections, regex, compiler and existing M3 tooling. It records source hashes,
size, parser/type admission, and candidate/fixed-point outcomes. Rejected parsing or incomplete type
attribution is explicitly retained as a coverage gap. It does not count as successful transformation,
semantic preservation, a JDK build, jtreg, native proof, or universal correctness. The observational
corpus does not authorize modifying those files.

## Verification contract

Use an isolated Java21/Maven cache under the task workspace. Preserve logs in execution order:
patch/diff -> static/lint -> compile -> JUnit tests -> runtime artifact audit. Each changed source
invalidates downstream receipts. Re-run only from the last valid stage. Report the focused result
separately from the full default migration-recipes gate. Historical parser and coverage issues
remain the owning gate's responsibility. No skip flags, exclusions, or changed thresholds may turn that
unexecuted or failing default gate into a pass.

All published additions carry preimage `ABSENT` and SHA-256 postimages. Evidence records the exact
recipe-source hashes, input corpus hashes, counts and stage statuses. The parent session publishes
the candidate branch and draft PR without rebasing or merging master.

## First retained lint failure

The initial exact-input static check passed. Checkstyle 12.3.1 then found one unused
`java.io.IOException` import in the retained `M3AtomizePureIntReturnRecipeTest`. No compilation or
tests ran after that failure. This task removes that unused import from the existing regression
fixture and retains its before/after hashes; no test or assertion is removed. Restart at static/lint.

## Executed semantic-memory loss

After static checking, Checkstyle and Java21 production/test compilation passed, the new inline
comment regression executed the real atomizer and failed: `ENTRY_CONTRACT` occurred zero times
after atomization instead of once. `replaceBody` discarded documentation on the original return
and its closing block. The failure is retained in the task evidence; subsequent tests did not run.

Repair the existing `M3AtomizePureIntReturnRecipe` with the existing
`M3HashPinnedJavaSnapshotRecipe` and an `atom-comment-custody` resource crate. Preserve the
original block prefix/end, prepend the original return comments to the generated atom comments,
and retain the original expression tree as the initializer. No expression evaluation, eligibility,
scope, method signature, API, runtime dependency or JNI behavior changes. The exact-hash recipe
must emit the reviewed source, refuse drift, and replay without change before its result is copied
to the approved source path. Then restart the target's lint/compile/test sequence.

The first meta-recipe run materialized the exact expected candidate. Its negative fixture incorrectly
expected an error callback; the existing snapshot recipe correctly throws `IllegalStateException`
directly during generation on source drift. Correct the fixture to assert that exact exception and
message, preserve the failed log, and repeat bootstrap verification. No candidate is applied until
the entire meta-recipe fixture passes.

## Measured bounded-heap frontier

The complete focused run executed 25 JUnit cases: 24 passed; the 324-expression, six-order case
failed with Java heap exhaustion in context-sensitive JavaTemplate parsing at the fixed 768 MiB
heap bound. Preserve that failure. The template only declares an int local, accepts a typed int
placeholder, and returns that local; its admitted expression tree is retained separately. It does
not need surrounding source/import context. Qualify the context-free template through a second
exact-source recipe transition and retain the same memory bound and the complete test denominator.

The next ambiguity regression distinguishes the exact canonical M3-IOP marker from a longer,
unadmitted role containing the same substring. Existing freeform comments must be preserved;
substring resemblance must not suppress emission of the canonical role marker.

The ambiguity regression executed and failed on `PURE_INT_EXPRESSION_NOT_ADMITTED`: substring
matching suppressed the real canonical marker. Repair the existing patternizer to compare the
complete comment role after surrounding Unicode whitespace is stripped. Preserve the decoy comment.

The final `atom-pattern-spectrum` crate replays both complete reviewed repairs from the immutable
repository baseline. The earlier comment-custody crate and its logs remain an intermediate record;
final qualification uses the complete two-owner candidate and proves a fresh fixed point. The
working authoring mirror may contain the previously tested intermediate comment fix; replacing it
with the final recipe-emitted image is explicitly recorded with its intermediate preimage hash.

The real-corpus refusal report identifies missing source relationships in the M3 sidecar files.
Test the already-pinned 25-file M3 group together as one in-memory source closure, retaining all
type checks. Compile that actual source group and exercise its existing PrefixZ algorithm on a
bounded exhaustive code-unit corpus. This is an M3 sidecar proof, not an OpenJDK or JNI build.

## Independent comment-custody review

The first complete 28-test candidate passed with 45 intermediate compiler/runtime/surface checks,
and its runtime receipts were audited. Independent source review identified another comment owner:
the original return statement's right padding, printed before its semicolon. Extend the existing
comment regression with a `TAIL_CONTRACT` comment at that location before publication. If it fails,
retain that failure and transfer the original statement padding to the extracted atom through the
same exact-source crate, then restart bootstrap and target gates. The prior green run only certifies
its earlier comment denominator.

The trailing-comment regression failed after its ordered static/lint/compile prerequisites:
`TAIL_CONTRACT` occurred zero times rather than once. Extend the resource candidate to preserve
the original return's right-padding space on the new atom statement. The final fixture covers
body-prefix, statement-prefix, expression-prefix, both binary operands, statement-tail and block-end
comments, including block and line comments before the original semicolon. The final runtime audit
also binds the actual production owners directly to the final resource manifest's output hashes.

## Current-master integration before publication

Publication preflight found that master advanced after merged PR124-127. All 41 intended write
paths still match their earlier preimages or remain absent; none of the 100 pinned corpus paths
occurs among the 23 changed repository files. The 12 other imported original owners are unchanged.
The shared `M3HashPinnedJavaSnapshotRecipe` gained an explicit PlainText opt-in. Its default and
one-argument constructor retain Java LST mode, which is the mode used by this task's existing fixtures.

Preserve the incoming helper byte-for-byte at Git blob
`717c3e01f8f0407ce96439e3ecc96190b10c1eed`, SHA-256
`9cf77c6e68a534e84b88230ca517fa6c5a76fb3e101ea3a1f68aa5222c3ec4d9`.
This is an upstream baseline integration, not an authored recipe change. Keep the earlier baseline
and all completed receipts; record the current helper in `SOURCE_PINS.tsv` and repeat the entire
focused ordered lane under `final-current`. The corpus remains pinned to its original commit.
Publication depends on the current-master run, not the earlier green result.

The complete upstream comparison also adds `M3AtomCoverageClosureTest` and
`M3AtomDefensiveBranchTest` inside the focused POM's existing wildcard-selected atom package.
Fetch their exact current Git blobs and include them in the current baseline and execution closure.
They add eleven upstream tests; keep every earlier fixture and expand the runtime receipt's exact
suite set from 28 to 39 tests. Missing newly selected sources must not be hidden by the older local
mirror. These files are initially integrated unchanged and remain subject to the same lint gate.

The `final-current` static pin check passed. Its actual Checkstyle gate then failed on an unused
`assertEquals` import in the newly upstream `M3AtomDefensiveBranchTest`. No compilation or tests
ran after that failure. Remove that import alone; preserve every test and assertion. This adds a
fourth modified existing file to the publication scope and leaves the upstream helper unchanged.
Record the original test preimage, restart all gates as `current-2`, and derive the final JUnit
denominator from the executed reports rather than excluding either upstream test file.

The `current-2` prerequisites passed and all 39 cases executed: 38 passed, while the newly upstream
coverage fixture errored when a direct detached-method visit tried to insert a data-table row using
a bare execution context. OpenRewrite requires a scheduled recipe cycle for that insertion. Preserve
the observed failure and repair only the fixture: a small wrapper runs through public `Recipe.run`
and explicitly delegates to the same detached methods using its runner-supplied context. Preserve
the detached condition, accessor assertions and registered inventory recipe; do not fabricate cycle
metadata or change production behavior. This adds the fifth modified existing test path. Restart
all gates as `current-3` and retain every test.

The `current-3` lane passed static pins, actual Checkstyle, strict compilation, the targeted detached
fixture, all 39 JUnit cases and the final runtime audit at 2026-10-05 08:02:44 UTC. Publication uses
this current-base result. The two atom/pattern production output hashes remain unchanged; the
latest shared helper is preserved byte-for-byte and excluded from the authored patch.
