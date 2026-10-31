# M3String/M3AST occurrence-source qualification

This candidate is stacked on M3JDK21 #1541 (which is stacked on #1539/#1538).
M3JDK21, not a wrapper, owns the generated product source; Synexia owns the canonical
OpenRewrite recipe. The API is additive, with no modifications to existing public APIs.

Run: `python3 m3/ast/qualification/ast-string-source-spans-20261020/verify.py`.

This validates exact recipe SHA-256 targets, strict Java 21 compilation and twice-run
deterministic occurrence/UTF-16/generation/forest proofs. It does not validate full JDK,
JNI, native memory or source-built installed-image contracts. The parent PR's full-image
gates remain outstanding; the receiving stage does not advance from String.
