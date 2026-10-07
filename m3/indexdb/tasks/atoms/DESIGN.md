# DB atom fault contracts

## Authority and bounded objective

Continue PR88 from 89373b64356a8b7d045ce721fc59907e522e6f9e.
The exact DB source tree is 8a1be2b7ba3aed18fda9aab93c31d22dc79e393f.
Executed baseline: run37184634302, 51 JUnit cases pass; JaCoCo reports
663/681 lines and235/241 branches. These are separate receipts, not a green verify.

## Cohesive private operations

Retain the existing store and codec. Extract four cohesive operations in their
existing files: size aggregation, atomic-move fallback, edge ordinal emission,
and length-prefixed text reading. No public/package API, new engine, function
wrapper, dependency, persisted format, validation policy or coverage gate changes.
Private operation tests use reflection rather than widening visibility.

The size operation owns and closes its already-filtered stream, catches the
existing SizeFailure after resource close, and unwraps the original IOException.
The public method builds the regular-file filter without executing it, then
transfers stream ownership. No change to evaluation order of predicate/size
callbacks or their exceptions is intended. Resource-exhaustion behavior during
pipeline construction is not certified equivalent.
The other three extractions retain the exact statements and order of each block.

## Faults and realism

Use a real ZIP filesystem and ordinary filesystem to exercise the move fallback;
no custom provider implementation or provider registration is needed. Use an
absent path to exercise size failure and cleanup. Test missing edge ordinals and
string-table membership at the private atom boundary, never by corrupting an
immutable public graph. Test short input reads with a stream that reports a
deliberately overstated availability estimate, leaving every production guard in place.

The required SHA-256-unavailable behavior is tested only in a fresh, bounded
child JVM with an empty java.security.properties override at startup. No call to
Security.removeProvider/addProvider and no parent-JVM security mutation is
allowed. Only existing JaCoCo agent arguments are inherited, using append mode
for the same evidence file; the child runs serially and exits before the parent.
The no-provider child must not perform ZIP/temp-file operations requiring RNG.

## Recipes and fixed point

Use the existing hash-pinned Java recipe engine. DbAtom owns exact store/codec
preimages and new fault tests. Update current recovery templates to the reviewed
final sources; preserve historical before templates and Git ancestry. DbJava,
DbParse, DbAtom and DbBuild each replay their own final outputs without mutation.
No hash alone certifies behavior: actual JUnit, compiler and API checks are required.

## Admission

All51 retained DB tests remain, as do all existing recipe tests. New tests must
execute against real JUnit/OpenRewrite, not substitutes. Keep the0.99 line and
branch gates and all production classes in the denominator. Public graph/wire
round trips and original/candidate API descriptors remain explicit checks.
No JDK-wide completion, native/JNI parity, transactional SQL or Java AST parser
is implied by this bounded DB pass. Canonical master/develop remain untouched.
