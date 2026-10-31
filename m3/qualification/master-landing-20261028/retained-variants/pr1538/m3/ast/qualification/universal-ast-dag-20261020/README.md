# Universal M3 AST/DAG qualification

This packet qualifies the focused primitive AST/DAG atom mechanics added on top of the current
M3JDK21 #1539 receiving line.

The verifier performs:

1. exact SHA-256 equality between every target Java file and its hash-pinned OpenRewrite postimage;
2. sorted/unique manifest validation with `ABSENT` preimages;
3. Java 21 strict compilation with `-Xlint:all -Werror`;
4. two deterministic executions of the dependency-free randomized self-test;
5. recipe registration check.

Run:

```bash
python3 m3/ast/qualification/universal-ast-dag-20261020/verify.py
```

This is a focused owner proof. It does not claim a complete OpenJDK image, jtreg/JCK, HotSpot/JNI,
or platform qualification. Those remain separate acceptance gates.
