# Runtime recipe-home closure — current M3 String supersession

Status: candidate target-specific closure on exact M3JDK21 master `c1acefe582ac84414f1082f095e96bbee167bc33`.

## Purpose

Close two current receiver/infrastructure failures without moving JDK runtime ownership into Synexia:

1. the historical char-array-copy recipe still requires the removed
   `src/java.base/share/classes/java/lang/MIndexString.java` owner even though current M3JDK21
   has superseded it with `M3String.java`;
2. the Synexia recipe-home workflow executes `git diff --check origin/master...HEAD` after a
   depth-1 checkout, so the merge-base history may be unavailable.

## Ownership

The char-array-copy recipe is target-specific JDK runtime custody. It remains M3JDK21-owned because
its semantics depend on exact `java.lang.String`/M3 String product state and target runtime tests.
This does not create a second canonical reusable Synexia recipe family.

Reusable recipe authorship continues to follow `m3/docs/SYNEXIA_CONVERGENCE_MODEL.md` and the
pinned Synexia canonical recipe-home manifest.

## Supersession rule

A historical recipe may recognize a removed owner as `superseded` only when its manifest names one
exact replacement path and exact Git blob identity. It must not recreate the historical owner,
mutate the replacement owner, or infer equivalence from names.

For this packet the admitted replacement is:

```text
src/java.base/share/classes/java/lang/MIndexString.java
  -> src/java.base/share/classes/java/lang/M3String.java
     Git blob 3137d574684eac57260775ce13f8fbe2bf7ef0bc
```

If the replacement path/blob drifts, the historical recipe fails closed.

## CI history rule

The recipe-home workflow must fetch sufficient Git history before computing a merge base. The
workflow change does not weaken `git diff --check`; it makes the existing check executable.

## Acceptance

- historical before/after transforms remain reversible in isolated fixtures;
- current canonical tree reports `superseded` and performs zero writes;
- missing/wrong replacement owner refuses;
- unknown source drift refuses;
- recipe-home policy tests run after a valid diff-hygiene step;
- no public JDK API, native ABI, coverage threshold, existing test, or Synexia canonical owner changes.
