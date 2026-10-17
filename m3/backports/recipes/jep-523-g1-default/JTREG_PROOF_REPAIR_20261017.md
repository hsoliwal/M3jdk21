# JEP 523 G1 jtreg acceptance — harness repair, 2026-10-17

Upstream code and original Java21 regression tests remain intact.

## Evidence from actual hosted Java21 JDK images

GitHub workflow `39007914024`, job `117324056780`:
- exact OpenJDK/JEP 523 donor path proof passed;
- source-built Synexia/Maven/OpenRewrite `M3Jep523BackportRecipeTest`
  passed, including fixed point;
- actual G1-enabled OpenJDK **release JDK image built successfully**;
- the focused jtreg step failed **before executing a GC test**:
  `Error: jtreg framework is not found. Please run configure using --with-jtreg.`
- parallel no-G1 image/job `117324056888` passed the existing Java21
  Serial/Parallel fallback assertions.

This is not an actual G1 semantic-test failure or evidence of success;
the remaining G1 jtreg and runtime-smoke gates were not exercised.

## Existing tooling donor

Reuse unchanged install/configure mechanism already in this tree:
`.github/workflows/m3-jep458-current-master.yml` (JEP 458 jtreg7 proof).

G1 job only:
- extend the Ubuntu-22.04 OpenJDK build-prerequisites apt package list by
  `jtreg7`;
- assert `/usr/share/jtreg/lib/jtreg.jar` exists before configuring;
- pass `--with-jtreg=/usr/share/jtreg` to the same release-image configure.

No other flags, GC defaults, tests or runtime code change. The independent
no-G1 proof job remains byte-for-byte unchanged.

## Still required

Re-run recipe, G1 and no-G1 build/GC tests on the exact new proof branch.
Confirm `TestSelectDefaultGC`, `TestDisableDefaultGC`, startup GC
jtreg and runtime smoke; retain `NOT_AUTHORIZED` until all tests
actually execute and pass. This is a JEP-specific workflow repair,
not a broader JDK 27 backport completion.
