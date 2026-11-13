# Gate: close observed recipe-proof failures

Base: PR88 `2aa6c8dcc7bc23e482c2a12e3b49e4de43fb2695`.
The complete local migration-recipes source tree was compared with the connected
Git subtree `1f6442214c7ae6ce7016f25641a81a563813f85d` before edits.

## Scope and purpose

Continue the existing whole-JDK programme by removing observed blockers in the
recipe proof module. This is not another parser, database, functional wrapper,
or replacement atomization engine. Database source, the measured database99%
gates, Java/HotSpot product source and public APIs remain locked.

The last executable full recipe job ran77 tests and failed12. DbMark already
addresses one metadata-oracle failure. This pass addresses the other observed
families and two additional literal-delimiter manifest defects found by inventory.

## Declared candidate changes

- Preserve strict crate constructors. Exercise explicitly configured recipes through
  OpenRewrite's existing composition DSL, and independently test configured
  serialization/deserialization and invalid-constructor rejection. Do not disable
  type validation or serialization flags to hide a failing gate.
- Exact snapshots must use SourceSpec.noTrim(); the default test DSL normalization
  is inappropriate for a hash-sealed source. Original source properties and output
  hashes stay unchanged.
- Two String-owner manifest files contain literal backslash-t sequences instead
  of actual TSV delimiters. Repair only those exact files; keep their source paths
  and both content hashes. The parser remains strict.
- The existing scope registry has14 entries including JEP458, while its test
  expects13. Assert the complete registered set and explicit JEP458 authority,
  rather than weakening the cardinality assertion.
- Distinguish scheduler exception wrapping from the guarded cause. Assert the
  exact preimage-drift exception/cause and target, not a broad successful refusal.
- A standalone java.base/module-info compilation unit makes javac attribution
  abort in the current OpenRewrite path. Route only that exact sealed descriptor
  to the existing PlainText recipe, while checking its syntax independently with
  the JDK compiler's ModuleTree. The total26 JEP458 targets and all pre/post hashes
  remain identical. Do not invent a Java LST or mark syntax as type/link proof.
  Full modified-JDK build/jtreg remains the authoritative product gate.

## Atom/pattern participation

Existing snapshot recipes retain Template/Recognizer/Materializer ownership.
Tests are ContractProbe participants. The change is in test configuration and
the manifest lane assignment, not application behavior. Module-relative Java
test repairs use the existing Java snapshot engine; repository-relative
manifest/resource changes use the existing text snapshot engine.

GateJava and GateMeta are recipe configurations, not new recipe classes.
All original tests remain present. Additional probes test configuration,
unmodified source identity, malformed inputs and the exact lane partition.
Recipes must prove replay, refusal and composed fixed points.

## Proof order and limits

Inventory -> documented candidate -> reviewed diff -> syntax/static checks ->
Java21 compilation -> actual JUnit/OpenRewrite -> configured coverage -> JDK
product/native tests. No run is complete merely because source was authored.
No reduced thresholds, test filters claimed as whole-module success, synthetic
dependency stubs, permission bypass, canonical merge, rebase or force-push.
