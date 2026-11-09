# JDK proof continuation — 2026-10-04

Base: 32af0afc4029b4b20378a964e4b2c3c033ca4477.

Finish execution of existing JDK work rather than add another parser, atomizer or function framework. The local shell has Java21 but no Maven or external DNS. Existing connected CI has executed real JUnit on some heads; it must be inspected rather than assuming all red cards are infrastructure failures.

## Work order

1. Execute the existing module-pack Maven test lifecycle, then its unchanged verify/coverage gate on the connected runner.
2. Preserve source, toolchain identity, the actual Maven runtime and the public artifacts resolved in an isolated local repository for bounded offline replay.
3. Inspect failures and repair existing owners/recipes. Reconcile the saved class-version repair with current source before publication.
4. Continue JDK normalization, backport and module-pack work from the existing owners. Product acceptance still requires native configure/make/jtreg/runtime gates.

The proof kit is not a release or a security-reviewed dependency distribution. It excludes user/runner settings, credentials, arbitrary home directories and generated application/private-key output. The installed Maven runtime is saved with its notices and an empty settings file; resolved artifacts preserve their original ownership. Sources and archive outputs carry checksums. Artifacts expire after three days.

A failed test or unchanged 99% coverage gate remains a failed job even when artifacts are preserved. No test/coverage exclusion, canonical write, merge, rebase, force push, Java API/JNI ABI change or unconditional donor absorption is authorized. OpenJDK retains its original build. The standalone Maven command is proof orchestration only.

## Recipe

`com.m3.rewrite.backport.Kit` reuses the existing `M3Jdk21HashPinnedTextSnapshotRecipe` for one absent-before workflow. `KitTest` must verify exact output, occupied-target refusal, named activation and fixed point through the actual engine. Sealing workflow bytes is not execution proof.

The entire JDK/JEP programme is not complete until its exact inventory, compatibility/dependency decisions, normalized source candidates, recipe tests, native build/jtreg/runtime and fixed-point evidence pass. Preserve incomplete rows in the existing action queues.
