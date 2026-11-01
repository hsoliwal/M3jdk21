# M3AST source seals and build consumers

M3AST attaches consumers to the `JavacTask` already created by the compiler or
build worker. Within that compilation, consumers receive the original javac
tree. Projections remain lazy, keyed by projection identity, and valid only on
the active callback thread. The public `com.sun.source.util.M3AST` API is
unchanged by this enhancement.

## Source-seal maintenance

The existing `com.sun.tools.javac.m3.M3ASTPC` owns all three changes:

* SHA-256 still covers the exact UTF-16 code units in big-endian byte order.
  A buffer of at most 8192 bytes replaces two digest updates per character.
  Lone surrogates and pairs split across blocks keep the previous digest.
* ANALYZE source requests reject a changed character count before calculating
  another digest. Equal-length edits still require and fail the digest check.
  Source reads and exception memoization remain scoped to the current event.
* An unchecked source-capture failure closes the session through its existing
  abort path before javac's later COMPILATION event can mark it successful.
  Consumers are released once, cleanup failures retain their suppression order,
  and the existing primitive-image consumer cannot publish on that failed path.

This last guarantee covers failures escaping M3AST's source-capture hook. It is
not a claim that every possible compiler, processor, or unrelated listener
failure has been classified by this patch.

## Build integration

The Maven fork's opt-in compiler configuration and the Gradle fork's worker
hooks use this same compiler-owned API. A provider attaches to the original
task; it must not call `JavaCompiler.getTask`, `parse`, or `analyze` to construct
a second frontend. The Gradle receiver provider is maintained under
`m3/tooling/module-packs/build-support/gradle-provider`.

Each forked JVM has its own javac context, classes, symbols and live trees.
Sharing a compiler distribution and attaching multiple consumers within each
task avoids duplicate frontend work by those consumers. It does not share a
live attributed tree between Maven and Gradle processes. Persistent reuse must
continue through the existing compatible immutable snapshot and cache owners.
This enhancement adds no persistent cache and changes no LRU or swap policy.

## Qualification and source custody

The canonical recipe is the existing
`synexia-openrewrite-recipes/recipe-crates/m3-ast-build-reverse-20261017`
in `hsoliwal/com.synexia`, dataset `m3jdk21-seals`. Its actual OpenRewrite SDK
results supply the product files after exact preimage, postimage, fixed-point
and refusal qualification. See that crate's
`enhancements/20261018-source-seals` for the contract and execution evidence.
The baseline compiler owner SHA-256 is
`845c70bdc5e77a62282417889dc77bf466fd0c0005f72112e86faf27ec616399`.

`M3ASTSourceSealTest` checks the actual digest owner against the unchanged scalar
algorithm, all 65536 code units, empty input, block seams and lone surrogates.
Real compiler tasks check shared tree/source identities, stale-source refusals,
duplicate-URI capture failure, no failed-path image publication, and cleanup
exception identity. The existing `M3ASTSharedFrontendTest` remains unchanged.
Execution results are reported separately; the test descriptions are not a
claim of a complete JDK build or end-to-end fork qualification.

The separate AST hierarchy from historical M3jdk21 PR #1192 is absent from the
pinned receiving master. Its additional observer hooks are not restored by this
focused patch. The recipe refuses that different whole-file preimage rather
than overwriting it, although the three modified source atoms are unchanged in
that historical version. No global porting-stage promotion is implied.
