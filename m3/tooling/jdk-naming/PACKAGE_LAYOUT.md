# Package layout rule and gate

**Rule (user decision, 2026-11-08):** M3jdk21 does not sprawl in names or packages.
- New code joins an existing package and module root.
- A new package, a new source root, or a package split across more source roots needs the user's approval. That
  approval takes the form of a reviewed change to `PACKAGE_BASELINE.tsv`.
- Also in force:
  - one owner per concept;
  - packages by family, with no flat dump packages;
  - no redundant `M3` prefix on a type inside an m3 package (existing types are backlog, below);
  - `internal` is no excuse.

## Gate

`package_gate.py` (PACKAGE_LAYOUT_GATE v1) runs in `.github/workflows/m3jdk-naming.yml` on every pull request and
push to master. It sorts every Java main source root into one of four kinds:

| Kind | Meaning |
|---|---|
| REAL | has its own `pom.xml` or `module-info.java` |
| LOOSE | product code without either |
| COPY | evidence copies under qualification, recipe, custody, fixtures, tasks, before/after images, resources, ... |
| VENDOR | third-party code under `m3/vendor/` |

The gate fails on:
- a source root missing from the baseline (`NEW_ROOT`);
- a product package missing from the baseline (`NEW_PACKAGE`);
- a package declared by more roots than its baseline allows (`SPLIT_GROWS`).

Removals always pass, so consolidation only ever shrinks the baseline. Commands:

```sh
python3 -I m3/tooling/jdk-naming/test_package_gate.py               # 9 synthetic-tree checks
python3 -I m3/tooling/jdk-naming/package_gate.py check --tree HEAD
python3 -I m3/tooling/jdk-naming/package_gate.py baseline --tree HEAD > PACKAGE_BASELINE.tsv   # after approval only
```

## Census at the baseline (master 2026-11-08; tables in `package-census-20261108/`)

| Measure | Count |
|---|---|
| Source roots | 729: 244 REAL, 21 LOOSE, 307 COPY, 157 VENDOR |
| Product packages | 387, in 58 families |
| Product Java files | 4,351 |
| Vendored Java files | 22,539 |
| Split packages | 10 |
| `M3`-prefixed types inside `jdk.m3.*` / `jdk.internal.m3.*` packages | 2,189 |

## Consolidation backlog (needs the owners and the user; nothing here is executed)

1. **Split packages (`SPLIT.tsv`).** These are packages one module per class variant.

   | Package | Roots |
   |---|---|
   | `jdk.m3.collections.apache` | 93 |
   | `jdk.m3.collections.guava` | 24 |
   | `jdk.m3.collections.jdk` | 18 |
   | `jdk.m3.collections.spring` | 4 |
   | `jdk.m3.collections.eclipse.impl.map.mutable` | 2 |
   | `jdk.internal.m3.rewrite` | 5 |
   | `jdk.m3.rewrite.backport` | 2 |
   | `org.m3jdk.tooling.rewrite` | 2 |
   | `com.google.common.collect` | 3 |
   | `org.apache.commons.collections4` | 2 |

   Target: one root per package owner. The collections lane owns most of these.
2. **Vendored third-party code.** 157 roots, with several libraries present in more than one version:
   - jackson-core ×3, jackson-databind ×3, snakeyaml ×3, fastdoubleparser ×3;
   - commons-compress ×2, checker-qual ×2, antlr4-runtime ×2, tomlj ×2, rewrite-java ×2.

   Target per the absorb-first rule: absorb the needed shapes into M3 owners, then keep at most one copy of each
   library, or none.
3. **One-package families** to fold into a family owner or confirm: `jdk.m3.text`, `jdk.m3.desktop`,
   `jdk.m3.tornado`, `jdk.internal.m3.eclipse`, `jdk.internal.m3.mindex`. `FAMILIES.tsv` has the full list.
4. **Redundant `M3` prefix.** 2,189 types. Renaming them is public-API churn and needs the user's call on scope
   (new code only, or a rename recipe for existing types).
