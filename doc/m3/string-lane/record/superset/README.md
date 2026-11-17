# Donor superset (owner decisions 33-40, 2026-11-17)

This folder covers 18 template-engine and expression-language donors: FreeMarker, Velocity (engine and tools),
StringTemplate 4, Pebble, Mustache.java, Handlebars.java, JTE, Jinjava, Thymeleaf with attoparser and unbescape,
Commons JEXL, MVEL, Expressly with the Jakarta EL API, OGNL, and Spring Expression. Each was atomized on CPU, then
consolidated into one additive superset against the M3JDK string baseline. The baseline is 161 files: java.lang
String, M3String* and the builders, plus `jdk.m3.text`, `jdk.m3.precompute`, the index-string package and
`java.util.regex`.

| File | Content |
|---|---|
| `DONOR_SOURCES.tsv` | Maven Central coordinates and SHA-1 of each binary and sources jar |
| `DONOR_FILES.tsv` | Java files per donor; 3,466 in total |
| `atomize_donors.sh` | runs the atomize kit (`M3AtomizeInventory`, OpenRewrite 8.17.1) and the hash planes over each donor and the baseline |
| `m3-atomize-noideas.yml` | `M3AtomizeInventoryNoIdeas`, used for FreeMarker; `M3CartesianRecipeIdeaExpansionRecipe` fails there with "CIR identity text" |
| `consolidate_superset.py` | one row per donor TYPE/METHOD atom: baseline plane, cross-donor cluster, status (REUSE, MERGE, VARIANT, INLINE), M3 class (M3_PATH, COMPOSITE) |
| `SUPERSET_SUMMARY.tsv` | per donor: 37,304 atoms; 3,941 REUSE, 4,755 MERGE, 28,608 INLINE; 12,383 string-relevant (11,342 M3_PATH, 3,432 COMPOSITE) |
| `SUPERSET_CLUSTERS.tsv` | 2,323 cross-donor clusters, each with its proposed owner |
| `base_closure.py`, `BASE_SUMMARY.tsv` | which donor files can enter java.base unchanged: 1,453 BASE; the others are blocked by outside libraries or by JDK modules |
| `MISSING_BITS.tsv` | the packages javac reports missing when all donors are compiled as a java.base patch (13,391 errors before bits are inlined) |

Every status is a proposal. Equivalence is decided by an oracle run or a diff, never by a hash alone. The full atom
table can be regenerated with the pinned jars and the tools above.

Decisions (see `../../ABSORB_PLAN.md` and `../../M3STRING_ARCHITECTURE.md`):

- The donors' code goes into java.base with upstream packages, atomized from the start: REUSE becomes calls to M3JDK
  owners, and MERGE keeps one owner across donors.
- The used bits of blocking libraries are inlined as well, relocated into internal packages. This includes ANTLR 3 and
  4, borrowed together with the univec vector kernels.
- Core uses of other JDK modules are rewritten onto java.base:
  - java.util.logging becomes System.Logger;
  - java.beans becomes an internal introspector;
  - java.sql checks are made by class name.
- The API bridges (servlet/JSP, DOM/SAX/XPath, JSR-223, Swing, Jython/Rhino) land in one companion module beside
  java.base.
