# absorb-cop2-20261110

The second synexia-cop batch for `jdk.internal.m3.paradigm.context` lands 36 main sources. It stacks on
absorb-cop-20261110 (M3jdk21 #2926).

These classes were eligible in the first COP selection, but they were held:
- 31 because their com.synexia sources did not compile;
- 11 because they need one of those 31.

com.synexia #16987 (DONOR_IMPORT_REPAIR v1, head 739b275a) repairs the 31. It adds 60 missing imports, restores
3 static imports that had lost `Math.`, and turns 15 `TokenInterner.getInstance().intern("...")` annotation values
back into their string literals. **#16987 is open and awaiting merge.** The proof's donor snapshot uses that PR's
blobs.

## Selection (SEL_cop2_v6.tsv, HELD.tsv)

| Count | What |
|------:|------|
| 42 | eligible classes held by absorb-cop-20261110 that do not need java.sql or java.xml |
| 6 | still held: they need GrailEventJournal (java.sql) or QualityEvidenceReports (javax.xml.stream) |
| 36 | main sources landed; none is renamed (none starts with `Context`) |

## Recipes

- **RELOCATE_M3_OWNER v6.3 with relocate_v5_lib v5.4.** LANDED_MAP_cop2 is the earlier landed map plus the 212
  COP-1 owners.
- **SLF4J_TO_PLACEHOLDER_LOGGER v1.2:** 18 files.
  - New rule: when a type already declares a field named `log`, lombok @Slf4j generates nothing ("Field 'log'
    already exists"). So the facade adds no field and only the annotation is removed.
  - AbstractEventStore keeps its own `List<IStoredEvent> log`.
- **LINT_SUPPRESS v1.3.**
  - `[serial]` on BulkheadFullException, RateLimitExceededException and RetryExhaustedException.
  - `[unchecked]` heap pollution on `IRetryPolicy.Builder.retryOn(Class<? extends Throwable>...)` gets
    `@SuppressWarnings("unchecked")`.
  - Both annotations have source retention.
- **DOCLINT_REPAIR v1.5:** 2 errors, down to 0.
- **Banner.** `/* ****`.

## Proof (PROOF.tsv)

The donor side is synexia-cop with the #16987 blobs, compiled with lombok 1.18.46 and slf4j-api 2.0.17.
- **javap -c:** 0 of the 143 class files differ.
- **Mutant:** caught on AbstractBaton. prove_mat v6.4 now also targets abstract classes. Every owner in this batch
  is abstract or an interface, so under v6.3 the probe found no target.
- **JUnit:** none of the COP tests is specific to these classes.
- **javap -v:** 17 class files differ. FACADE_RESIDUAL v1.2 (FACADE_ALL.txt) gives 126 equal, 17 differing only in
  the declared logger shape or in javap's padding after a stripped pool index, and 0 residual. v1.2 collapses that
  padding.

## Gates

| Gate | Result |
|------|--------|
| Compile | jdk.m3.paradigm with annotation, contract and context (237 context sources) compiles under `-Xlint:all,-module -Werror -Xdoclint:all/protected,-missing` with 0 diagnostics |
| jdk_naming | Fixed point on src/jdk.m3.paradigm with every file tracked (0 moves, 0 rewrites) |
| Sprawl gate | new=36, refused=0 |
| One owner (module) | No simple name is declared in more than one jdk.m3.paradigm package |
| Default suite | The werror reactor passes on the IOP + COP + COP-2 tree. m3/collections: 2841 tests, 0 failures, 0 errors, 151 skipped. m3/paradigm: 288 tests, 0 failures, 0 errors. |
