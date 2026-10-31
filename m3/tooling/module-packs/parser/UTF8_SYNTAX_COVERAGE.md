<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3AST exact syntax coverage and M3File UTF-8 coordinates

Existing M3ASTIndex is the sole AST occurrence geometry owner.
M3ASTSyntaxCoverage inspects EXTERNAL token and trivia lanes and proves exact
ordered, gap-free source coverage with a SHA-256 of UTF-16BE code units.
Recovered/missing syntax is refused unless explicitly admitted. A passed
coverage check says nothing about whether the foreign parser used this same
M3File generation: SOURCE_UNVERIFIED remains unmodified.

M3Utf8SourceMap accepts a caller-owned original captured byte array and the
existing M3File's original-byte SHA-256 identity. It rechecks SHA-256,
strictly decodes UTF-8 and compares the original decoded text before deriving
two primitive checkpoint lanes, one per 128 Unicode scalar values.
No extra source String, source file read, retained byte array, second parser,
or native memory owner is introduced. Byte positions in the middle of a UTF-8
scalar and UTF-16 positions splitting a surrogate pair refuse.
The UTF-8 BOM is retained as U+FEFF by the Java UTF-8 decoder.
This map does not claim atomic filesystem generations or byte equality beyond
the SHA evidence. A caller may reuse an existing M3FS/M3File captured byte array
to avoid a second provider read.

Open-source concepts compared: Tree-sitter field names, byte positions,
concrete tokens and recovery; Roslyn full fidelity immutable syntax;
rust-analyzer/rowan source-preserving CST; OpenRewrite type-attributed LST;
LibCST whitespace-aware position metadata; Clang SourceManager spelling versus
expansion locations. No donor source copied or relicensed.

Not yet included: actual Tree-sitter/ANTLR/JDT/Clang adapters, macro source maps,
typed symbols, incremental reparse/edit maps, fast JNI parity, or whole-M3JDK
HotSpot/JMOD/GC regression evidence.
