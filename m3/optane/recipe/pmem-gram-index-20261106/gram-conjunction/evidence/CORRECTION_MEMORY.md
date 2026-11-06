# M3 source-sealed correction memory — 2026-11-05

Symptoms: one globally rarest two-byte gram can be present in thousands of source files even when the complete pattern is absent. For a deterministic 20,000-file corpus and query "abc", 10,103 files passed the original one-gram test but only 207 contained both needed grams and the complete pattern.

Corrective atom: intersect the two globally rarest *distinct* pattern grams before original first-hit KMP; maintain primitive sorted original file indices, source seal and all JNI/Java contracts. Use Java21/FILE or POSIX/JNI KMP only on survivors, never treat a gram membership signal as semantic equivalence or code-copy authority.

Independent frozen output: 85,005 brute-force assertions in each source version; exactly identical SHA-256 first-hit output root d81690f7723fa478d04cca40d51ea20c6023a9531c43ed1ab7b0463ff8ccff18; original regression 3,887 unchanged. A new 300-file JUnit adversary confirms true hits survive and extra false positives are pruned. No performance benchmark or real Maven/JNI proof is claimed.

Fix the canonical hash-pinned Maven/OpenRewrite recipe and fixtures first. Retain stale/missing-source refusal, self fixed point, and regenerate output in a strictly serial per-file pass. Full recipient native/toolchain and provenance gates remain HOLD.
