# Real JDT to M3AST ingress proof

`M3JdtIngressProof` qualifies the existing `jdk.m3.parser.M3ASTIngress.eclipseJdt` operation with the source-built JDT in this receiving source. It uses real `ASTParser`, structural descriptors, child lists, bindings and `ASTRewrite`. The comparison oracle reads JDT's typed child-property contracts directly and checks every indexed occurrence's parent, first child, next sibling, child count, kind, UTF-16 source range, source slice and recovery flags.

The seven tests cover these finite cases:

| Case | Contract |
| --- | --- |
| Java 21 record, record pattern and `when` guard | Complete structural traversal, including supplementary Unicode before declarations; the offsets count UTF-16 units. |
| Incomplete source with real JDT recovery | Original ranges and `RECOVERED`/`MALFORMED` bits are preserved through the index's recovered flag. |
| Ninety-six statements in one block | Child-list and ingress capacity growth preserve every occurrence and sibling; source, occurrence and depth limits refuse independently. |
| Same-parent reorder and newly inserted node | JDT list identity and order remain authoritative; a node without source positions remains `(-1,-1)`; the preceding index remains immutable. |
| ASTRewrite applied and reparsed | Rewriting leaves the original DOM and index intact; reparsing creates a new source generation and complete index. |
| Cross-parent move with stale source positions | JDT accepts the move, but the index explicitly refuses a child whose old range escapes its new parent. Apply text edits and reparse before indexing that source generation. |
| Bound record | JDT's resolved binding identity remains intact; the structural index continues to advertise syntax-only qualification. |

The caller supplies the `M3File` belonging to the parsed source generation. This API cannot independently authenticate a supplied text snapshot against an arbitrary mutable JDT node. Source positions in an edited DOM still refer to the original parse; they do not imply that edited syntax has already been written to that text. Detached comments that are not structural children of the compilation unit are outside the structural-descriptor traversal. These tests do not qualify all recovery inputs, language levels, working-copy reconciliation, incremental compilation or the Eclipse workspace runtime.

## Run

First build the parent receiving reactor with its default Java 21 core target. Supply its output directory and the platform/JUnit dependency directory from that source build. The runner refuses a dependency directory containing an additional `ASTParser` or `EclipseCompiler` owner.

Use the emitted named parser module with a Java 21 JDK:

```sh
python3 proof/run.py \
  --compiler-home /absolute/path/to/jdk21 \
  --runtime-home /absolute/path/to/jdk21 \
  --compiler-module-path /absolute/path/to/emitted/parser/modules \
  --runtime-module-path /absolute/path/to/emitted/parser/modules \
  --jdt-build /tmp/jdt-array-nodelist-receiving \
  --dependency-dir /absolute/path/to/source-build/dependencies \
  --output /tmp/jdt-real-ingress-proof
```

For a source-built M3JDK21 image containing that same parser module, set `--runtime-home` to the image and omit `--runtime-module-path`. The compiler can still use a Java 21 toolchain and the exact emitted module path. This executes the parser module linked in the image without a runtime patch or module replacement.

The runner writes the exact compile/run commands, toolchain identity, source and artifact checksums, runtime module-image checksum, named-module descriptor and seven-test output to `receipt.json`. The JDT core and compiler JARs must come from the locked receiving source build; installing a released JDT binary does not reproduce this proof.
