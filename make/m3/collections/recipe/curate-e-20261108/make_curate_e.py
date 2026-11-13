# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""make_curate_e.py: derive curate_rename_e.py (CURATE_E v1) from curate_rename_bc.py v1.5 by exact, counted edits.
CURATE_E generalizes the B+C recipe (user decisions 2026-11-08: drop M3 everywhere unless a java.* name clashes; internal
family subpackages; one owner per concept):
  - CURATE_CORE (default jdk.m3.collections) names the package whose types move/rename (jdk.internal.m3.collections for E1);
  - module-info gains `exports P` only for packages outside jdk.internal (internal families stay non-exported);
  - TESTS_AS_OWNERS=1: test types listed in the map are owners too (a test moves into its subject's family and drops the
    prefix with it).
The B+C recipe itself stays byte-identical (it is the landed record of #2789)."""
import os

H = os.path.dirname(os.path.abspath(__file__))
t = open(os.path.join(H, "curate_rename_bc.py"), encoding="utf-8", newline="").read()
E = [
    ('CORE = "jdk.m3.collections"\n',
     'CORE = os.environ.get("CURATE_CORE", "jdk.m3.collections")  # CURATE_E: the package whose types move/rename\n'
     'CORE_RE, CORE_SL, CORE_JNI = re.escape(CORE), CORE.replace(".", "/"), CORE.replace(".", "_")\n'
     'TESTS_AS_OWNERS = os.environ.get("TESTS_AS_OWNERS") == "1"\n', 1),
    ('FQN_DOT = re.compile(r"\\bjdk\\.m3\\.collections\\.(" + alt + r")" + W)',
     'FQN_DOT = re.compile(r"\\b" + CORE_RE + r"\\.(" + alt + r")" + W)', 1),
    ('FQN_SL = re.compile(r"\\b(L?)jdk/m3/collections/(" + alt + r")" + W)',
     'FQN_SL = re.compile(r"\\b(L?)" + re.escape(CORE_SL) + r"/(" + alt + r")" + W)', 1),
    ('JNI = re.compile(r"\\bJava_jdk_m3_collections_(" + alt + r")_")',
     'JNI = re.compile(r"\\bJava_" + CORE_JNI + r"_(" + alt + r")_")', 1),
    ('    owner = pkg == CORE and not is_test and name in ren\n',
     '    owner = pkg == CORE and (TESTS_AS_OWNERS or not is_test) and name in ren\n', 1),
    ('r"^\\s*import\\s+jdk\\.m3\\.collections\\.\\*\\s*;"', 'r"^\\s*import\\s+" + CORE_RE + r"\\.\\*\\s*;"', 1),
    ('r"^\\s*import\\s+jdk\\.m3\\.collections\\.(" + alt + r")\\s*;"', 'r"^\\s*import\\s+" + CORE_RE + r"\\.(" + alt + r")\\s*;"', 1),
    ('r"(?m)^(\\s*import\\s+(?:static\\s+)?)jdk\\.m3\\.collections\\.(" + alt + r")(?=[.;\\s])"',
     'r"(?m)^(\\s*import\\s+(?:static\\s+)?)" + CORE_RE + r"\\.(" + alt + r")(?=[.;\\s])"', 1),
    ('rel.replace(os.path.join("jdk", "m3", "collections") + os.sep + name + ".java"',
     'rel.replace(os.path.join(*CORE.split(".")) + os.sep + name + ".java"', 1),
    ('    pkgs = sorted({pp for pp, _ in ren.values() if pp != CORE})\n',
     '    pkgs = sorted({pp for pp, _ in ren.values() if pp != CORE and not pp.startswith("jdk.internal.")})\n', 1),
    ('"CURATE_BC renamed=', '"CURATE_E renamed=', 1),
    # v1.2 names as data in build config: surefire include/exclude patterns name test files (**/<Type>.java)
    ('        elif f == "RELOCATION_MAP.tsv" and os.sep + os.path.join("m3", "compiler") + os.sep in p:\n',
     '        elif f == "pom.xml":\n'
     '            t = open(p, encoding="utf-8", newline="").read()\n'
     '            n_ = re.sub(r"(\\*\\*/)(" + alt + r")(\\.java)", lambda m: m.group(1) + ren[m.group(2)][1] + m.group(3), t)\n'
     '        elif f == "RELOCATION_MAP.tsv" and os.sep + os.path.join("m3", "compiler") + os.sep in p:\n', 1),
    ('core_names = pkg_classes.get(CORE, set())\n',
     'core_names = set(pkg_classes.get(CORE, set()))  # v1.1: a snapshot of the OLD core names (the live set gains new names)\n'
     '# CURATE_E guard: two types may never get one FQN (a STAY_CORE type pulled back into a package that already has its new\n'
     '# simple name, or two renames onto one target); the run refuses instead of overwriting a file\n'
     '_eff = {}\n'
     'for _s, (_p, _n) in ren.items():\n'
     '    _eff.setdefault((_p, _n), []).append(_s)\n'
     'for _c in core_names - set(ren):\n'
     '    _eff.setdefault((CORE, _c), []).append(_c)\n'
     '_dup = {k: v for k, v in _eff.items() if len(v) > 1}\n'
     'if _dup:\n'
     '    raise SystemExit("CURATE_E refuses: duplicate target FQN " + "; ".join(f"{p}.{n} <- {v}" for (p, n), v in sorted(_dup.items())))\n'
     '# CURATE_E v1.1: effective simple names per package AFTER the run ({pkg: {new name: {old names}}}); two packages may\n'
     '# now hold one simple name (select.CollectionRequest, shape.CollectionRequest, root CollectionRequest)\n'
     'eff_names = {}\n'
     'for (_p, _n), _v in _eff.items():\n'
     '    eff_names.setdefault(_p, {}).setdefault(_n, set()).update(_v)\n', 1),
    # v1.1 clash guard: a referenced type s clashes when another type of the file's (target) package, another import or a
    # nested type already owns its new simple name
    ('    visible = other_imports | (pkg_classes.get(tpkg, set()) - {ren[s][1] for s in ren}) | (nested - {name})\n'
     '    fq = {s for s in sees if ren[s][1] in visible and ren[s][1] != s}\n',
     '    visible = other_imports | (nested - {name})\n'
     '    fq = {s for s in sees if ren[s][0] != tpkg and (ren[s][1] in visible or any(x != s for x in eff_names.get(tpkg, {}).get(ren[s][1], ())))}\n', 1),
    # v1.1 rule 4 decides on the OLD names the file referenced (after the rename two types may share the new name)
    ('    used = set(re.findall(r"\\b[A-Z][A-Za-z0-9_]*\\b", re.sub(r\'"(?:\\\\.|[^"\\\\\\n])*"\', \'""\', n_)))\n',
     '    used = set(re.findall(r"\\b[A-Z][A-Za-z0-9_]*\\b", re.sub(r\'"(?:\\\\.|[^"\\\\\\n])*"\', \'""\', n_)))\n'
     '    used_old = set(re.findall(r"\\b[A-Z][A-Za-z0-9_]*\\b", re.sub(r\'"(?:\\\\.|[^"\\\\\\n])*"\', \'""\', t)))\n', 1),
    ('        need = {new_fqn(s) for s in ren if ren[s][0] != CORE and ren[s][1] in used and s in sees and s not in fq}\n',
     '        need = {new_fqn(s) for s in ren if ren[s][0] != CORE and s in used_old and s in sees and s not in fq}\n', 1),
    ('            if pp != tpkg and nn in used and s not in fq:\n',
     '            if pp != tpkg and s in used_old and s not in fq:\n', 1),
]
for a, b, n in E:
    c = t.count(a)
    if c != n:
        raise SystemExit(f"anchor count {c} != {n}: {a[:70]}")
    t = t.replace(a, b)
t = t.replace('"""curate_rename_bc.py <tree root> <RENAME_MAP.tsv> [--check]',
              '"""curate_rename_e.py <tree root> <RENAME_MAP.tsv> [--check]   (CURATE_E v1, derived by make_curate_e.py\n'
              'from curate_rename_bc.py v1.5; env CURATE_CORE, TESTS_AS_OWNERS; internal packages are never exported)\n', 1)
open(os.path.join(H, "curate_rename_e.py"), "w", encoding="utf-8", newline="").write(t)
left = [l for l in t.splitlines() if "jdk.m3.collections" in l or "jdk/m3/collections" in l or "jdk_m3_collections" in l
        or 'm3\\\\.collections' in l]
print("written curate_rename_e.py; remaining literal mentions:", len(left))
for l in left:
    print("  ", l.strip()[:110])
