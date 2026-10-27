<!-- SPDX-License-Identifier: Apache-2.0 -->
# Real javac AST projection from one original M3File capture

The executable contract reads actual original com.m3.file.M3File bytes
once into the existing immutable jdk.m3.parser.M3File, verifies the
source-bound M3Utf8SourceMap, and feeds only that original String to a
real JDK JavacTask via SimpleJavaFileObject.getCharContent.

The physical file is rewritten BEFORE parsing. openInputStream refuses
any attempted parser-side physical reopen. Two independent compiler
tasks must produce stable M3ASTIndex structural SHA256 hashes, exact
JAVAC kind/name facts, and UTF16 source spans over the captured version.
UTF8 byte offsets for Unicode literal ranges remain exact after edits.

The public M3ASTIngress.javac API retains SOURCE_UNVERIFIED, even though
this particular test controls the compilation unit's original String:
generic caller-supplied AST and M3File bindings are not automatically
certified. Compiler-event M3AST projection remains the authoritative
input-sealed path. This receiver adds no production dependency, alternate
tree, JNI handle, AST node classes or second source owner.

Concepts compared, no copied donor bodies: javac public JavacTask,
Tree-sitter byte source coordinates, Roslyn green/red immutable syntax,
rust-analyzer rowan typed views, OpenRewrite LST, Clang SourceManager,
LibCST source layout provenance.

Required additional gates: canonical Synexia Maven/OpenRewrite replay,
whole parser JMOD/jlink, patched compiler ABI, JNI and native M3FS
parity, real Tree-sitter/ANTLR/JDT donor adapters, and benchmarks.
