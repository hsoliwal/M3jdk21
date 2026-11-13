# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""make_curate_e_bytecode.py: derive curate_e_bytecode.py from curate_bc_bytecode.py by exact, counted edits.
  - CURATE_CORE (default jdk.m3.collections) is the package whose types were renamed/moved;
  - source-file Utf8 constants (<Name>.java) are dropped on both sides: after E1 two families may hold one simple name
    (select.CollectionRequest, shape.CollectionRequest), so a file-name map would be ambiguous; the class identity is
    already compared through this_class / FQNs.
The B+C tool stays byte-identical (landed record of #2789)."""
import os

H = os.path.dirname(os.path.abspath(__file__))
t = open(os.path.join(H, "curate_bc_bytecode.py"), encoding="utf-8", newline="").read()
E = [
    ('CORE = "jdk.m3.collections"\n', 'CORE = os.environ.get("CURATE_CORE", "jdk.m3.collections")  # CURATE_E\n'),
    ('drop = re.compile(r"^(Classfile |', 'drop = re.compile(r"= Utf8\\s+[A-Za-z0-9_$]+\\.java\\s*$|^(Classfile |'),
    ('        if sub.startswith(os.path.join("jdk", "m3")) and top in ren:\n',
     '        if sub == os.path.join(*CORE.split(".")) and top in ren:\n'),
    # one alternation pass instead of 3 x len(pairs) passes (E1 maps ~850 types); longest key first, no chaining
    ('def norm(t):\n'
     '    for new, old in pairs:\n'
     '        for a, b in ((new.replace(".", "/"), old.replace(".", "/")), (new, old), ("$" + new.replace(".", "$"), "$" + old.replace(".", "$"))):\n'
     '            t = re.sub(re.escape(a) + r"(?![A-Za-z0-9_])", b, t)\n',
     '_FORMS = {}\n'
     'for _new, _old in pairs:\n'
     '    for _a, _b in ((_new.replace(".", "/"), _old.replace(".", "/")), (_new, _old), ("$" + _new.replace(".", "$"), "$" + _old.replace(".", "$"))):\n'
     '        _FORMS.setdefault(_a, _b)\n'
     '_RX = re.compile("(?:" + "|".join(re.escape(k) for k in sorted(_FORMS, key=len, reverse=True)) + r")(?![A-Za-z0-9_])") if _FORMS else None\n'
     '\n\n'
     'def norm(t):\n'
     '    if _RX:\n'
     '        t = _RX.sub(lambda m: _FORMS[m.group(0)], t)\n'),
]
for a, b in E:
    if t.count(a) != 1:
        raise SystemExit(f"anchor count {t.count(a)}: {a[:60]}")
    t = t.replace(a, b)
t = t.replace('"""curate_bc_bytecode.py <base classes>', '"""curate_e_bytecode.py (CURATE_E, derived by make_curate_e_bytecode.py) <base classes>', 1)
open(os.path.join(H, "curate_e_bytecode.py"), "w", encoding="utf-8", newline="").write(t)
print("written curate_e_bytecode.py")
