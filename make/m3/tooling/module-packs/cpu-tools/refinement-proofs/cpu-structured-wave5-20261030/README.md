# M3 CPU wave5: matching, multi-literal search, dataflow and DAG scheduling

First-party Java21 M3 implementation using exclusively primitive array/bitmask internal storage.
Exact four additive class sources and two independent algorithm-oracle fixtures originate
from the canonical Synexia Maven/OpenRewrite source-sealed crate `m3-cpu-structured-wave5-20261030`.
No new String owner, Node/Entry collection payload, subprocess or JNI ABI is introduced.

- Hopcroft-Karp bounded bipartite maximum matching (not weighted assignment)
- Aho-Corasick multi-pattern UTF-16 literal search over nonflattenable M3 CharSequence views
- Monotone backwards liveness with 64-bit variable masks and CSR predecessors
- Critical-path earliest/latest times on a checked, acyclic task DAG

Local strict OpenJDK21 JIT and `-Xint` oracles: 179,911 matching/search assertions and
649,078 dataflow/schedule assertions per mode, **828,989/mode**. Not a JMOD proof.
Full Maven/OpenRewrite SDK, installed M3JDK21 JMOD, whole product, jtreg and
performance are NOT_RUN. All 91 tool families remain tracked individually;
this receiver does NOT claim full family applications or full external CLI semantics.
