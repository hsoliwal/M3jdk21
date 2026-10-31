<!-- SPDX-License-Identifier: Apache-2.0 -->
# Real Eclipse JDT source provenance in the original M3AST

Eclipse JDT DOM ASTParser supplies UTF-16 positions, source geometry,
recovery markers and a mutable original JDT tree. M3ASTIngress.eclipseJdt
projects those original nodes into the ONE M3ASTIndex, without retaining a
parallel AST or loading JDT libraries as a production parser dependency.

Independently supplied JDT ASTNode plus M3File never proves parser input
identity. The original JDT ingress previously labeled syntax/ranges but did
not set SOURCE_UNVERIFIED. The canonical Synexia Java recipe adds this
mandatory bit to every resulting occurrence. It preserves EXACT_RANGE,
SYNTAX_ONLY, RECOVERED, node order and all existing public owner APIs.

The isolated Maven descriptor resolves real org.eclipse.jdt.core 3.47.0
into the integration-test classpath only. The executable contract creates
real ASTParser compilation units, supplies exactly one captured M3File text,
and verifies original UTF-16 geometry and missing source-attestation flags.
The fixture does not infer source certification from equal URI or length.
Upstream JDT remains Eclipse Public License 2.0, an external TEST dependency;
no donor source is copied or commingled into the modified OpenJDK runtime.

Remaining gates: JDT semantic/binding-resolution parity, actual provenance
seals where the original JDT owner can prove source capture, whole parser JMOD,
custom HotSpot/JNI, full Eclipse/Maven/Gradle reactor and benchmarks.
