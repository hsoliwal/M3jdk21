# JBS enhancements of JDK 22-27: backport scaffold for M3JDK21

Status: **SCAFFOLD** (2026-10-20). The JEP side is complete in `../JEP_CATALOGUE.tsv` and `../ALL_JEP_MATRIX.md`
(85 feature JEPs). This folder adds the rest of JBS: every fixed **Enhancement** of JDK 22 to 27, so that
M3JDK21 can take what is possible beyond the JEPs.

| File | Content |
| --- | --- |
| `JBS_ENHANCEMENTS_22_27.tsv` | 5,299 issues: release, key, component, subcomponent, priority, labels, summary, disposition |
| `JDK21U_BACKPORTED_KEYS.txt` | 3,552 main-issue keys that a fixed 21.0.x Backport issue links to |
| `ECLIPSE_JAVA22_API_DEMAND.tsv` | Java 22+ APIs the Eclipse forks call (from their `--release 21` gate residue): replace in Eclipse or backport here |
| `jbs_enhancements.sh` | pass 1: the JBS query, one row per issue (public JIRA REST API, no login) |
| `jbs_mark_21u.sh` | pass 2: `ALREADY_IN_JDK21U` when jdk21u already carries the issue |

| Release | Fixed enhancements | Already in jdk21u | To classify |
| --- | --- | --- | --- |
| 22 | 805 | 258 | 547 |
| 23 | 885 | 149 | 736 |
| 24 | 876 | 115 | 761 |
| 25 | 901 | 98 | 803 |
| 26 | 1010 | 76 | 934 |
| 27 | 822 | 37 | 785 |

## Fill (next passes, mechanical first)

1. Component lanes: map component/subcomponent to the owner lanes of `../POST21_PRIORITY_COMPATIBILITY.tsv`.
2. Dependency pass: an issue whose code builds on a JEP marked `reject-language` or `hold-*` in
   `../JEP_CATALOGUE.tsv` inherits that disposition.
3. Additive pass: issues that only add files, tools, jcmd/JFR options or API methods (the `candidate-adapted`
   pattern of `../UPSTREAM_CHANGE_SEEDS.tsv`) become seeds there, with the upstream commit pinned.
4. Everything admitted goes through the existing backport DAG (`../BACKPORT_RECIPE_DAG.md`), never around it.

Language features stay out: M3JDK21 is Java 21 and javac source acceptance is locked (`reject-language`). The
Eclipse forks are changed to Java 21 source instead (branch `m3/jdk21-compat-20261020` in each fork).
