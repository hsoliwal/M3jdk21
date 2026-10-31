# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""gen_atom_tests.py <repo> <m3_classes_dir> <oracle_jars> [--check]

Template-first generator of the jdk.m3.graphics atom tests (CPU only, deterministic, no LLM). Inputs: the leaf-1
ledgers (ledgers/CLASSES.tsv patterns, leaf1/TYPE_RENAME_L1.tsv rename), javap of the compiled module classes (public
API, bytecode, enum flags) and javap of the oracle jars (skija-shared-0.143.18.jar and types-0.2.0.jar, separated by
the platform path separator). javap comes from $JDK_BIN, else $JAVA_HOME/bin, else PATH. Outputs:

  test/jdk/jdk/m3/graphics/M3GraphicsAtomsTestNN.java      one JUnit test per tested atom, files of <= MAX_LINES lines
  test/jdk/jdk/m3/graphics/M3GraphicsAtomsRecipes.java     how the tests build values of donor value types
  test/jdk/jdk/m3/graphics/M3GraphicsAtomsPerf.java        jtreg main/othervm harness, one row per tested atom
  test/jdk/jdk/m3/graphics/M3GraphicsAtomsPerfGroupNN.java static final MethodHandles and timed loops per row
  m3/tooling/skija-absorb/leaf1/ATOM_TESTS.tsv             per atom: pattern, status, reason, counts
  m3/tooling/skija-absorb/leaf1/ATOM_MEMBERS.tsv           per public member: kind, status, reason, cases

The hand-written runner (M3GraphicsAtoms*.java) executes the cases. The generator computes the same case counts
(cases(), mirror of M3GraphicsAtoms.cases) and every test asserts them. Members whose bytecode can reach native code
(native methods, class initializers that load the library, transitively) are BLOCKED_NATIVES; the tests fail when an
emitted member reaches natives, so the classification is checked by execution. --check regenerates in memory and
exits 1 when a generated file differs from disk (fixed point).
"""
import os
import re
import subprocess
import sys
import zipfile
from collections import defaultdict

JAVA_HEADER = ("/*\n * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0\n"
               " * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others\n */\n")
TOOL = "m3/tooling/skija-absorb/tools/gen_atom_tests.py"
TEST_DIR = "test/jdk/jdk/m3/graphics"
LEAF = "m3/tooling/skija-absorb/leaf1"
LEDGERS = "m3/tooling/skija-absorb/ledgers"
GENERATED = re.compile(r"^M3GraphicsAtoms(Test\d+|PerfGroup\d+|Recipes|Perf)\.java$")
DONOR_ROOT = "io.github.humbleui."
M3_ROOT = "jdk.m3.graphics"
RUNNABLE = ("ENUM", "VALUE_DATA", "STATIC_UTIL", "SUBTYPE_PURE")
NATIVE_PATTERNS = ("MANAGED_OWNED", "MANAGED_REFCNT", "NATIVE_STATIC_FACADE", "NATIVE_HANDLE", "IMPL_INFRA")
PATTERN_RANK = {p: i for i, p in enumerate(RUNNABLE + ("INTERFACE",) + NATIVE_PATTERNS)}
ONCE, RECEIVER_ROUNDS, ARG_ROUNDS, ENUM_ARG_ROUNDS = 1, 12, 24, 4
MAX_LINES = 300
MAX_LINE = 120
MAX_CONST_NAME = 24
BATCH = 80
KIND_ORDER = ("STATIC_FIELD", "FIELD", "CTOR", "STATIC_METHOD", "METHOD")
RECIPE_KINDS = ("PRIM", "STRING", "ENUM", "VALUE", "NULL", "ARRAY_PRIM", "ARRAY_STRING", "ARRAY_ENUM", "ARRAY_VALUE")
MODIFIERS = {"public", "protected", "private", "static", "final", "abstract", "native", "synchronized",
             "transient", "volatile", "strictfp", "default", "sealed", "non-sealed"}
NONDETERMINISTIC = re.compile(r"^(java\.lang\.System\.(currentTimeMillis|nanoTime|identityHashCode)"
                              r"|java\.time\..*\.now|java\.util\.Random\..*|java\.lang\.Math\.random"
                              r"|java\.util\.concurrent\.ThreadLocalRandom\..*|java\.util\.UUID\.randomUUID)$")
LOADS_NATIVES = re.compile(r"^java\.lang\.(System|Runtime)\.(load|loadLibrary)$")
HEADER = re.compile(r"^(?P<mods>(?:[a-z-]+ )*)(?P<kind>class|interface|enum|record) (?P<name>[\w.$]+)"
                    r"(?:<.*?>)?(?P<rest>.*?)\s*\{?$")
GETTERISH = re.compile(r"^(get|is|has|with)[A-Z_]")
NON_NULL_CHECK = "is marked non-null but is null"
OBJECT_METHODS = ("equals", "hashCode", "toString")


# ---------------------------------------------------------------- inputs

def rows(path):
    with open(path, encoding="utf-8") as f:
        head = f.readline().rstrip("\n").split("\t")
        return [dict(zip(head, line.rstrip("\n").split("\t"))) for line in f if line.strip()]


def javap_bin():
    for env, sub in (("JDK_BIN", ""), ("JAVA_HOME", "bin")):
        if os.environ.get(env):
            return os.path.join(os.environ[env], sub, "javap")
    return "javap"


def javap(args, names, cp):
    out, err = [], []
    for i in range(0, len(names), BATCH):
        r = subprocess.run([javap_bin()] + args + ["-cp", cp] + names[i:i + BATCH], capture_output=True,
                           text=True, encoding="utf-8", errors="replace")
        out.append(r.stdout)
        err.append(r.stderr)
    return "\n".join(out).replace("\r", ""), "\n".join(err).replace("\r", "")


def binary(internal):
    return internal.replace("/", ".")


def short(donor_binary):
    return donor_binary[len(DONOR_ROOT):]


def tokens(desc):
    """Parameter tokens and return token of a JVM descriptor."""
    i, out = 1, []
    while desc[i] != ")":
        j = i
        while desc[j] == "[":
            j += 1
        j = desc.index(";", j) + 1 if desc[j] == "L" else j + 1
        out.append(desc[i:j])
        i = j
    return out, desc[i + 1:]


def leading_mods(text):
    mods = set()
    for t in text.split():
        if t not in MODIFIERS:
            break
        mods.add(t)
    return mods


def declaration(s, owner):
    body = s[:-1]
    if body.startswith("static {}"):
        return {"kind": "clinit", "name": "<clinit>", "mods": {"static"}}
    head = body.split(" throws ")[0]
    if "(" in head:
        pre = head[:head.index("(")]
        name = pre.split()[-1]
        return {"kind": "ctor" if name == owner else "method", "name": name, "mods": leading_mods(pre)}
    return {"kind": "field", "name": head.split()[-1], "mods": leading_mods(head)}


def parse_header(line):
    m = HEADER.match(line)
    if not m or line.startswith(" "):
        return None
    rest = m.group("rest")
    sup = None
    ext = re.search(r" extends ([\w.$]+)", rest)
    if m.group("kind") == "class" and ext:
        sup = ext.group(1)
    is_enum = " extends java.lang.Enum<" in rest
    return {"name": m.group("name"), "kind": "enum" if is_enum else m.group("kind"),
            "mods": set(m.group("mods").split()), "super": sup, "members": []}


def parse_api(text):
    """javap -public -s: {binary name: type} with members [{kind, name, desc, mods}]."""
    types, cur, pending = {}, None, None
    for line in text.splitlines():
        if line.startswith("Compiled from") or not line.strip():
            continue
        head = parse_header(line)
        if head and line.rstrip().endswith("{"):
            cur = types[head["name"]] = head
            continue
        if line == "}":
            cur = None
            continue
        if cur is None:
            continue
        s = line.strip()
        if s.startswith("descriptor:") and pending:
            pending["desc"] = s.split(":", 1)[1].strip()
            cur["members"].append(pending)
            pending = None
        elif re.match(r"^  [^ #]", line) and s.endswith(";"):
            pending = declaration(s, cur["name"])
    return types


INSN = re.compile(r"^\s+\d+: (\w+)")


def member_ref(ref, owner):
    target, _, desc = ref.rpartition(":")
    if "." in target.replace('"<init>"', "").replace('"<clinit>"', ""):
        cls, _, name = target.rpartition(".")
    else:
        cls, name = owner, target
    return binary(cls.strip('"')), name.strip('"'), desc


def parse_bodies(text):
    """javap -c -p -s: classes {name: super} and bodies {(owner, name, desc): body facts}."""
    classes, bodies, cur, key = {}, {}, None, None
    pending = None
    for line in text.splitlines():
        head = parse_header(line)
        if head and line.rstrip().endswith("{"):
            cur = head["name"]
            classes[cur] = head["super"]
            continue
        if line == "}":
            cur = key = None
            continue
        if cur is None:
            continue
        s = line.strip()
        if s.startswith("descriptor:") and pending:
            name = "<init>" if pending["kind"] == "ctor" else pending["name"]
            key = (cur, name, s.split(":", 1)[1].strip())
            simple = {"ctor": "new", "clinit": "static"}.get(pending["kind"], pending["name"])
            bodies[key] = {"mods": pending["mods"], "calls": set(), "inits": set(), "jdk": set(),
                           "indy": False, "simple": simple}
            pending = None
            continue
        if re.match(r"^  [^ #]", line) and s.endswith(";"):
            pending = declaration(s, cur)
            key = None
            continue
        m = INSN.match(line)
        if key and m:
            record_insn(bodies[key], m.group(1), line, cur)
    return classes, bodies


def record_insn(body, op, line, owner):
    c = line.find("// ")
    ref = line[c + 3:].strip() if c >= 0 else ""
    kind, _, rest = ref.partition(" ")
    if kind == "InvokeDynamic":
        body["indy"] = True
    elif kind in ("Method", "InterfaceMethod"):
        target = member_ref(rest, owner)
        body["calls"].add(target)
        body["jdk"].add(target[0] + "." + target[1])
        if op == "invokestatic":
            body["inits"].add(target[0])
    elif kind == "Field" and op in ("getstatic", "putstatic"):
        body["inits"].add(member_ref(rest, owner)[0])
    elif kind == "class" and op == "new":
        body["inits"].add(binary(rest.strip('"')))
    elif kind == "String" and rest.endswith(NON_NULL_CHECK):
        body["nonnull"] = True


def parse_enum_flags(text):
    """javap -p -v of enum classes: {class: set of ACC_ENUM field names}."""
    out, cur, pending = defaultdict(set), None, None
    for line in text.splitlines():
        m = re.match(r"^\s*this_class: #\d+\s+// ([\w/$]+)", line)
        if m:
            cur, pending = binary(m.group(1)), None
            continue
        s = line.strip()
        if re.match(r"^  [^ #]", line) and s.endswith(";"):
            pending = None if "(" in s else s[:-1].split()[-1]
        elif pending and s.startswith("flags:"):
            if "ACC_ENUM" in s:
                out[cur].add(pending)
            pending = None
    return out


# ---------------------------------------------------------------- analysis

class Model:
    def __init__(self, repo, m3_classes, oracle_jars):
        self.repo = repo
        rename = rows(os.path.join(repo, LEAF, "TYPE_RENAME_L1.tsv"))
        self.donor_to_m3 = {r["old_fqn"]: r["new_fqn"] for r in rename}
        self.m3_to_donor = {v: k for k, v in self.donor_to_m3.items()}
        self.patterns = {}
        for r in rows(os.path.join(repo, LEDGERS, "CLASSES.tsv")):
            self.patterns[r["package"] + "." + r["type"]] = r["pattern"]
        m3_names = sorted(self.donor_to_m3.values())
        text, _ = javap(["-public", "-s"], m3_names, m3_classes)
        self.m3_api = parse_api(text)
        otext, oerr = javap(["-public", "-s"], sorted(self.donor_to_m3), oracle_jars)
        self.oracle_api = parse_api(otext)
        self.oracle_missing = sorted(set(re.findall(r"class not found: ([\w.$]+)", oerr)))
        self.mr_variants = multi_release_variants(oracle_jars)
        all_classes = sorted(p[:-6].replace(os.sep, "/").replace("/", ".")
                             for p in walk_classes(m3_classes))
        btext, _ = javap(["-c", "-p", "-s"], all_classes, m3_classes)
        self.classes, self.bodies = parse_bodies(btext)
        enums = sorted(n for n, t in self.m3_api.items() if t["kind"] == "enum")
        vtext, _ = javap(["-p", "-v"], enums, m3_classes)
        self.enum_constants = parse_enum_flags(vtext)
        self.touch = self.native_closure()
        self.nondet = self.nondet_closure()

    # ---- bytecode closures
    def resolve(self, owner, name, desc):
        o = owner
        while o in self.classes:
            if (o, name, desc) in self.bodies:
                return (o, name, desc)
            o = self.classes[o]
        return None

    def edges(self, key):
        body = self.bodies[key]
        out = [r for r in (self.resolve(*c) for c in sorted(body["calls"])) if r]
        if body["indy"]:
            prefix = "lambda$" + body["simple"] + "$"
            out += sorted(k for k in self.bodies if k[0] == key[0] and k[1].startswith(prefix))
        return out

    def init_touches(self, cls, touch):
        while cls in self.classes:
            if touch.get((cls, "<clinit>", "()V")):
                return True
            cls = self.classes[cls]
        return False

    def native_closure(self):
        touch = {k: "native" in b["mods"] or any(LOADS_NATIVES.match(j) for j in b["jdk"])
                 for k, b in self.bodies.items()}
        edges = {k: self.edges(k) for k in self.bodies}
        changed = True
        while changed:
            changed = False
            for k in sorted(self.bodies):
                if touch[k]:
                    continue
                inits = [c for c in sorted(self.bodies[k]["inits"]) if c != k[0]]
                if any(touch[e] for e in edges[k]) or any(self.init_touches(c, touch) for c in inits):
                    touch[k] = changed = True
        return touch

    def nondet_closure(self):
        nd = {k: any(NONDETERMINISTIC.match(j) for j in b["jdk"]) for k, b in self.bodies.items()}
        edges = {k: self.edges(k) for k in self.bodies}
        changed = True
        while changed:
            changed = False
            for k in sorted(self.bodies):
                if not nd[k] and any(nd[e] for e in edges[k]):
                    nd[k] = changed = True
        return nd

    def class_init_touches(self, m3_name):
        return self.init_touches(m3_name, self.touch)

    # ---- names and tokens
    def short_token(self, tok):
        dims = len(tok) - len(tok.lstrip("["))
        base = tok[dims:]
        if base.startswith("L"):
            b = binary(base[1:-1])
            outer, sep, nested = b.partition("$")
            donor = self.m3_to_donor.get(outer) if outer.startswith(M3_ROOT) else None
            if donor:
                b = donor + sep + nested
            if b.startswith(DONOR_ROOT):
                b = short(b)
            base = "L" + b.replace(".", "/") + ";"
        return "[" * dims + base

    def oracle_token(self, tok):
        return tok.replace("L" + DONOR_ROOT.replace(".", "/"), "L")


def multi_release_variants(jars):
    """{donor binary name: 'versions/N'} for classes a multi-release oracle jar replaces on Java N+ (JEP 238)."""
    out = {}
    for jar in jars.split(os.pathsep):
        with zipfile.ZipFile(jar) as z:
            for n in sorted(z.namelist()):
                m = re.match(r"^META-INF/(versions/\d+)/(.+)\.class$", n)
                if m and "module-info" not in m.group(2):
                    out[m.group(2).replace("/", ".")] = m.group(1)
    return out


def walk_classes(root):
    for d, _, files in os.walk(root):
        for f in files:
            if f.endswith(".class"):
                yield os.path.relpath(os.path.join(d, f), root)


def cases(kind, params, constants):
    """Mirror of M3GraphicsAtoms.cases."""
    enum = constants > 0
    if kind == "CTOR":
        return ONCE if params == 0 else ARG_ROUNDS
    if kind == "STATIC_METHOD":
        return ONCE if params == 0 else (constants * ENUM_ARG_ROUNDS if enum else ARG_ROUNDS)
    if kind == "METHOD":
        if enum:
            return constants * (ONCE if params == 0 else ENUM_ARG_ROUNDS)
        return RECEIVER_ROUNDS if params == 0 else ARG_ROUNDS
    if kind == "STATIC_FIELD":
        return ONCE
    return constants if enum else RECEIVER_ROUNDS


class Atom:
    def __init__(self, donor, m3):
        self.donor, self.m3 = donor, m3
        self.short = short(donor)
        self.simple = donor.rsplit(".", 1)[1]
        self.pattern, self.pattern_source = "-", "-"
        self.status, self.scope, self.reason = "-", "-", "-"
        self.members = []   # (kind, name, params tuple, desc string, status, reason, cases, nulls)
        self.constants = 0
        self.instances = []
        self.recipe = None
        self.api_diff = []
        self.test_class = "-"
        self.perf = None
        self.native_receiver = set()

    def tested(self):
        return [m for m in self.members if m[4] == "TESTED"]

    def total_cases(self):
        return sum(m[6] for m in self.tested())


class Generator:
    def __init__(self, model):
        self.m = model
        self.atoms = [Atom(d, model.donor_to_m3[d]) for d in sorted(model.donor_to_m3)]
        self.by_short = {a.short: a for a in self.atoms}
        for a in self.atoms:
            self.classify_pattern(a)
        self.pure = {a.short for a in self.atoms if a.m3 in model.m3_api and not model.class_init_touches(a.m3)}
        self.enums = {a.short for a in self.atoms if a.short in self.pure and self.api(a)["kind"] == "enum"}
        self.candidates = {a.short for a in self.atoms if a.short in self.pure and a.short not in self.enums
                           and (a.pattern in RUNNABLE or a.pattern == "INTERFACE")}
        for a in self.atoms:
            if a.m3 in model.m3_api:
                a.instances = self.static_instances(a)
        self.values = {s for s in self.candidates if self.by_short[s].instances}
        self.recipe_levels()
        for a in self.atoms:
            self.classify(a)

    def recipe_levels(self):
        """Which value types can be built: level 0 recipes take no value parameters, level k recipes take values
        (and arrays of values) of types buildable before level k. Then the final recipe of each type is its
        best-scored option among all buildable ones whose value parameters do not lead back to the type, so a typed
        constructor (TextBox(Rect, Direction)) wins over a JNI-style one (TextBox(FFFFI)) and building terminates."""
        level = 0
        while True:
            allowed = frozenset(self.values)
            chosen = {a.short: self.choose_recipe(a, allowed) for a in self.atoms
                      if a.short in self.candidates and a.recipe is None}
            for s, r in sorted(chosen.items()):
                if r:
                    self.by_short[s].recipe = r + (level,)
            self.values = {s for s in self.candidates if self.by_short[s].recipe or self.by_short[s].instances}
            if self.values == allowed:
                break
            level += 1
        for s in sorted(self.values):
            a = self.by_short[s]
            for _, (name, sp, deps) in self.recipe_options(a, frozenset(self.values)):
                if not any(self.depends(d, s) for d in deps):
                    a.recipe = (name, sp, a.recipe[2] if a.recipe else 0)
                    break

    def depends(self, start, target, seen=None):
        """True when building start (through the chosen recipes) builds target."""
        seen = set() if seen is None else seen
        if start == target:
            return True
        if start in seen:
            return False
        seen.add(start)
        r = self.by_short[start].recipe
        return bool(r) and any(self.depends(d, target, seen) for d in self.value_deps(r[1]))

    def recipe_depth(self, s):
        r = self.by_short[s].recipe
        deps = self.value_deps(r[1]) if r else []
        return 1 + max(self.recipe_depth(d) for d in deps) if deps else 0

    def value_deps(self, sp):
        return sorted({binary(t.lstrip("[")[1:-1]) for t in tokens(sp + "V")[0]
                       if self.param_kind(t).endswith("VALUE")})

    def api(self, a):
        return self.m.m3_api[a.m3]

    def classify_pattern(self, a):
        if a.donor in self.m.patterns:
            a.pattern, a.pattern_source = self.m.patterns[a.donor], "CLASSES.tsv"
            return
        t = self.m.m3_api.get(a.m3)
        if t is None:
            return
        fields = [x for x in t["members"] if x["kind"] == "field" and "static" not in x["mods"]]
        if t["kind"] == "enum":
            a.pattern = "ENUM"
        elif t["kind"] == "interface":
            a.pattern = "INTERFACE"
        elif fields and all("final" in x["mods"] for x in fields):
            a.pattern = "VALUE_DATA"
        else:
            a.pattern = "STATIC_UTIL"
        a.pattern_source = "STRUCTURAL(" + ("types-0.2.0" if a.short.startswith("types.") else "leaf1") + ")"

    def static_instances(self, a):
        t = self.api(a)
        if t["kind"] == "enum":
            return []
        own = "L" + a.m3.replace(".", "/") + ";"
        return sorted(x["name"] for x in t["members"] if x["kind"] == "field" and x["desc"] == own
                      and {"public", "static", "final"} <= x["mods"])

    def param_kind(self, tok, allowed=None):
        """PRIM, STRING, SELF (Object), ENUM, VALUE, NULL or ARRAY_<kind>. With allowed (recipe selection), a
        candidate value type outside allowed is PENDING: its recipe comes at a later level."""
        if tok.startswith("["):
            return "ARRAY_" + self.param_kind(tok[1:], allowed)
        if not tok.startswith("L"):
            return "PRIM"
        b = binary(tok[1:-1])
        if b in ("java.lang.String", "java.lang.CharSequence"):
            return "STRING"
        if b == "java.lang.Object":
            return "SELF"
        if b.startswith("skija.") or b.startswith("types."):
            if b in self.enums:
                return "ENUM"
            if b in (self.values if allowed is None else allowed):
                return "VALUE"
            if allowed is not None and b in self.candidates:
                return "PENDING"
        return "NULL"

    def choose_recipe(self, a, allowed):
        options = self.recipe_options(a, allowed)
        return options[0][1][:2] if options else None

    def recipe_options(self, a, allowed):
        """Constructors and static factories of a candidate value type usable as recipes, best first: fewest null
        parameters, most enum parameters, fewest value parameters, fewest arrays, most parameters, constructors
        first. A null parameter is unusable when the body rejects null (Lombok @NonNull check); when its type needs
        natives, the type's instances need natives (a.native_receiver)."""
        if a.short not in self.candidates:
            return []
        t = self.api(a)
        own = "L" + a.m3.replace(".", "/") + ";"
        options = []
        for x in t["members"]:
            if "public" not in x["mods"] or x["kind"] not in ("ctor", "method"):
                continue
            params, ret = tokens(x["desc"])
            factory = x["kind"] == "method" and "static" in x["mods"] and ret == own
            ctor = x["kind"] == "ctor" and "abstract" not in t["mods"] and t["kind"] == "class"
            if not (factory or ctor) or self.touches(a, x):
                continue
            shorts = [self.m.short_token(p) for p in params]
            kinds = [self.param_kind(p, allowed) for p in shorts]
            if not all(k in RECIPE_KINDS for k in kinds):
                continue
            if "NULL" in kinds and self.rejects_null(a, x):
                a.native_receiver |= {binary(p[1:-1]) for p, k in zip(shorts, kinds)
                                      if k == "NULL" and self.is_native_type(binary(p[1:-1]))}
                continue
            sp = "(" + "".join(shorts) + ")"
            name = "<init>" if ctor else x["name"]
            options.append(((kinds.count("NULL"), -kinds.count("ENUM"), sum(k.endswith("VALUE") for k in kinds),
                             sum(k.startswith("ARRAY") for k in kinds), -len(params), 0 if ctor else 1, name, sp),
                            (name, sp, self.value_deps(sp))))
        return sorted(options)

    def rejects_null(self, a, x):
        key = self.m.resolve(*self.body_key(a, x))
        return bool(key and self.m.bodies[key].get("nonnull"))

    def is_native_type(self, short_name):
        other = self.by_short.get(short_name)
        return bool(other) and (short_name not in self.pure or other.pattern in NATIVE_PATTERNS)

    def body_key(self, a, x):
        name = "<init>" if x["kind"] == "ctor" else x["name"]
        return (a.m3, name, x["desc"])

    def touches(self, a, x):
        if "native" in x["mods"]:
            return True
        if x["kind"] == "field":
            return False
        key = self.m.resolve(*self.body_key(a, x))
        return bool(key and self.m.touch.get(key))

    def nondeterministic(self, a, x):
        if x["kind"] == "field" or tokens(x["desc"])[1] == "V":
            return False
        key = self.m.resolve(*self.body_key(a, x))
        return bool(key and self.m.nondet.get(key))

    def classify(self, a):
        t = self.m.m3_api.get(a.m3)
        if a.donor in self.m.oracle_missing or a.donor not in self.m.oracle_api:
            a.status, a.reason = "NOT_APPLICABLE", "no donor class in the oracle jars (added in leaf 1)"
            return
        if t is None:
            a.status, a.reason = "NOT_APPLICABLE", "no M3 class"
            return
        self.compare_api(a)
        if a.short not in self.pure:
            a.status, a.scope = "BLOCKED_NATIVES", "-"
            a.reason = "class initializer reaches the native library (leaf 2)"
            return
        if a.short in self.enums:
            a.constants = len(self.m.enum_constants.get(a.m3, ()))
        receivers = a.short in self.enums or bool(a.instances) or bool(a.recipe)
        seen = set()
        for x in sorted(t["members"], key=lambda x: (x["kind"], x["name"], x["desc"])):
            row = self.member(a, t, x, receivers)
            if row and (row[0], row[1], row[2]) not in seen:
                seen.add((row[0], row[1], row[2]))
                a.members.append(row)
        a.members.sort(key=lambda r: (KIND_ORDER.index(r[0]) if r[0] in KIND_ORDER else 9, r[1], r[3]))
        self.atom_status(a)

    def member(self, a, t, x, receivers):
        if "public" not in x["mods"] or x["kind"] == "clinit":
            return None
        static = "static" in x["mods"]
        if x["kind"] == "field":
            if a.short in self.enums and x["name"] in self.m.enum_constants.get(a.m3, ()):
                return None
            kind, params, desc = ("STATIC_FIELD" if static else "FIELD"), (), "()"
            name = x["name"]
        else:
            ptoks, _ = tokens(x["desc"])
            params = tuple(self.m.short_token(p) for p in ptoks)
            desc = "(" + "".join(params) + ")"
            if x["kind"] == "ctor":
                if t["kind"] != "class" or "abstract" in t["mods"] or a.short in self.enums:
                    return None
                kind, name = "CTOR", "<init>"
            else:
                kind, name = ("STATIC_METHOD" if static else "METHOD"), x["name"]
        nulls = ",".join(p for p in params if self.param_kind(p) in ("NULL", "ARRAY_NULL"))
        status, reason = "TESTED", "-"
        if self.touches(a, x):
            status, reason = "BLOCKED_NATIVES", "native method" if "native" in x["mods"] else "reaches native code"
        elif self.nondeterministic(a, x):
            status, reason = "EXCLUDED", "nondeterministic result (clock or random)"
        elif kind in ("METHOD", "FIELD") and not receivers:
            native = a.pattern in NATIVE_PATTERNS or bool(a.native_receiver)
            status = "BLOCKED_NATIVES" if native else "EXCLUDED"
            reason = ("instances need native handles" if a.pattern in NATIVE_PATTERNS
                      else "instances need a native " + ",".join(sorted(a.native_receiver)) if a.native_receiver
                      else "no receiver without natives")
        n = cases(kind, len(params), a.constants) if status == "TESTED" else 0
        return (kind, name, params, desc, status, reason, n, nulls or "-")

    def atom_status(self, a):
        tested = a.tested()
        blocked = [m for m in a.members if m[4] == "BLOCKED_NATIVES"]
        runnable = a.pattern in RUNNABLE or a.pattern == "INTERFACE"
        if tested:
            a.status = "TESTED"
            a.scope = "FULL" if runnable else "STATIC_PURE"
            a.reason = "-" if not blocked else "%d members reach natives (leaf 2)" % len(blocked)
        elif a.pattern in NATIVE_PATTERNS or blocked:
            a.status, a.reason = "BLOCKED_NATIVES", "every member needs natives (leaf 2)"
        else:
            a.status = "NOT_APPLICABLE"
            a.reason = "no public members runnable without an instance" if a.members else "no public members"

    def compare_api(self, a):
        def surface(api, conv):
            out = set()
            for x in api["members"]:
                if "public" not in x["mods"]:
                    continue
                name = "<init>" if x["kind"] == "ctor" else x["name"]
                out.add((x["kind"], name, conv(x["desc"])))
            return out
        m3 = surface(self.m.m3_api[a.m3], self.short_desc)
        oracle = surface(self.m.oracle_api[a.donor], lambda d: self.m.oracle_token(d))
        a.api_diff = sorted(("M3_ONLY",) + x for x in m3 - oracle) + sorted(("ORACLE_ONLY",) + x for x in oracle - m3)

    def short_desc(self, desc):
        if not desc.startswith("("):
            return self.m.short_token(desc)
        params, ret = tokens(desc)
        return "(" + "".join(self.m.short_token(p) for p in params) + ")" + self.m.short_token(ret)

    def choose_perf(self, a):
        """The perf member: a computing instance method first, then static methods with parameters (valueOf for
        enums), equals, withers, constructors, getters, the rest; ties by name and descriptor."""
        def rank(m):
            kind, name, params = m[0], m[1], m[2]
            plain = not GETTERISH.match(name) and name not in OBJECT_METHODS
            order = [kind == "METHOD" and bool(params) and plain, kind == "METHOD" and plain,
                     kind == "STATIC_METHOD" and bool(params), kind == "METHOD" and name == "equals",
                     kind == "METHOD" and name.startswith("with"), kind == "CTOR" and bool(params),
                     kind == "METHOD", kind == "STATIC_METHOD", kind == "FIELD", True]
            return (order.index(True), name, m[3])
        tested = a.tested()
        return min(tested, key=rank) if tested else None


# ---------------------------------------------------------------- emission

def snake(s):
    """UPPER_SNAKE of an identifier; a leading underscore stays (_advanceX -> _ADVANCE_X)."""
    lead = "_" if s.startswith("_") else ""
    s = re.sub(r"(?<=[a-z0-9])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])", "_", s)
    s = re.sub(r"[^A-Za-z0-9]+", "_", s)
    return lead + (s.upper().strip("_") or "X")


def token_name(tok):
    dims = len(tok) - len(tok.lstrip("["))
    base = tok[dims:]
    name = base if not base.startswith("L") else snake(base[1:-1].rsplit("/", 1)[-1])
    return "ARR_" * dims + name


def desc_base(params):
    if not params:
        return "NONE"
    parts, prims = [], ""
    for t in params:
        if len(t) == 1:
            prims += t
            continue
        if prims:
            parts.append(prims)
            prims = ""
        parts.append(token_name(t))
    if prims:
        parts.append(prims)
    return "_".join(parts)


class Constants:
    ORDER = ("KIND", "SIDE", "PATTERN", "ATOM", "NAME", "DESC", "CASES", "HANDLE")

    def __init__(self):
        self.by_key, self.used, self.decl = {}, set(), defaultdict(list)

    def get(self, group, jtype, base, literal, unique=None):
        key = (group, literal if unique is None else unique)
        if key in self.by_key:
            return self.by_key[key]
        name, n = base, 2
        while name in self.used:
            name = "%s_%d" % (base, n)
            n += 1
        self.used.add(name)
        self.by_key[key] = name
        self.decl[group].append((name, jtype, literal))
        return name

    def lines(self, groups=None):
        out = []
        for g in self.ORDER:
            if groups and g not in groups:
                continue
            for name, jtype, literal in sorted(self.decl[g]):
                out += const_line(jtype, name, literal)
        return out


def const_line(jtype, name, literal):
    line = "    private static final %s %s = %s;" % (jtype, name, literal)
    if len(line) <= MAX_LINE:
        return [line]
    out = ["    private static final %s %s =" % (jtype, name)]
    parts = split_literal(literal) if literal.startswith('"') else [literal]
    out += ["            %s%s" % ("" if i == 0 else "+ ", p) for i, p in enumerate(parts)]
    out[-1] += ";"
    return out


def split_literal(literal):
    """A long Java string literal as a concatenation of literals cut after ';' (descriptor tokens)."""
    width = MAX_LINE - len("            + ;")
    chunks, cur = [], ""
    for piece in re.findall(r"[^;]*;|[^;]+", literal[1:-1]):
        if cur and len(cur) + len(piece) + 2 > width:
            chunks.append(cur)
            cur = ""
        cur += piece
    chunks.append(cur)
    return ['"%s"' % c for c in chunks]


def jstr(s):
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"') + '"'


JAVA_KEYWORDS = {"abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
                 "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float", "for",
                 "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native", "new",
                 "package", "private", "protected", "public", "return", "short", "static", "strictfp", "super",
                 "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void", "volatile", "while"}


def method_name(a, taken):
    base = a.simple[0].lower() + a.simple[1:]
    base = base + "Atom" if base in JAVA_KEYWORDS else base
    parts = a.short.split(".")
    name = base if base not in taken else "".join(parts[1:-1]) + a.simple
    n = 2
    while name in taken:
        name = "%s%d" % (base, n)
        n += 1
    taken.add(name)
    return name


KIND_TYPE = "M3GraphicsAtoms.Kind"


def kind_const(c, kind):
    return c.get("KIND", KIND_TYPE, kind, KIND_TYPE + "." + kind)


def atom_const(c, a):
    return c.get("ATOM", "String", "A_" + snake(a.simple), jstr(a.short))


def name_const(c, name):
    return c.get("NAME", "String", "N_" + ("INIT" if name == "<init>" else snake(name)), jstr(name))


def desc_const(c, params):
    base = desc_base(params)[:MAX_CONST_NAME].rstrip("_")
    return c.get("DESC", "String", "D_" + base, jstr("(" + "".join(params) + ")"))


def render_atom(a, c, method):
    lines = ["    /** %s %s -> %s: %d members, %d cases. */" % (a.pattern, a.short, a.m3.rsplit(".", 1)[1],
                                                              len(a.tested()), a.total_cases()),
             "    @Test", "    void %s() {" % method, "        M3GraphicsAtoms.atom(%s)" % atom_const(c, a)]
    for kind, name, params, _, status, _, _, _ in a.members:
        if status == "TESTED":
            lines.append("                .member(%s, %s, %s)" % (kind_const(c, kind), name_const(c, name),
                                                                  desc_const(c, params)))
    cases_name = c.get("CASES", "int", "CASES_" + snake(a.simple), str(a.total_cases()), unique=a.short)
    lines += ["                .verify(%s);" % cases_name, "    }"]
    return lines


def render_test(cls, atoms, methods):
    c = Constants()
    body = []
    for a in atoms:
        body += render_atom(a, c, methods[a.short]) + [""]
    first, last = atoms[0].short, atoms[-1].short
    head = [JAVA_HEADER, "/*", " * @test",
            " * @summary Generated by %s; do not edit. JUnit differential tests of" % TOOL,
            " *          jdk.m3.graphics atoms against the donor (HumbleUI Skija 0.143.18, Types 0.2.0), one test per",
            " *          atom, %d atoms: %s .. %s. The oracle jars come from" % (len(atoms), first, last),
            " *          -Dm3.graphics.oracle=<skija-shared jar><path separator><types jar>; without it the tests abort.",
            " * @modules jdk.m3.graphics", " * @run junit %s" % cls, " */", "",
            "import org.junit.jupiter.api.Test;", "", "public class %s {" % cls]
    return "\n".join(head + c.lines() + [""] + body[:-1] + ["}", ""])


def render_recipes(gen):
    c = Constants()
    entries, only = [], []
    for a in sorted(gen.atoms, key=lambda a: a.short):
        if a.short in gen.values and a.recipe:
            name, sp = a.recipe[0], a.recipe[1]
            params = tokens(sp + "V")[0]
            entries.append("            Map.entry(%s, new Recipe(%s, %s))" % (
                atom_const(c, a), name_const(c, name), desc_const(c, params)))
        elif a.short in gen.values:
            only.append(atom_const(c, a))
    lines = [JAVA_HEADER, "import java.util.Map;", "import java.util.Optional;", "import java.util.Set;", "",
             "/**",
             " * Generated by %s; do not edit. How the jdk.m3.graphics atom" % TOOL,
             " * tests build a value of a donor value type: a recipe, the public constructor ({@code <init>}) or static",
             " * factory whose parameters are primitives, Strings, enums, arrays of those, or null for types without a",
             " * native-free value, chosen by the generator: fewest null parameters, fewest arrays, most parameters,",
             " * constructors first. INSTANCE_ONLY lists the value types built from their static instances only.",
             " */", "final class M3GraphicsAtomsRecipes {",
             "    /** A constructor ({@code <init>}) or static factory and its parameter descriptor. */",
             "    record Recipe(String name, String descriptor) {}", ""]
    lines += c.lines()
    lines += ["    private static final Map<String, Recipe> RECIPES = Map.ofEntries("]
    lines += [e + ("," if i < len(entries) - 1 else ");") for i, e in enumerate(entries)]
    lines += ["    static final Set<String> INSTANCE_ONLY = Set.of(%s);" % ", ".join(only), "",
              "    private M3GraphicsAtomsRecipes() {}", "",
              "    /** The recipe of a donor value type (short donor name). */",
              "    static Optional<Recipe> recipe(String type) {", "        return Optional.ofNullable(RECIPES.get(type));",
              "    }", "}", ""]
    return "\n".join(lines)


def render_perf_group(cls, atoms):
    c = Constants()
    c.get("SIDE", "M3GraphicsAtomsOracle.Side", "M3", "M3GraphicsAtomsOracle.Side.M3")
    c.get("SIDE", "M3GraphicsAtomsOracle.Side", "ORACLE", "M3GraphicsAtomsOracle.Side.ORACLE")
    handles, rows_ = [], []
    for i, a in enumerate(atoms, 1):
        kind, name, params = a.perf[0], a.perf[1], a.perf[2]
        args = "%s, %s, %s, %s" % (atom_const(c, a), kind_const(c, kind), name_const(c, name), desc_const(c, params))
        pattern = c.get("PATTERN", "String", "P_" + snake(a.pattern), jstr(a.pattern))
        for side, prefix in (("M3", "M"), ("ORACLE", "O")):
            handles += const_line("MethodHandle", "%s%02d" % (prefix, i), "h(%s, %s)" % (side, args))
        rows_ += ["        p.row(%s, %s, %s, %s, %s," % (atom_const(c, a), pattern, kind_const(c, kind),
                                                  name_const(c, name), desc_const(c, params))]
        for prefix, end in (("M", ","), ("O", ");")):
            rows_ += ["            (x, n) -> { for (int k = 0; k < n; k++) for (Object[] a : x) try { "
                      "consume((Object) %s%02d.invokeExact(a)); }" % (prefix, i),
                      "                    catch (Throwable t) { consume(t); } return n; }%s" % end]
    first, last = atoms[0].short, atoms[-1].short
    lines = [JAVA_HEADER, "import java.lang.invoke.MethodHandle;", "", "/**",
             " * Generated by %s; do not edit. Perf rows of %d" % (TOOL, len(atoms)),
             " * jdk.m3.graphics atoms, %s .. %s:" % (first, last),
             " * per atom a static final MethodHandle per side (Mnn, Onn) and one timed loop per side over the same",
             " * calls; the loop calls the constant handle",
             " * directly and hands every result to the blackhole M3GraphicsAtomsPerfSupport.consume.", " */",
             "final class %s {" % cls]
    lines += c.lines() + handles + ["", "    private %s() {}" % cls, "",
                                    "    static void rows(M3GraphicsAtomsPerfSupport p) throws Throwable {"]
    lines += rows_ + ["    }", "",
                      "    private static MethodHandle h(M3GraphicsAtomsOracle.Side side, String atom, M3GraphicsAtoms.Kind kind,",
                      "            String name, String descriptor) {",
                      "        return M3GraphicsAtomsPerfSupport.handle(side, atom, kind, name, descriptor);", "    }", "",
                      "    private static void consume(Object o) {",
                      "        M3GraphicsAtomsPerfSupport.consume(o);", "    }", "}", ""]
    return "\n".join(lines)


def render_perf_main(groups, n_rows):
    lines = [JAVA_HEADER, "/*", " * @test",
             " * @summary Generated by %s; do not edit. One performance" % TOOL,
             " *          row per tested jdk.m3.graphics atom (%d rows): the same calls on the M3 type and on the" % n_rows,
             " *          donor type (Skija 0.143.18 / Types 0.2.0 jars from -Dm3.graphics.oracle=...) through static",
             " *          final MethodHandles, best of 7 rounds after 5 warm-up rounds, ns/op per side and the ratio.",
             " *          Guard: no atom slower than its oracle by more than 1.5x (-Dm3.graphics.perf.slack=<factor>).",
             " *          C2 only and foreground compilation (-XX:-TieredCompilation -Xbatch) keep the two sides'",
             " *          compiled loops alike; the blackhole keeps every result. Without the oracle: PERF_SKIPPED.",
             " * @requires vm.compiler2.enabled",
             " * @modules jdk.m3.graphics",
             " * @run main/othervm -Xmx512m -XX:-TieredCompilation -Xbatch -XX:+UnlockExperimentalVMOptions",
             " *      -XX:CompileCommand=blackhole,M3GraphicsAtomsPerfSupport::consume M3GraphicsAtomsPerf", " */", "",
             "public class M3GraphicsAtomsPerf {",
             "    private static final String SKIPPED = \"PERF_SKIPPED\\toracle jars not configured: -D\"",
             "            + M3GraphicsAtomsOracle.ORACLE_PROPERTY;", "",
             "    public static void main(String[] args) throws Throwable {",
             "        if (!M3GraphicsAtomsOracle.present()) {", "            System.out.println(SKIPPED);",
             "            return;", "        }", "        M3GraphicsAtomsPerfSupport p = new M3GraphicsAtomsPerfSupport();",
             "        p.header();"]
    lines += ["        %s.rows(p);" % g for g in groups]
    lines += ["        p.finish();", "    }", "}", ""]
    return "\n".join(lines)


def split(atoms, render, prefix):
    files, cur = [], []
    for a in atoms:
        trial = cur + [a]
        cls = "%s%02d" % (prefix, len(files) + 1)
        if cur and render(cls, trial).count("\n") > MAX_LINES:
            files.append(cur)
            cur = [a]
        else:
            cur = trial
    if cur:
        files.append(cur)
    return files


def tsv(header, data):
    return "\n".join(["\t".join(header)] + ["\t".join(str(x) for x in r) for r in data]) + "\n"


def generate(gen):
    out = {}
    tested = sorted((a for a in gen.atoms if a.tested()), key=lambda a: (PATTERN_RANK.get(a.pattern, 99), a.short))
    taken, methods = set(), {}
    for a in tested:
        methods[a.short] = method_name(a, taken)
        a.perf = gen.choose_perf(a)
    test_files = split(tested, lambda cls, at: render_test(cls, at, methods), "M3GraphicsAtomsTest")
    for i, group in enumerate(test_files, 1):
        cls = "M3GraphicsAtomsTest%02d" % i
        out["%s/%s.java" % (TEST_DIR, cls)] = render_test(cls, group, methods)
        for a in group:
            a.test_class = cls
    perf_files = split(tested, render_perf_group, "M3GraphicsAtomsPerfGroup")
    groups = []
    for i, group in enumerate(perf_files, 1):
        cls = "M3GraphicsAtomsPerfGroup%02d" % i
        groups.append(cls)
        out["%s/%s.java" % (TEST_DIR, cls)] = render_perf_group(cls, group)
    out["%s/M3GraphicsAtomsPerf.java" % TEST_DIR] = render_perf_main(groups, len(tested))
    out["%s/M3GraphicsAtomsRecipes.java" % TEST_DIR] = render_recipes(gen)
    out["%s/ATOM_TESTS.tsv" % LEAF] = tsv(
        ("atom", "donor", "pattern", "pattern_source", "status", "scope", "reason", "members_tested",
         "members_blocked_natives", "members_excluded", "cases", "receivers", "test_class", "perf_member", "api_diff",
         "oracle_variant"),
        [(a.m3, a.donor, a.pattern, a.pattern_source, a.status, a.scope, a.reason, len(a.tested()),
          sum(m[4] == "BLOCKED_NATIVES" for m in a.members), sum(m[4] == "EXCLUDED" for m in a.members),
          a.total_cases(), receivers(gen, a), a.test_class, perf_name(a), len(a.api_diff),
          gen.m.mr_variants.get(a.donor, "-"))
         for a in sorted(gen.atoms, key=lambda a: a.m3)])
    out["%s/ATOM_MEMBERS.tsv" % LEAF] = tsv(
        ("atom", "kind", "member", "status", "reason", "cases", "null_params"),
        [(a.m3, m[0], m[1] + m[3], m[4], m[5], m[6], m[7]) for a in sorted(gen.atoms, key=lambda a: a.m3)
         for m in a.members])
    return out


def receivers(gen, a):
    if a.short in gen.enums:
        return "enum constants"
    parts = (["static " + ",".join(a.instances)] if a.instances else []) + (
        ["recipe L%d %s%s" % (gen.recipe_depth(a.short), a.recipe[0], a.recipe[1])] if a.recipe else [])
    return "; ".join(parts) or "-"


def perf_name(a):
    return "%s %s%s" % (a.perf[0], a.perf[1], a.perf[3]) if a.tested() and a.perf else "-"


def main(argv):
    if len(argv) < 4:
        sys.stderr.write(__doc__)
        return 2
    repo, m3_classes, oracle = argv[1], argv[2], argv[3]
    check = "--check" in argv[4:]
    gen = Generator(Model(repo, m3_classes, oracle))
    out = generate(gen)
    test_dir = os.path.join(repo, TEST_DIR)
    stale = sorted(f for f in os.listdir(test_dir) if GENERATED.match(f)
                   and "%s/%s" % (TEST_DIR, f) not in out) if os.path.isdir(test_dir) else []
    differ = []
    for rel, text in sorted(out.items()):
        path = os.path.join(repo, rel)
        old = open(path, encoding="utf-8").read() if os.path.exists(path) else None
        if old != text:
            differ.append(rel)
            if not check:
                os.makedirs(os.path.dirname(path), exist_ok=True)
                with open(path, "w", encoding="utf-8", newline="\n") as f:
                    f.write(text)
    if not check:
        for f in stale:
            os.remove(os.path.join(test_dir, f))
    counts = defaultdict(int)
    for a in gen.atoms:
        counts[a.status] += 1
    tested = [a for a in gen.atoms if a.tested()]
    print("atoms=%d tested=%d blocked_natives=%d not_applicable=%d members_tested=%d cases=%d files=%d "
          "max_lines=%d api_diffs=%d changed=%d stale=%d" % (
              len(gen.atoms), counts["TESTED"], counts["BLOCKED_NATIVES"], counts["NOT_APPLICABLE"],
              sum(len(a.tested()) for a in tested), sum(a.total_cases() for a in tested), len(out),
              max(t.count("\n") for r, t in out.items() if r.endswith(".java")),
              sum(len(a.api_diff) for a in gen.atoms), len(differ), len(stale)))
    for a in gen.atoms:
        if a.donor in gen.m.mr_variants:
            print("MR_VARIANT\t%s\toracle loads META-INF/%s; M3 source is the base variant" % (
                a.m3, gen.m.mr_variants[a.donor]))
        for d in a.api_diff:
            print("API_DIFF\t%s\t%s" % (a.m3, "\t".join(d)))
    if check and (differ or stale):
        print("NOT_FIXED_POINT\t" + " ".join(differ + stale))
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
