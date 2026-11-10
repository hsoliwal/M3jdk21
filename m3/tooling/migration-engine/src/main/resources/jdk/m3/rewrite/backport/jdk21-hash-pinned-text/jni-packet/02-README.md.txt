# Native JDK build-repair packet — 2026-10-04

Current task is the actual product build blocker from Actions run37181307729 job111374285910,
not additional search/Nebula work. Source root remains the exact PR84 tarball0c957d0. Two product
files are reviewed: jni.cpp (all4027lines inventoried) and MIndexStringBacking.java (fully read),
with the existing javaClasses handle factories, exception macros, sole mapped implementation,
String admission hook and existing native/Java fixtures read as dependency context.

1. Reuse java_lang_String::create_from_unicode/create_from_str, which already return GC Handles.
   Their oop wrappers already call those factories. CHECK_NULL must occur at statement scope;
   nesting its multi-statement expansion in a Handle constructor is invalid C++.
2. Explicitly redeclare internal backing close() without checked exceptions, matching the sole
   in-tree implementation's existing behavior. This narrows the internal inherited throws contract;
   it is NOT a signature-only claim of full source compatibility for hypothetical out-of-tree
   implementors. No supported Java SE API or binary method descriptor changes. No warning
   suppression or exclusions are used. Keep existing names and the mapped implementation body.
3. Reuse the existing runtime-integration patch engine in an isolated module instance with this
   packet directory as its resource context; do not implement another native patch engine.
4. Test exact/reverse/fixedpoint/drift/symlink/mixed-state boundaries, original warning reproduction,
   actual backing Java21 compilation and mapped-store tests. Existing String/JNI probe is the runtime
   oracle after a native product build. No mock compile is labelled a HotSpot build.
5. Build in an isolated candidate checkout with warnings-as-errors. Save recipe and exact delta;
   a remote candidate branch may be created only after recorded selected proof passes; no default
   branch write, force push, rebase or merge. Complete programme status remains separate.

## Execution and custody

`mvn -f m3/runtime-integration/fix/pom.xml verify` runs recipe-contract tests and checks source
readiness without changing source. `-Papply` explicitly applies the two-file packet before verify.
The adapter loads the existing engine into an isolated resource context; the engine source checksum
is locked. Historical runtime supersession is extended separately so older patches cannot undo the
new JNI state.

The opt-in push workflow targets only m3/jdk-proof-20261004. It has contents-write permission only
for the product job to create a new m3/jni-checked-<exact-base> ref after real product build and
existing JNI/String semantic tests. It POST-creates that ref; occupied refs fail, never overwrite.
No canonical branch update, merge, rebase, squash or force push occurs. The candidate commit is a
child of the exact original GitHub head, not of a reconstructed local source-export history.
Artifacts contain source diffs/logs, not credentials. An incomplete/full-jtreg gate is never inferred
from the focused runtime result. Native failure logs are preserved and an unsuccessful build saves
no product branch. New Git objects alone are not a published branch or acceptance claim.
