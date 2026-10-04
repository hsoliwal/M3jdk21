# DB recovery and bounded snapshot parsing

Status: candidate, not full JDK/database/parser admission.

## Observed state and owners
master ef39c669ee9ac77bee073f5b95479ec1613cdf3a retains PR61/79 history but
has no m3/indexdb tree or reactor entry. Recover the latest retained source
from 46112be5b68d8d3816b9d3100fc31d8b2e9de736, including both later test suites.
Preserve original module coordinates, APIs, licenses and existing tests.
A merged PR or retained ancestry is not source-content integration.

## Contract repair
The V1 semantic snapshot decoder currently preallocates ArrayLists from declared
row counts before checking whether those rows fit in the supplied byte array.
A 24-byte malicious header can request 50,000,000 slots and exhaust a small JVM.
It also replaces malformed UTF-8 in fields such as sourcePath, silently changing
stored metadata instead of rejecting corrupt bytes.

Retain V1 magic, version, encoding and valid-data results. Add:
- frame-budget atom: 4 bytes per string-length, 173 per node and 16 per edge,
  calculated with long before any count-sized allocation;
- text-decoding atom: standard JDK UTF-8 CharsetDecoder with REPORT;
- contract probes: child JVM with bounded heap, malformed UTF-8, legal Unicode,
  byte-for-byte V1 roundtrip, store error propagation and stable graph ties.

This explicitly tightens malformed-input behavior. It is not represented as
universal behavior equivalence on previously accepted corrupt data. No encoder,
public signature, global provider configuration or JDK product source is changed.
The byte budget is a structural bound, not a configurable process-memory quota.
The caller must own the payload during parsing. Encoder handling of unpaired
UTF-16 surrogates and total application storage budgets remain separate obligations.

## Composition and naming
The existing codec is the parser owner for this wire format, not a Java parser.
Budget and text are private operation atoms; validation/decoding compose inside
the existing decode method. Standard JDK functions, buffers and charset APIs are
reused; no M3Function or new database/parser framework is introduced.
New probe/test types use short names. Existing public class names are not renamed.

## Recipe and build ownership
Use the existing M3HashPinnedJavaSnapshotRecipe for the module-relative Java
restoration and the existing M3Jdk21HashPinnedTextSnapshotRecipe for repository-
relative Maven/workflow restoration. No generic engine or admission fence changes.
Retain all existing DB JUnit tests and its 0.99 line/branch gate without exclusions.
Maven reactor registration is MULTI_MODULE; codec corruption refusal is a reviewed
MODULE behavior repair. Canonical branches remain read-only; publish a draft PR.

## Verification
Record diff -> syntax/static -> strict Java21 compilation -> real JUnit/coverage
-> recipe replay and corruption/runtime evidence. Test source is not execution.
Keep the old artifact and source hashes as baseline; do not mutate the oracle.
Source restore completeness and per-file identities must be checked after GitHub
publication. Report full-JDK, Java source parser and whole-programme proof separately.
