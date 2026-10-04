# PR82 exact-branch proof routing

The previous PR-triggered run had no job and therefore no source/test verdict.
This pass adds the existing PR82 branch to the existing workflow push allowlist.
All original steps, toolchain hashes, permissions and clean verify commands are
byte-identical. Production source, database tests, POMs and coverage thresholds
are untouched. The old receipt remains historical evidence, not a new pass claim.

The existing text snapshot recipe owns this exact one-file change. Three JUnit
tests cover replay/fixed point, preserved job body and missing/drift refusal.
Local validation checked the source diff, YAML structure, exact byte reversal,
and resource hashes. Local Maven/JUnit/OpenRewrite remain unexecuted.

The new pushed head must supply its own runner logs and reports. Test success
does not equal99% coverage, and no whole-JDK or database admission is inferred.
Keep the draft PR and record the actual run outcome separately after execution.
