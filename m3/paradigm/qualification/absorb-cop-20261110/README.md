# absorb-cop-20261110

The third jdk.m3.paradigm batch moves synexia-cop (packages com.synexia.context.* and com.synexia.cop.*) into
`jdk.internal.m3.paradigm.context`. The package is internal and not exported. The batch lands 212 main sources and
12 tests. It builds on the IOP landing (absorb-iop-20261110).

## Naming (RENAMES.tsv)

User decision 2026-11-10, "Strip prefix, keep 7": a class named `Context<X>` lands as `context.<X>`, because the
package already says context. This gives 90 renames.

The prefix stays on 8 classes whose bare name would collide:
- with a java.* simple name: ContextArena, ContextException, ContextGuard, ContextProgressMonitor, ContextSelector,
  ContextVector;
- with another COP class: ContextImmutablePool (ImmutablePool) and ContextSkillInventory (SkillInventory, in
  cop.cognition).

ContextSkillInventory is one more than the 7 named in the decision. It falls under the same rule (the bare name is
another COP class), and it was found after the question was asked.

Tests follow their subjects: ContextControlWordTest becomes ControlWordTest. Tests that are not about a renamed
class keep their names.

## Selection (SEL_cop_v6.tsv, HELD.tsv)

| Count | What |
|------:|------|
| 763 | synexia-cop main classes in 44 packages, selected by select_multi v1.4 (SLF4J_FACADE=1, LANDED_MAP_cop = landed through IOP) |
| 259 | eligible |
| 501 | held by selection: 399 need a held class, 50 simple-name collisions, 30 import lombok, 11 import com.synexia.bitop, 11 other imports or no type declaration |
| 3 | hubs held: Log, LongLongConsumer, Parser |
| 31 | eligible, but the donor source does not compile in com.synexia develop |
| 11 | eligible, but need one of those 31 |
| 5 | use java.sql (4) or javax.xml.stream (1); jdk.m3.paradigm requires only jdk.m3.collections, and new module wiring needs admission |
| 212 | main sources landed |
| 12 | tests landed; JsonMementoStoreTest is held with its subject |

The 31 donor defects are in com.synexia itself:
- missing imports: java.nio.file.Path, TimeUnit, UUID, java.util.concurrent types, lombok `Slf4j`;
- `TokenInterner.getInstance().intern("...")` written into annotation values ("expression not allowed as
  annotation value");
- nested types of IContextMemento used unqualified from IContextKernel.

The held set was computed by compiling to a fixed point on the M3 side. Round 0 held the donor defects. Each later
round held every file with an error, until a round had 0 errors.

## Recipes

- **RELOCATE_M3_OWNER v6.3 with relocate_v5_lib v5.4.**
  - v5.3: a nested type of the same name blocks a rename in its file.
  - v5.4 (SFA): string literals and text blocks are parked during the simple-name pass, so `"ContextIndexList"`
    stays as data. com.synexia FQNs in strings are still resolved.
- **SLF4J_TO_PLACEHOLDER_LOGGER v1.1:** 31 files.
- **LINT_SUPPRESS v1.2.** `[serial]` on 4 exception classes gets `@SuppressWarnings("serial")` on the class. It
  has source retention, so no serialVersionUID field is added and the class file is unchanged.
- **DOCLINT_REPAIR v1.5.** An unclosed `<ul>`/`<ol>` is closed before the next block element. The doclint error
  count went 41 → 0.
- **Banner.** `/*****` becomes `/* ****` on all 212 files.
- **DONOR_BUG_FIX (main code).** `ContextProgressMonitor.checkCancelled` threw a plain `CancellationException`.
  CancelledException (donor ContextCancelledException) extends it and has a package-private `(String)`
  constructor written for this throw. The method now throws that subtype again, which fixes 2 tests in
  ContextCancellationCompatibilityTest on both sides. com.synexia #16982 carries the same fix and is open,
  awaiting merge. The proof's donor snapshot uses that PR's blob 2f676d5c.

## Proof (PROOF.tsv)

The donor side compiles with lombok 1.18.46 and slf4j-api 2.0.17. Its ContextProgressMonitor is the #16982 blob.
`norm_cop.sed` maps the landed map plus this batch. Its 90 rename-back rules map each new simple name to its
`Context` name.
- **javap -c:** 0 of the eligible class files differ.
- **Mutant:** caught.
- **JUnit:** 67 tests in 12 classes on the donor and 67 relocated, all passing on both sides.
- **javap -v:** 13 class files differ. FACADE_RESIDUAL v1.1 over every eligible class pair: 431 equal, 13 differ only in the declared logger facade shape (PlaceholderLogger.of InterfaceMethodref, slf4j pool entries, lombok's CLASS-retention @lombok.Generated on the generated log field), 0 residual (FACADE_ALL.txt).

## Gates

| Gate | Result |
|------|--------|
| Compile | jdk.m3.paradigm with annotation, contract and context (607 sources before the hold) compiles under `-Xlint:all,-module -Werror -Xdoclint:all/protected,-missing` with 0 diagnostics |
| jdk_naming | Fixed point on src/jdk.m3.paradigm (0 moves, 0 rewrites) |
| Sprawl gate | new=212, refused=0 |
| One owner (module) | No simple name is declared in more than one jdk.m3.paradigm package |
| Custody | jdk.m3.collections sources are untouched |
| Default suite | The werror reactor (m3/arrays, m3/collections, m3/paradigm) passes on the IOP and COP tree. m3/collections: 2841 tests, 0 failures, 0 errors, 151 skipped. m3/paradigm: 310 tests, 0 failures, 0 errors. The first run failed 2 tests; that is the DONOR_BUG_FIX above. |
