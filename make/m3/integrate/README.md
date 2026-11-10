<!-- SPDX-License-Identifier: Apache-2.0 -->
# m3/ into the OpenJDK layout (recipe M3_TO_JDK_LAYOUT v1)

M3JDK is one source tree in the OpenJDK layout. This directory holds the recipe that dissolves the former top-level
`m3/` tree:

| m3/ content | Home | Phase |
| --- | --- | --- |
| product code of a Maven module (`src/main/java`, natives, tests) | `src/<module>/share/{classes,native,legal}`, `test/jdk` | P1, one area per PR, gated |
| build, release, recipe and qualification tools and their records | `make/m3/...` (approved 2026-11-08: build tooling -> make/m3) | P2 |
| documentation corpus (`m3/docs`, `m3/papers`) | `doc/m3/...` | P2 |
| libraries relocated to `com.m3.lib.*` | `src/jdk.m3.lib.<name>` (new module roots) | P3, needs approval |
| donor sources, image inputs, retained-variant archives, snapshots | removed from the tree, kept in history | P4, needs approval |

`MAPPING.tsv` has one row per `m3/` path of the base commit: old path, new path or `RETIRE`, rule, phase, status, mode,
blob id and reason. `RULES.tsv` (first match wins) and `AREAS.tsv` (the product areas, kept whole until their own P1
move) are the only inputs; a path no rule matches, two rows with one target, or a target that already exists refuses.
`AMBIGUOUS.tsv` lists the rows moved by default that need a review. `SUMMARY.tsv` counts rows per phase, status and
destination.

`m3/<x>` becomes `make/m3/<x>`, so every relation between sibling tools is unchanged; only references that leave the
shifted tree change. The steps of a phase:

```bash
python3 -I make/m3/integrate/m3_integrate.py map --repo . --rev <base> --out make/m3/integrate
python3 -I make/m3/integrate/m3_integrate.py move --phase P2 | git update-index --index-info   # pure moves
python3 -I make/m3/integrate/m3_integrate.py refs --repo . --base <base> --rev <moved> --phase P2 --ref <ref>
python3 -I make/m3/integrate/reseal_moves.py --root . --base <base> --tree <ref> --ref <ref> --phase P2 \
        --reseal make/m3/tooling/jdk-naming/reseal.py
python3 -I make/m3/integrate/m3_integrate.py repin --repo . --base <base> --rev <ref> --phase P2 --ledger RESEAL_P2.tsv --ref <ref>
python3 -I make/m3/integrate/reseal_moves.py ...   # once more: a fixed point when it changes nothing
python3 -I make/m3/integrate/m3_integrate.py residue --repo . --rev <ref> --phase P2
python3 -m unittest discover -s make/m3/integrate -p 'test_*.py'
```

`refs` carries root forms (`m3/<p>`, `$VAR/m3/<p>`, master URLs, `"m3" / "x"` path components), relative forms
(`../<p>`, pom `<module>`, `<relativePath>`, `${project.basedir}/<p>`, the implicit parent `../pom.xml`),
`Path(__file__).parents[N]`, and workflow path filters and sparse checkouts (as unions while the old directory still
holds content). Nested mirrors (`before/m3/...`, `overlay/m3/...`, `<sha>/m3/...`) and package paths
(`jdk/m3/...`) are not repository references and keep their bytes. `reseal_moves.py` runs the existing
JDK_NAMING_RESEAL owner with these moves, so digest pins of changed bytes follow (ledger `RESEAL_<phase>.tsv`).
A digest the owner leaves AMBIGUOUS (an unchanged copy still has the old bytes) is carried by `repin` only where the
record names the file within four lines. `PROTECT.tsv` lists files whose bytes are never rewritten (`bytes`: recipe
rule tables, rename ledgers, replayed fixtures) and tests whose `m3/...` literals are inputs of a recorded rename
(`fixture`: only their own location, relative forms and `__file__` parents, follows). Tests of live tools (release,
name contract, package gate) build synthetic trees in the real layout, so their literals follow the move.
