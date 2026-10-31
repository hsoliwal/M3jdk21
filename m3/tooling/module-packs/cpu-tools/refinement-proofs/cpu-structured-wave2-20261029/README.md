# M3 CPU structured tools — second-wave receiver (2026-10-29)

The original `com.m3.tools.cpu` module receives ten first-party primitive-array algorithms
and two independently executable Java 21 fixture programs from the sole canonical
Synexia source-sealed recipe. No recipe or experimental source transformation
engine is installed in M3JDK21. Array-backed data, indexed offsets, primitive
bit masks, and Java String/M3String public boundaries remain product-owned.

Implementation includes positive Horn fixed-point facts, bounded 1-safe Petri
reachability, min-cost assignment, event-time windows, data integrity masks,
canonical byte-RLE, exact bounded Cartesian spatial queries, SHA-256
provenance receipts, G-counter/vector-clock primitives, and narrow schema
field-widening compatibility. These do NOT implement all 91 structured-tool
families or general SMT/SQL/Protobuf/consensus/codec/POSIX behavior.

Local `javac --release 21 -Xlint:all -Werror` and the two independent tests
pass 1,018,676 checks per mode, both JIT and `-Xint`; these are candidate-only.
Maven/OpenRewrite SDK replay, full installed M3 JMOD, HotSpot/jtreg,
cross-platform JNI and performance gates are NOT_RUN. Review as draft and
retain all strict source seals, licenses and original product owner semantics.
