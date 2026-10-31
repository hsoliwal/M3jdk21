# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""gen_atom_tests.py <repo> <m3_classes_dir> <oracle_jars> [--check]

Template-first generator (v2) of the jdk.m3.graphics atom tests (CPU only, deterministic, no LLM). Inputs: the leaf-1
ledgers (ledgers/CLASSES.tsv patterns, leaf1/TYPE_RENAME_L1.tsv rename, leaf1/MR_SELECT.tsv Multi-Release sources),
javap of the compiled module classes (public API, bytecode, enum flags) and javap --multi-release 21 of the oracle
jars (skija-shared-0.143.18.jar and types-0.2.0.jar, separated by the platform path separator), which is what a JDK
21 class loader reads from them. javap comes from $JDK_BIN, else $JAVA_HOME/bin, else PATH. Outputs:

  test/jdk/jdk/m3/graphics/M3GraphicsAtomsTestNN.java       one JUnit test per tested atom, files of <= MAX_LINES lines
  test/jdk/jdk/m3/graphics/M3GraphicsAtomsContractTest.java plain JUnit contract tests of atoms without a donor class
  test/jdk/jdk/m3/graphics/M3GraphicsAtomsRecipes.java      how the tests build values and proxies of donor types
  test/jdk/jdk/m3/graphics/M3GraphicsAtomsDomains.java      int parameters used as enum ordinals or switch codes
  test/jdk/jdk/m3/graphics/M3GraphicsAtomsPerf.java         jtreg main/othervm harness, one row per tested atom
  test/jdk/jdk/m3/graphics/M3GraphicsAtomsPerfGroupNN.java  static final MethodHandles and timed loops per row
  m3/tooling/skija-absorb/leaf1/ATOM_TESTS.tsv              per atom: pattern, status, reason, counts
  m3/tooling/skija-absorb/leaf1/ATOM_MEMBERS.tsv            per public member: kind, status, reason, cases

v2 (review of #2243): null checks are read per parameter from the bytecode (Lombok @NonNull, requireNonNull); equals
and hashCode also run on copies with exactly one field changed, built through a field-mapping constructor; an int
parameter that indexes an enum's values or selects a switch case gets values from that domain; boxed primitives,
Supplier, Runnable and URL parameters get values instead of null; a virtual call on a type whose instances need
natives does not reach natives here (such receivers are always null), so members guarded by a null check are tested;
interface default methods run on test proxies; atoms without a donor get plain contract tests; tests run with
identity hashes pinned (-XX:hashCode=2) so every hashCode compares exactly; perf rows carry SAME or DIFF, the
normalized bytecode of the member and its module callees on both sides; the Multi-Release variant the oracle loads
must be the MR_SELECT source of the M3 class, and the TYPE_RENAME rows must follow the runner's rename rule.

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
GENERATED = re.compile(r"^M3GraphicsAtoms(Test\d+|PerfGroup\d+|Recipes|Perf|Domains|ContractTest)\.java$")
DONOR_ROOT = "io.github.humbleui."
M3_ROOT = "jdk.m3.graphics"
M3_PREFIX = "M3"
SKIJA = "skija"
RUNNABLE = ("ENUM", "VALUE_DATA", "STATIC_UTIL", "SUBTYPE_PURE")
NATIVE_PATTERNS = ("MANAGED_OWNED", "MANAGED_REFCNT", "NATIVE_STATIC_FACADE", "NATIVE_HANDLE", "IMPL_INFRA")
NATIVE_INSTANCE_PATTERNS = ("MANAGED_OWNED", "MANAGED_REFCNT", "NATIVE_HANDLE")
NATIVE_BASES = ("jdk.m3.graphics.impl.M3Native",)
PATTERN_RANK = {p: i for i, p in enumerate(RUNNABLE + ("INTERFACE",) + NATIVE_PATTERNS)}
ONCE, RECEIVER_ROUNDS, ARG_ROUNDS, ENUM_ARG_ROUNDS = 1, 12, 24, 4
PERTURB_ROUNDS = 2
SNEAKY_CASES = 6
MR_RELEASE = "21"
HASH_PINNED_OPTIONS = "-XX:+UnlockExperimentalVMOptions -XX:hashCode=2"
MAX_LINES = 300
MAX_LINE = 120
MAX_CONST_NAME = 24
MAX_DOMAIN_DEPTH = 3
INTS_PER_LINE = 16
BATCH = 80
KIND_ORDER = ("STATIC_FIELD", "FIELD", "CTOR", "STATIC_METHOD", "METHOD", "PERTURB")
BOXED = frozenset("java.lang." + n for n in ("Boolean", "Byte", "Character", "Short", "Integer", "Long", "Float",
                                            "Double"))
JDK_KINDS = {"java.util.function.Supplier": "SUPPLIER", "java.lang.Runnable": "RUNNABLE", "java.net.URL": "URL"}
RECIPE_KINDS = ("PRIM", "STRING", "ENUM", "VALUE", "NULL", "BOXED", "SUPPLIER", "RUNNABLE", "URL", "ARRAY_PRIM",
                "ARRAY_STRING", "ARRAY_ENUM", "ARRAY_VALUE")
STABLE_FIELD_TYPES = frozenset(("Ljava/lang/String;",) + tuple("L%s;" % b.replace(".", "/") for b in BOXED))
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
NPE_CLASS = "class java/lang/NullPointerException"
REQUIRE_NON_NULL = "java/util/Objects.requireNonNull"
OBJECT_INIT = 'java/lang/Object."<init>":()V'
NPE_INIT = 'java/lang/NullPointerException."<init>"'
ASSERTIONS_DISABLED = "$assertionsDisabled:Z"
OBJECT_METHODS = ("equals", "hashCode", "toString")
EQUALS_PARAMS = ("Ljava/lang/Object;",)
SNEAKY_DESC = "(Ljava/lang/Throwable;)Ljava/lang/RuntimeException;"
INSN = re.compile(r"^\s+(\d+): ([a-z][a-z0-9_]*)(.*)$")
CASE = re.compile(r"^\s+(-?\d+|default): (\d+)\s*$")
LOAD = re.compile(r"^([ailfd])load(?:_(\d))?$")
SWITCHES = ("tableswitch", "lookupswitch")
BRANCHES = frozenset(("ifeq", "ifne", "iflt", "ifge", "ifgt", "ifle", "if_icmpeq", "if_icmpne", "if_icmplt",
                      "if_icmpge", "if_icmpgt", "if_icmple", "if_acmpeq", "if_acmpne", "goto", "goto_w", "jsr",
                      "jsr_w", "ifnull", "ifnonnull"))
OP_ALIASES = {"ldc_w": "ldc", "goto_w": "goto", "jsr_w": "jsr"}
DONOR_REF = re.compile(r"io/github/humbleui/[A-Za-z0-9_/$]+")
M3_REF = re.compile(r"jdk/m3/graphics/[A-Za-z0-9_/$]+")
INDY_INDEX = re.compile(r"InvokeDynamic #\d+:")
WIDE_SLOTS = ("J", "D")


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


def param_slots(desc, static):
    """Local variable slots of the parameters (slot 0 is this for an instance member; long and double take two)."""
    slot, out = 0 if static else 1, []
    for t in tokens(desc)[0]:
        out.append(slot)
        slot += 2 if t in WIDE_SLOTS else 1
    return out


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


def member_ref(ref, owner):
    target, _, desc = ref.rpartition(":")
    if "." in target.replace('"<init>"', "").replace('"<clinit>"', ""):
        cls, _, name = target.rpartition(".")
    else:
        cls, name = owner, target
    return binary(cls.strip('"')), name.strip('"'), desc


def parse_bodies(text):
    """javap -c -p -s: classes {name: super} and bodies {(owner, name, desc): facts and instructions}."""
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
            bodies[key] = {"kind": pending["kind"], "mods": pending["mods"], "calls": set(), "direct": set(),
                           "inits": set(), "jdk": set(), "indy": False, "simple": simple, "insns": []}
            pending = None
            continue
        if re.match(r"^  [^ #]", line) and s.endswith(";"):
            pending = declaration(s, cur)
            key = None
            continue
        if key is None:
            continue
        c = CASE.match(line)
        insns = bodies[key]["insns"]
        if c and insns and insns[-1]["op"] in SWITCHES:
            insns[-1]["cases"].append((c.group(1), int(c.group(2))))
            continue
        m = INSN.match(line)
        if m:
            record_insn(bodies[key], int(m.group(1)), m.group(2), m.group(3), cur)
    return classes, bodies


def record_insn(body, offset, op, rest, owner):
    c = rest.find("// ")
    ref = rest[c + 3:].strip() if c >= 0 else ""
    arg = (rest[:c] if c >= 0 else rest).strip()
    body["insns"].append({"off": offset, "op": op, "arg": arg, "ref": ref, "cases": []})
    kind, _, target = ref.partition(" ")
    if kind == "InvokeDynamic":
        body["indy"] = True
    elif kind in ("Method", "InterfaceMethod"):
        t = member_ref(target, owner)
        body["calls"].add(t)
        body["jdk"].add(t[0] + "." + t[1])
        if op in ("invokestatic", "invokespecial"):
            body["direct"].add(t)
        if op == "invokestatic":
            body["inits"].add(t[0])
    elif kind == "Field" and op in ("getstatic", "putstatic"):
        body["inits"].add(member_ref(target, owner)[0])
    elif kind == "class" and op == "new":
        body["inits"].add(binary(target.strip('"')))
    elif kind == "String" and target.endswith(NON_NULL_CHECK):
        body["nonnull"] = True


def load_slot(insn):
    """(type letter, slot) of a local-variable load instruction, else None."""
    m = LOAD.match(insn["op"])
    if not m:
        return None
    return m.group(1), int(m.group(2)) if m.group(2) is not None else int(insn["arg"].split()[0])


def ref_target(insn, owner):
    return member_ref(insn["ref"].partition(" ")[2], owner)


def without_asserts(ins):
    """Instructions without javac assert blocks (getstatic $assertionsDisabled; ifne <end>; ...; athrow)."""
    out, skip_to = [], None
    for i, x in enumerate(ins):
        if skip_to is not None and x["off"] < skip_to:
            continue
        skip_to = None
        nxt = ins[i + 1] if i + 1 < len(ins) else None
        if x["op"] == "getstatic" and x["ref"].endswith(ASSERTIONS_DISABLED) and nxt and nxt["op"] == "ifne":
            skip_to = int(nxt["arg"])
            continue
        out.append(x)
    return out


def null_checked_slots(body):
    """Slots a body rejects when null: Lombok's @NonNull check (load, ifnonnull, new NullPointerException) or a
    requireNonNull call on the loaded value."""
    ins, out = body["insns"], set()
    for i, x in enumerate(ins[:-1]):
        ls = load_slot(x)
        if not ls or ls[0] != "a":
            continue
        nxt, after = ins[i + 1], ins[i + 2:i + 4]
        lombok = nxt["op"] == "ifnonnull" and any(y["op"] == "new" and y["ref"] == NPE_CLASS for y in after)
        required = any(y["op"] == "invokestatic" and REQUIRE_NON_NULL in y["ref"]
                       for y in [nxt] + ([after[0]] if after and nxt["op"].startswith("ldc") else []))
        if lombok or required:
            out.add(ls[1])
    return out


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


def jar_classes(jars):
    out = set()
    for jar in jars.split(os.pathsep):
        with zipfile.ZipFile(jar) as z:
            out |= {n[:-len(".class")].replace("/", ".") for n in z.namelist()
                    if n.endswith(".class") and not n.startswith("META-INF/") and "module-info" not in n}
    return sorted(out)


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


def rename_rule(donor):
    """The runner's rename rule (M3GraphicsAtomsOracle.m3Name); TYPE_RENAME_L1.tsv must follow it."""
    dot = donor.rindex(".")
    pkg = donor[len(DONOR_ROOT):dot]
    sub = pkg[len(SKIJA):] if pkg.startswith(SKIJA) else ""
    return M3_ROOT + sub + "." + M3_PREFIX + donor[dot + 1:]


# ---------------------------------------------------------------- analysis

class Model:
    def __init__(self, repo, m3_classes, oracle_jars):
        self.repo = repo
        rename = rows(os.path.join(repo, LEAF, "TYPE_RENAME_L1.tsv"))
        self.donor_to_m3 = {r["old_fqn"]: r["new_fqn"] for r in rename}
        self.m3_to_donor = {v: k for k, v in self.donor_to_m3.items()}
        self.rename_mismatches = sorted(d for d, m in self.donor_to_m3.items() if rename_rule(d) != m)
        self.patterns = {r["package"] + "." + r["type"]: r["pattern"]
                         for r in rows(os.path.join(repo, LEDGERS, "CLASSES.tsv"))}
        self.mr_select = {r["name"]: r for r in rows(os.path.join(repo, LEAF, "MR_SELECT.tsv")) if r["kind"] == "CLASS"}
        mr = ["--multi-release", MR_RELEASE]
        self.m3_api = parse_api(javap(["-public", "-s"], sorted(self.donor_to_m3.values()), m3_classes)[0])
        otext, oerr = javap(mr + ["-public", "-s"], sorted(self.donor_to_m3), oracle_jars)
        self.oracle_api = parse_api(otext)
        self.oracle_missing = sorted(set(re.findall(r"class not found: ([\w.$]+)", oerr)))
        self.mr_variants = multi_release_variants(oracle_jars)
        all_classes = sorted(p[:-6].replace(os.sep, "/").replace("/", ".") for p in walk_classes(m3_classes))
        self.classes, self.bodies = parse_bodies(javap(["-c", "-p", "-s"], all_classes, m3_classes)[0])
        _, self.oracle_bodies = parse_bodies(javap(mr + ["-c", "-p", "-s"], jar_classes(oracle_jars), oracle_jars)[0])
        enums = sorted(n for n, t in self.m3_api.items() if t["kind"] == "enum")
        self.enum_constants = parse_enum_flags(javap(["-p", "-v"], enums, m3_classes)[0])
        self.fields = defaultdict(list)
        for k, b in self.bodies.items():
            if b["kind"] == "field" and "static" not in b["mods"]:
                self.fields[k[0]].append((k[1], k[2]))
        self.native_instances = {}
        self.edge_map = {k: self.edges(k) for k in self.bodies}
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

    def native_instance(self, cls):
        """True when instances of a module class need natives: managed or native-handle patterns, or a subclass of
        impl.M3Native. A virtual call on such a type needs such an instance, and the tests never have one."""
        if cls not in self.native_instances:
            c, found = cls, False
            while c in self.classes and not found:
                donor = self.m3_to_donor.get(c, "")
                found = c in NATIVE_BASES or self.patterns.get(donor) in NATIVE_INSTANCE_PATTERNS
                c = self.classes[c]
            self.native_instances[cls] = found
        return self.native_instances[cls]

    def edges(self, key):
        body = self.bodies[key]
        out = [r for r in (self.resolve(*c) for c in sorted(body["calls"])
                           if c in body["direct"] or not self.native_instance(c[0])) if r]
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
        changed = True
        while changed:
            changed = False
            for k in sorted(self.bodies):
                if touch[k]:
                    continue
                inits = [c for c in sorted(self.bodies[k]["inits"]) if c != k[0]]
                if any(touch[e] for e in self.edge_map[k]) or any(self.init_touches(c, touch) for c in inits):
                    touch[k] = changed = True
        return touch

    def nondet_closure(self):
        nd = {k: any(NONDETERMINISTIC.match(j) for j in b["jdk"]) for k, b in self.bodies.items()}
        changed = True
        while changed:
            changed = False
            for k in sorted(self.bodies):
                if not nd[k] and any(nd[e] for e in self.edge_map[k]):
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

    def m3_internal(self, donor_internal):
        outer, sep, nested = donor_internal.partition("$")
        m3 = self.donor_to_m3.get(binary(outer))
        return (m3.replace(".", "/") if m3 else outer) + sep + nested

    def donor_internal(self, m3_internal):
        outer, sep, nested = m3_internal.partition("$")
        donor = self.m3_to_donor.get(binary(outer))
        return (donor.replace(".", "/") if donor else outer) + sep + nested

    # ---- bytecode identity (perf rows)
    def oracle_key(self, key):
        cls, name, desc = key
        outer, sep, nested = cls.partition("$")
        donor = self.m3_to_donor.get(outer)
        if not donor:
            return None
        return donor + sep + nested, name, M3_REF.sub(lambda m: self.donor_internal(m.group(0)), desc)

    def same_body(self, key):
        okey = self.oracle_key(key)
        other = self.oracle_bodies.get(okey) if okey else None
        return other is not None and normalized(self.bodies[key], lambda s: s) == normalized(
            other, lambda s: DONOR_REF.sub(lambda m: self.m3_internal(m.group(0)), s))

    def same_closure(self, key):
        """True when the body and every module body it reaches are the same code on both sides (modulo rename)."""
        seen, todo = set(), [key]
        while todo:
            k = todo.pop()
            if k in seen:
                continue
            seen.add(k)
            if not self.same_body(k):
                return False
            todo += self.edge_map[k]
        return True


def normalized(body, rename):
    """Instructions without offsets and constant-pool indexes; branch targets as instruction numbers."""
    index = {x["off"]: i for i, x in enumerate(body["insns"])}
    out = []
    for x in body["insns"]:
        op = OP_ALIASES.get(x["op"], x["op"])
        arg = x["arg"]
        if op in BRANCHES:
            arg = str(index.get(int(arg), arg))
        elif arg.startswith("#"):
            arg = ""
        cases = tuple((k, index.get(t, t)) for k, t in x["cases"])
        out.append((op, arg, INDY_INDEX.sub("InvokeDynamic ", rename(x["ref"])), cases))
    return out


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


def mrow(kind, name, params, status, reason, n, nulls="-", m3desc=None, static=False):
    return {"kind": kind, "name": name, "params": params, "desc": "(" + "".join(params) + ")", "status": status,
            "reason": reason, "cases": n, "nulls": nulls, "m3desc": m3desc, "static": static, "domains": {},
            "hash": "-"}


class Atom:
    def __init__(self, donor, m3):
        self.donor, self.m3 = donor, m3
        self.short = short(donor)
        self.simple = donor.rsplit(".", 1)[1]
        self.pattern, self.pattern_source = "-", "-"
        self.status, self.scope, self.reason = "-", "-", "-"
        self.members = []
        self.constants = 0
        self.instances = []
        self.recipe = None          # (name, short params, level, m3 descriptor)
        self.recipe_domains = {}
        self.api_diff = []
        self.test_class = "-"
        self.perf = None
        self.perf_code = "-"
        self.perturbation = "-"
        self.contract = None
        self.native_receiver = set()

    def tested(self):
        return [m for m in self.members if m["status"] == "TESTED"]

    def total_cases(self):
        return sum(m["cases"] for m in self.tested())


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
        self.proxied = {a.short: p for a in self.atoms if a.short in self.candidates
                        for p in [self.proxy_methods(a)] if p}
        for a in self.atoms:
            self.classify(a)
        for a in self.atoms:
            self.domains(a)

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
                    self.by_short[s].recipe = (r[0], r[1], level, r[3])
            self.values = {s for s in self.candidates if self.by_short[s].recipe or self.by_short[s].instances}
            if self.values == allowed:
                break
            level += 1
        for s in sorted(self.values):
            a = self.by_short[s]
            for _, (name, sp, deps, m3desc) in self.recipe_options(a, frozenset(self.values)):
                if not any(self.depends(d, s) for d in deps):
                    a.recipe = (name, sp, a.recipe[2] if a.recipe else 0, m3desc)
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

    def proxy_methods(self, a):
        """{abstract method: short return token} of an interface whose receivers are test proxies: it has default
        methods, no static instances, and every abstract method returns a value the tests can build."""
        t = self.api(a)
        methods = [x for x in t["members"] if x["kind"] == "method" and "static" not in x["mods"]]
        if t["kind"] != "interface" or a.instances or not any("default" in x["mods"] for x in methods):
            return None
        out = {}
        for x in methods:
            if "abstract" not in x["mods"]:
                continue
            ret = self.m.short_token(tokens(x["desc"])[1])
            if x["name"] in out or not (ret == "V" or self.param_kind(ret) in RECIPE_KINDS):
                return None
            out[x["name"]] = ret
        return out

    def param_kind(self, tok, allowed=None):
        """PRIM, STRING, SELF (Object), BOXED, SUPPLIER, RUNNABLE, URL, ENUM, VALUE, NULL or ARRAY_<kind>. With
        allowed (recipe selection), a candidate value type outside allowed is PENDING: its recipe comes later."""
        if tok.startswith("["):
            return "ARRAY_" + self.param_kind(tok[1:], allowed)
        if not tok.startswith("L"):
            return "PRIM"
        b = binary(tok[1:-1])
        if b in ("java.lang.String", "java.lang.CharSequence"):
            return "STRING"
        if b == "java.lang.Object":
            return "SELF"
        if b in BOXED:
            return "BOXED"
        if b in JDK_KINDS:
            return JDK_KINDS[b]
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
        return options[0][1] if options else None

    def recipe_options(self, a, allowed):
        """Constructors and static factories of a candidate value type usable as recipes, best first: fewest null
        parameters, most enum parameters, fewest value parameters, fewest arrays, most parameters, constructors
        first. A null parameter is unusable when the body null-checks that parameter (Lombok @NonNull or
        requireNonNull); when its type needs natives, the type's instances need natives (a.native_receiver)."""
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
            checked = self.null_checked_params(a, x)
            rejected = [binary(shorts[i][1:-1]) for i, k in enumerate(kinds) if k == "NULL" and i in checked]
            if rejected:
                a.native_receiver |= {r for r in rejected if self.is_native_type(r)}
                continue
            sp = "(" + "".join(shorts) + ")"
            name = "<init>" if ctor else x["name"]
            options.append(((kinds.count("NULL"), -kinds.count("ENUM"), sum(k.endswith("VALUE") for k in kinds),
                             sum(k.startswith("ARRAY") for k in kinds), -len(params), 0 if ctor else 1, name, sp),
                            (name, sp, self.value_deps(sp), x["desc"])))
        return sorted(options)

    def null_checked_params(self, a, x):
        """Indexes of the parameters whose null the member's own body rejects."""
        key = self.m.resolve(*self.body_key(a, x))
        if not key:
            return set()
        slots = param_slots(x["desc"], "static" in x["mods"])
        checked = null_checked_slots(self.m.bodies[key])
        return {i for i, s in enumerate(slots) if s in checked}

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

    # ---- contracts (atoms without a donor class)
    def contract(self, a):
        """SNEAKY_THROW: a public static (Throwable)RuntimeException that never returns normally (Lombok's
        sneakyThrow semantics, which M3SneakyThrow carries for delomboked code)."""
        for x in sorted(self.m.m3_api[a.m3]["members"], key=lambda x: (x["name"], x["desc"])):
            if x["kind"] != "method" or not {"public", "static"} <= x["mods"] or x["desc"] != SNEAKY_DESC:
                continue
            key = self.m.resolve(*self.body_key(a, x))
            ops = {i["op"] for i in self.m.bodies[key]["insns"]} if key else set()
            if "athrow" in ops and "areturn" not in ops:
                return "SNEAKY_THROW", x
        return None

    def classify(self, a):
        t = self.m.m3_api.get(a.m3)
        if a.donor in self.m.oracle_missing or a.donor not in self.m.oracle_api:
            c = self.contract(a) if t else None
            if c:
                a.contract = (c[0], c[1]["name"])
                a.status, a.scope = "TESTED", "CONTRACT"
                a.reason = "no donor class (added in leaf 1): plain JUnit contract %s, no oracle" % c[0]
                a.members = [mrow("STATIC_METHOD", c[1]["name"], tuple(tokens(c[1]["desc"])[0]), "TESTED",
                                  "contract " + c[0], SNEAKY_CASES, m3desc=c[1]["desc"], static=True)]
            else:
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
        receivers = a.short in self.enums or bool(a.instances) or bool(a.recipe) or a.short in self.proxied
        seen = set()
        for x in sorted(t["members"], key=lambda x: (x["kind"], x["name"], x["desc"])):
            row = self.member(a, t, x, receivers)
            if row and (row["kind"], row["name"], row["params"]) not in seen:
                seen.add((row["kind"], row["name"], row["params"]))
                a.members.append(row)
        self.perturb(a)
        a.members.sort(key=lambda r: (KIND_ORDER.index(r["kind"]), r["name"], r["desc"]))
        self.atom_status(a, t)

    def member(self, a, t, x, receivers):
        if "public" not in x["mods"] or x["kind"] == "clinit":
            return None
        static = "static" in x["mods"]
        if x["kind"] == "field":
            if a.short in self.enums and x["name"] in self.m.enum_constants.get(a.m3, ()):
                return None
            kind, params, name = ("STATIC_FIELD" if static else "FIELD"), (), x["name"]
        else:
            params = tuple(self.m.short_token(p) for p in tokens(x["desc"])[0])
            if x["kind"] == "ctor":
                if t["kind"] != "class" or "abstract" in t["mods"] or a.short in self.enums:
                    return None
                kind, name = "CTOR", "<init>"
            else:
                kind, name = ("STATIC_METHOD" if static else "METHOD"), x["name"]
        nulls = ",".join(p for p in params if self.param_kind(p) in ("NULL", "ARRAY_NULL"))
        status, reason = self.member_status(a, t, x, kind, receivers)
        n = cases(kind, len(params), a.constants) if status == "TESTED" else 0
        row = mrow(kind, name, params, status, reason, n, nulls or "-", x["desc"], static)
        if kind == "METHOD" and name == "hashCode" and not params:
            row["hash"] = "EXACT" if self.identity_free(a.m3, 0) else "PINNED"
        return row

    def member_status(self, a, t, x, kind, receivers):
        if self.touches(a, x):
            return "BLOCKED_NATIVES", "native method" if "native" in x["mods"] else "reaches native code"
        if self.nondeterministic(a, x):
            return "EXCLUDED", "nondeterministic result (clock or random)"
        if t["kind"] == "interface" and "abstract" in x["mods"] and kind == "METHOD" and not a.instances:
            return "EXCLUDED", ("abstract: the test's proxy implements it" if a.short in self.proxied
                                else "abstract interface method (no implementation to compare)")
        if kind in ("METHOD", "FIELD") and not receivers:
            native = a.pattern in NATIVE_PATTERNS or bool(a.native_receiver)
            reason = ("instances need native handles" if a.pattern in NATIVE_PATTERNS
                      else "instances need a native " + ",".join(sorted(a.native_receiver)) if a.native_receiver
                      else "no receiver without natives")
            return ("BLOCKED_NATIVES" if native else "EXCLUDED"), reason
        return "TESTED", "-"

    def identity_free(self, cls, depth):
        """True when the hashCode of a module value class mixes no identity hash: its instance fields (and its
        superclasses' within the module) are primitives, Strings, boxed values, primitive arrays or such classes."""
        if depth > MAX_DOMAIN_DEPTH:
            return False
        c, fields = cls, []
        while c in self.m.classes:
            fields += self.m.fields.get(c, [])
            c = self.m.classes[c]
        return all(self.stable_type(desc, depth) for _, desc in fields)

    def stable_type(self, desc, depth):
        base = desc.lstrip("[")
        if len(base) == 1 or desc in STABLE_FIELD_TYPES:
            return True
        if desc.startswith("[") or not base.startswith("L"):
            return False
        cls = binary(base[1:-1])
        overrides = (cls, "hashCode", "()I") in self.m.bodies
        return cls in self.m.classes and overrides and self.identity_free(cls, depth + 1)

    # ---- equals / hashCode perturbation
    def field_mapping(self, a, x):
        """Fields assigned by a constructor that stores each parameter into a distinct instance field and does
        nothing else but call Object.<init> and null-check parameters: [field name per parameter], else None."""
        body = self.m.bodies.get((a.m3, "<init>", x["desc"]))
        if body is None or self.m.classes.get(a.m3) is not None:
            return None
        ins, slots, assigned = without_asserts(body["insns"]), param_slots(x["desc"], False), {}
        for i in range(len(ins) - 2):
            ls = load_slot(ins[i + 1])
            if ins[i]["op"] == "aload_0" and ls and ins[i + 2]["op"] == "putfield":
                cls, name, _ = ref_target(ins[i + 2], a.m3)
                if cls == a.m3:
                    assigned[ls[1]] = name
        puts = sum(1 for y in ins if y["op"] in ("putfield", "putstatic"))
        calls = [y["ref"] for y in ins if y["op"].startswith("invoke")]
        clean = calls.count("Method " + OBJECT_INIT) == 1 and all(
            c == "Method " + OBJECT_INIT or NPE_INIT in c for c in calls)
        own = [f for f, _ in self.m.fields.get(a.m3, [])]
        names = [assigned.get(s) for s in slots]
        if not clean or puts != len(slots) or None in names or sorted(names) != sorted(own):
            return None
        return names if len(set(names)) == len(names) else None

    def mapping_failure(self, a, x):
        """Why a public constructor is not field-mapping (ledger text)."""
        sup = self.m.classes.get(a.m3)
        if sup is not None:
            return "fields in superclass " + short(self.m.m3_to_donor.get(sup, DONOR_ROOT + sup))
        body = self.m.bodies.get((a.m3, "<init>", x["desc"]), {"insns": []})
        calls = [ref_target(y, a.m3) for y in without_asserts(body["insns"])
                 if y["op"].startswith("invoke") and y["op"] != "invokedynamic"]
        if any(c[0] == a.m3 and c[1] == "<init>" for c in calls):
            return "constructors delegate to this(...)"
        other = sorted({c[0].rsplit(".", 1)[-1] + "." + c[1] for c in calls
                        if c[1] != "<init>" or c[0] not in ("java.lang.Object", "java.lang.NullPointerException")})
        return "constructor calls " + ",".join(other) if other else "constructor stores computed values"

    def perturb(self, a):
        """Adds the PERTURB row of a value atom whose equals or hashCode is tested: copies with exactly one field
        changed, built through the field-mapping constructor with the most non-null parameters."""
        declared = [m for m in a.members if m["status"] == "TESTED" and m["kind"] == "METHOD"
                    and (m["name"], m["params"]) in (("equals", EQUALS_PARAMS), ("hashCode", ()))]
        if not declared:
            return
        best, why = None, set()
        for x in sorted(self.api(a)["members"], key=lambda x: x["desc"]):
            if x["kind"] != "ctor" or "public" not in x["mods"] or self.touches(a, x):
                continue
            fields = self.field_mapping(a, x)
            why |= set() if fields else {self.mapping_failure(a, x)}
            shorts = tuple(self.m.short_token(p) for p in tokens(x["desc"])[0])
            kinds = [self.param_kind(s) for s in shorts]
            checked = self.null_checked_params(a, x)
            if not fields or not all(k in RECIPE_KINDS for k in kinds) or any(
                    k == "NULL" and i in checked for i, k in enumerate(kinds)):
                continue
            positions = [i for i, k in enumerate(kinds) if k != "NULL"]
            score = (-len(positions), x["desc"])
            if positions and (best is None or score < best[0]):
                best = (score, x, shorts, fields, positions)
        if best is None:
            a.perturbation = "NONE: no field-mapping public constructor (%s)" % "; ".join(sorted(why))
            return
        _, x, shorts, fields, positions = best
        skipped = [fields[i] for i in range(len(fields)) if i not in positions]
        a.perturbation = "%d/%d fields via <init>(%s)" % (len(positions), len(fields), "".join(shorts))
        reason = "equals and hashCode on copies with exactly one field changed: %d of %d fields (%s)%s" % (
            len(positions), len(fields), ",".join(fields[i] for i in positions),
            "; native-typed, always null: " + ",".join(skipped) if skipped else "")
        a.members.append(mrow("PERTURB", "<init>", shorts, "TESTED", reason, len(positions) * PERTURB_ROUNDS,
                              m3desc=x["desc"]))

    # ---- int domains
    def domains(self, a):
        for m in a.members:
            if m["status"] == "TESTED" and m["m3desc"] and m["kind"] in ("CTOR", "STATIC_METHOD", "METHOD",
                                                                          "PERTURB"):
                name = "<init>" if m["kind"] in ("CTOR", "PERTURB") else m["name"]
                m["domains"] = self.int_domains(a.m3, name, m["m3desc"], m["static"])
        if a.recipe:
            a.recipe_domains = self.int_domains(a.m3, a.recipe[0], a.recipe[3], a.recipe[0] != "<init>")

    def int_domains(self, cls, name, desc, static):
        key = self.m.resolve(cls, name, desc)
        slots = param_slots(desc, static)
        out = {}
        for p, tok in enumerate(tokens(desc)[0]):
            d = self.int_domain(key, slots[p], 0) if key and tok == "I" else None
            if d:
                out[p] = d
        return out

    def int_domain(self, key, slot, depth):
        """(label, values) when the body uses an int slot as an enum ordinal (values-array index) or a switch code
        (cases with their own target), directly or through a module static method taking that int."""
        body = self.m.bodies.get(key)
        if not body or depth > MAX_DOMAIN_DEPTH:
            return None
        ins = body["insns"]
        for i in range(len(ins) - 1):
            if load_slot(ins[i]) != ("i", slot):
                continue
            nxt = ins[i + 1]
            enum = self.values_array(ins[i - 1], key[0]) if i and nxt["op"] == "aaload" else None
            if enum:
                return "ORDINAL " + short(self.m.m3_to_donor[enum]), list(range(len(self.m.enum_constants[enum])))
            if nxt["op"] in SWITCHES:
                default = dict(nxt["cases"]).get("default")
                keys = sorted(int(k) for k, t in nxt["cases"] if k != "default" and t != default)
                return ("SWITCH " + short(self.m.m3_to_donor.get(key[0], key[0])) + "." + key[1], keys) if keys \
                    else None
            if nxt["op"] == "invokestatic":
                cls, name, desc = ref_target(nxt, key[0])
                callee = self.m.resolve(cls, name, desc) if desc.startswith("(I)") else None
                found = self.int_domain(callee, 0, depth + 1) if callee else None
                if found:
                    return found
        return None

    def values_array(self, insn, owner):
        """The enum class whose values array an instruction loads (getstatic E._values or invokestatic E.values)."""
        if insn["op"] not in ("getstatic", "invokestatic") or not insn["ref"]:
            return None
        _, _, desc = ref_target(insn, owner)
        arr = desc[desc.index(")") + 1:] if desc.startswith("(") else desc
        cls = binary(arr[2:-1]) if arr.startswith("[L") else None
        return cls if cls in self.m.enum_constants and cls in self.m.m3_to_donor else None

    def atom_status(self, a, t):
        tested = a.tested()
        blocked = [m for m in a.members if m["status"] == "BLOCKED_NATIVES"]
        runnable = a.pattern in RUNNABLE or a.pattern == "INTERFACE"
        native = a.pattern in NATIVE_PATTERNS
        if tested:
            a.status = "TESTED"
            a.scope = "FULL" if runnable else "STATIC_PURE"
            a.reason = "-" if not blocked else "%d members reach natives (leaf 2)" % len(blocked)
        elif not a.members and "abstract" in t["mods"] and t["kind"] == "class":
            ctor = any(x["kind"] == "ctor" and "public" in x["mods"] for x in t["members"])
            a.status = "BLOCKED_NATIVES" if native else "NOT_APPLICABLE"
            a.reason = "abstract class, no public members%s%s" % (
                " besides constructors" if ctor else "", "; instances are native subclasses (leaf 2)" if native else "")
        elif native or blocked:
            a.status, a.reason = "BLOCKED_NATIVES", "every member needs natives (leaf 2)"
        else:
            a.status = "NOT_APPLICABLE"
            abstract_only = t["kind"] == "interface" and all(m["kind"] == "METHOD" for m in a.members)
            a.reason = ("interface with abstract methods only" if a.members and abstract_only
                        else "no public members runnable without an instance" if a.members else "no public members")

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
            kind, name, params = m["kind"], m["name"], m["params"]
            plain = not GETTERISH.match(name) and name not in OBJECT_METHODS
            order = [kind == "METHOD" and bool(params) and plain, kind == "METHOD" and plain,
                     kind == "STATIC_METHOD" and bool(params), kind == "METHOD" and name == "equals",
                     kind == "METHOD" and name.startswith("with"), kind == "CTOR" and bool(params),
                     kind == "METHOD", kind == "STATIC_METHOD", kind == "FIELD", True]
            return (order.index(True), name, m["desc"])
        tested = [m for m in a.tested() if m["kind"] != "PERTURB"]
        return min(tested, key=rank) if tested else None

    def code_identity(self, a, m):
        """SAME when the perf member's body and its module callees are the same bytecode on both sides."""
        if m["kind"] in ("FIELD", "STATIC_FIELD"):
            return "SAME"
        name = "<init>" if m["kind"] == "CTOR" else m["name"]
        key = self.m.resolve(a.m3, name, m["m3desc"])
        return "SAME" if key and self.m.same_closure(key) else "DIFF"

    def oracle_variant(self, a):
        v = self.m.mr_variants.get(a.donor)
        if not v:
            return "-", True
        sel = self.m.mr_select.get(a.donor)
        match = bool(sel) and v == "versions/" + sel["selected"]
        return "%s; M3 source %s (MR_SELECT): %s" % (v, sel["source"] if sel else "base", "MATCH" if match
                                                     else "MISMATCH"), match


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
    ORDER = ("KIND", "SIDE", "PATTERN", "CODE", "ATOM", "NAME", "DESC", "TOKEN", "CASES", "HANDLE")

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
    for m in a.tested():
        if m["kind"] == "PERTURB":
            lines.append("                .perturb(%s)" % desc_const(c, m["params"]))
        else:
            lines.append("                .member(%s, %s, %s)" % (kind_const(c, m["kind"]), name_const(c, m["name"]),
                                                                  desc_const(c, m["params"])))
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
            " *          Identity hashes are pinned (-XX:hashCode=2), so every hashCode compares exactly.",
            " * @modules jdk.m3.graphics", " * @run junit/othervm %s %s" % (HASH_PINNED_OPTIONS, cls), " */", "",
            "import org.junit.jupiter.api.Test;", "", "public class %s {" % cls]
    return "\n".join(head + c.lines() + [""] + body[:-1] + ["}", ""])


def render_contract_test(atoms):
    c = Constants()
    body = []
    for a in atoms:
        pattern, name = a.contract
        atom = c.get("ATOM", "String", "A_" + snake(a.simple), jstr(a.m3))
        cases_name = c.get("CASES", "int", "CASES_" + snake(a.simple), str(a.total_cases()), unique=a.short)
        body += ["    /** %s %s.%s: same throwable rethrown, never returns, null: NullPointerException. */" % (
                     pattern, a.m3.rsplit(".", 1)[1], name), "    @Test",
                 "    void %s() {" % (a.simple[0].lower() + a.simple[1:]),
                 "        M3GraphicsAtomsContracts.sneakyThrow(%s, %s, %s);" % (atom, name_const(c, name), cases_name),
                 "    }", ""]
    summary = " *          tests of jdk.m3.graphics atoms without a donor class (added in leaf 1), %d atoms; no oracle."
    head = [JAVA_HEADER, "/*", " * @test",
            " * @summary Generated by %s; do not edit. Plain JUnit contract" % TOOL, summary % len(atoms),
            " * @modules jdk.m3.graphics", " * @run junit M3GraphicsAtomsContractTest", " */", "",
            "import org.junit.jupiter.api.Test;", "", "public class M3GraphicsAtomsContractTest {"]
    return "\n".join(head + c.lines() + [""] + body[:-1] + ["}", ""])


def render_recipes(gen):
    c = Constants()
    entries, only, proxies = [], [], []
    for a in sorted(gen.atoms, key=lambda a: a.short):
        if a.short in gen.values and a.recipe:
            params = tokens(a.recipe[1] + "V")[0]
            entries.append("            Map.entry(%s, new Recipe(%s, %s))" % (
                atom_const(c, a), name_const(c, a.recipe[0]), desc_const(c, params)))
        elif a.short in gen.values:
            only.append(atom_const(c, a))
        if a.short in gen.proxied:
            methods = ", ".join("%s, %s" % (name_const(c, n), c.get("TOKEN", "String", "T_" + token_name(t), jstr(t)))
                                for n, t in sorted(gen.proxied[a.short].items()))
            proxies.append("Map.entry(%s, Map.of(%s))" % (atom_const(c, a), methods))
    lines = [JAVA_HEADER, "import java.util.Map;", "import java.util.Optional;", "import java.util.Set;", "",
             "/**",
             " * Generated by %s; do not edit. How the jdk.m3.graphics atom" % TOOL,
             " * tests build a value of a donor value type: a recipe, the public constructor ({@code <init>}) or static",
             " * factory whose parameters are primitives, Strings, enums, arrays of those, boxed values, or null for",
             " * types without a native-free value (a parameter its body null-checks never gets null), chosen by the",
             " * generator: fewest null parameters, most enums, fewest values, fewest arrays, most parameters,",
             " * constructors first. INSTANCE_ONLY lists the value types built from their static instances only;",
             " * PROXIED maps an interface whose receivers are test proxies to the token each abstract method returns.",
             " */", "final class M3GraphicsAtomsRecipes {",
             "    /** A constructor ({@code <init>}) or static factory and its parameter descriptor. */",
             "    record Recipe(String name, String descriptor) {}", ""]
    lines += c.lines()
    lines += ["    private static final Map<String, Recipe> RECIPES = Map.ofEntries("]
    lines += [e + ("," if i < len(entries) - 1 else ");") for i, e in enumerate(entries)]
    lines += ["    static final Set<String> INSTANCE_ONLY = Set.of(%s);" % ", ".join(only)]
    lines += ["    static final Map<String, Map<String, String>> PROXIED = Map.ofEntries(" + ("" if proxies else ");")]
    lines += ["            " + p + ("," if i < len(proxies) - 1 else ");") for i, p in enumerate(proxies)]
    lines += ["", "    private M3GraphicsAtomsRecipes() {}", "",
              "    /** The recipe of a donor value type (short donor name). */",
              "    static Optional<Recipe> recipe(String type) {", "        return Optional.ofNullable(RECIPES.get(type));",
              "    }", "}", ""]
    return "\n".join(lines)


def domain_entries(gen):
    """{member key#position: (label, values)} of every tested member and recipe with a domain."""
    out = {}
    for a in gen.atoms:
        for m in a.tested():
            name = "<init>" if m["kind"] in ("CTOR", "PERTURB") else m["name"]
            for p, d in m["domains"].items():
                out["%s.%s%s#%d" % (a.short, name, m["desc"], p)] = d
        if a.recipe and a.short in gen.values:
            for p, d in a.recipe_domains.items():
                out["%s.%s%s#%d" % (a.short, a.recipe[0], a.recipe[1], p)] = d
    return out


def render_domains(gen):
    entries = sorted(domain_entries(gen).items())
    lines = [JAVA_HEADER, "import java.util.Map;", "import java.util.Optional;", "import java.util.stream.IntStream;",
             "", "/**", " * Generated by %s; do not edit. Int parameters" % TOOL,
             " * of tested members and recipes that the member's bytecode uses as an enum ordinal (an index into the",
             " * enum's values) or a switch code (the cases with their own target): the key is the member key",
             " * (atom.name(descriptor)) and the parameter position. M3GraphicsAtomsValues draws such a parameter from",
             " * its domain, except in escape rounds that keep the out-of-domain path covered.", " */",
             "final class M3GraphicsAtomsDomains {",
             "    private static final Map<String, int[]> DOMAINS = Map.ofEntries(" + ("" if entries else ");")]
    for i, (key, (label, values)) in enumerate(entries):
        end = ")," if i < len(entries) - 1 else "));"
        parts = literal_parts(jstr(key))
        lines += ["            // %s" % label, "            Map.entry(" + parts[0]]
        lines += ["                    " + p for p in parts[1:]]
        lines[-1] += ","
        if label.startswith("ORDINAL") and values == list(range(len(values))):
            lines.append("                    range(%d)%s" % (len(values), end))
            continue
        chunks = [values[k:k + INTS_PER_LINE] for k in range(0, len(values), INTS_PER_LINE)]
        body = [", ".join(str(v) for v in ch) for ch in chunks]
        lines += ["                    of(" + body[0] + ("," if len(body) > 1 else ")" + end)]
        lines += ["                            " + b + ("," if j < len(body) - 1 else ")" + end)
                  for j, b in enumerate(body[1:], 1)]
    lines += ["", "    private M3GraphicsAtomsDomains() {}", "",
              "    /** The domain of parameter {@code position} of a member key, if its bytecode gives one. */",
              "    static Optional<int[]> domain(String key, int position) {",
              "        return Optional.ofNullable(DOMAINS.get(key + \"#\" + position));", "    }", "",
              "    private static int[] range(int n) {", "        return IntStream.range(0, n).toArray();", "    }", "",
              "    private static int[] of(int... values) {", "        return values;", "    }", "}", ""]
    return "\n".join(lines)


def literal_parts(literal):
    """A key literal for 'Map.entry(' lines: whole, or cut after ';' with '+ ' continuations."""
    if len("            Map.entry(" + literal + ",") <= MAX_LINE:
        return [literal]
    parts = split_literal(literal)
    return [parts[0]] + ["+ " + p for p in parts[1:]]


def render_perf_group(cls, atoms):
    c = Constants()
    c.get("SIDE", "M3GraphicsAtomsOracle.Side", "M3", "M3GraphicsAtomsOracle.Side.M3")
    c.get("SIDE", "M3GraphicsAtomsOracle.Side", "ORACLE", "M3GraphicsAtomsOracle.Side.ORACLE")
    handles, rows_ = [], []
    for i, a in enumerate(atoms, 1):
        kind, name, params = a.perf["kind"], a.perf["name"], a.perf["params"]
        args = "%s, %s, %s, %s" % (atom_const(c, a), kind_const(c, kind), name_const(c, name), desc_const(c, params))
        pattern = c.get("PATTERN", "String", "P_" + snake(a.pattern), jstr(a.pattern))
        code = c.get("CODE", "String", "C_" + a.perf_code, jstr(a.perf_code))
        for side, prefix in (("M3", "M"), ("ORACLE", "O")):
            handles += const_line("MethodHandle", "%s%02d" % (prefix, i), "h(%s, %s)" % (side, args))
        rows_ += ["        p.row(%s, %s, %s, %s, %s, %s," % (atom_const(c, a), pattern, kind_const(c, kind),
                                                      name_const(c, name), desc_const(c, params), code)]
        for prefix, end in (("M", ","), ("O", ");")):
            rows_ += ["            (x, n) -> { for (int k = 0; k < n; k++) for (Object[] a : x) try { "
                      "consume((Object) %s%02d.invokeExact(a)); }" % (prefix, i),
                      "                    catch (Throwable t) { consume(t); } return n; }%s" % end]
    first, last = atoms[0].short, atoms[-1].short
    lines = [JAVA_HEADER, "import java.lang.invoke.MethodHandle;", "", "/**",
             " * Generated by %s; do not edit. Perf rows of %d" % (TOOL, len(atoms)),
             " * jdk.m3.graphics atoms, %s .. %s:" % (first, last),
             " * per atom a static final MethodHandle per side (Mnn, Onn) and one timed loop per side over the same",
             " * calls; the loop calls the constant handle directly and hands every result to the blackhole",
             " * M3GraphicsAtomsPerfSupport.consume. C_SAME / C_DIFF: the member's bytecode and its module callees",
             " * are the same on both sides (modulo the rename) or not.", " */",
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
             " *          final MethodHandles. Per row: warm-up until both loops are compiled, then calibration of the",
             " *          passes to 2 ms rounds, then 11 rounds alternating which side runs first; ns/op per side is the",
             " *          median round, ratio = M3 / oracle, spread = interquartile range of the round ratios over their",
             " *          median. No re-measuring. Guard: no atom slower than its oracle by more than 1.5x",
             " *          (-Dm3.graphics.perf.slack=<factor>). C2 only and foreground compilation",
             " *          (-XX:-TieredCompilation -Xbatch) keep the two sides' compiled loops alike; the blackhole keeps",
             " *          every result. Rows whose code is the same on both sides (SAME) give the harness's own spread.",
             " *          Without the oracle: PERF_SKIPPED.",
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


def domain_text(m):
    return "; ".join("p%d %s %s" % (p, label, compact(values)) for p, (label, values) in sorted(m["domains"].items())) \
        or "-"


def compact(values):
    if values == list(range(len(values))):
        return "0..%d" % (len(values) - 1)
    return "{" + ",".join(str(v) for v in values) + "}"


def generate(gen):
    out = {}
    tested = sorted((a for a in gen.atoms if a.tested() and a.scope != "CONTRACT"),
                    key=lambda a: (PATTERN_RANK.get(a.pattern, 99), a.short))
    taken, methods = set(), {}
    for a in tested:
        methods[a.short] = method_name(a, taken)
        a.perf = gen.choose_perf(a)
        a.perf_code = gen.code_identity(a, a.perf)
    test_files = split(tested, lambda cls, at: render_test(cls, at, methods), "M3GraphicsAtomsTest")
    for i, group in enumerate(test_files, 1):
        cls = "M3GraphicsAtomsTest%02d" % i
        out["%s/%s.java" % (TEST_DIR, cls)] = render_test(cls, group, methods)
        for a in group:
            a.test_class = cls
    contracts = sorted((a for a in gen.atoms if a.scope == "CONTRACT"), key=lambda a: a.short)
    if contracts:
        out["%s/M3GraphicsAtomsContractTest.java" % TEST_DIR] = render_contract_test(contracts)
        for a in contracts:
            a.test_class = "M3GraphicsAtomsContractTest"
    perf_files = split(tested, render_perf_group, "M3GraphicsAtomsPerfGroup")
    groups = []
    for i, group in enumerate(perf_files, 1):
        cls = "M3GraphicsAtomsPerfGroup%02d" % i
        groups.append(cls)
        out["%s/%s.java" % (TEST_DIR, cls)] = render_perf_group(cls, group)
    out["%s/M3GraphicsAtomsPerf.java" % TEST_DIR] = render_perf_main(groups, len(tested))
    out["%s/M3GraphicsAtomsRecipes.java" % TEST_DIR] = render_recipes(gen)
    out["%s/M3GraphicsAtomsDomains.java" % TEST_DIR] = render_domains(gen)
    out["%s/ATOM_TESTS.tsv" % LEAF] = tsv(
        ("atom", "donor", "pattern", "pattern_source", "status", "scope", "reason", "members_tested",
         "members_blocked_natives", "members_excluded", "cases", "receivers", "test_class", "perf_member",
         "perf_code", "perturbation", "api_diff", "oracle_variant"),
        [(a.m3, a.donor, a.pattern, a.pattern_source, a.status, a.scope, a.reason, len(a.tested()),
          sum(m["status"] == "BLOCKED_NATIVES" for m in a.members), sum(m["status"] == "EXCLUDED" for m in a.members),
          a.total_cases(), receivers(gen, a), a.test_class, perf_name(a), a.perf_code if a.perf else "-",
          a.perturbation, len(a.api_diff), gen.oracle_variant(a)[0])
         for a in sorted(gen.atoms, key=lambda a: a.m3)])
    out["%s/ATOM_MEMBERS.tsv" % LEAF] = tsv(
        ("atom", "kind", "member", "status", "reason", "cases", "null_params", "int_domains", "hash"),
        [(a.m3, m["kind"], m["name"] + m["desc"], m["status"], m["reason"], m["cases"], m["nulls"], domain_text(m),
          m["hash"]) for a in sorted(gen.atoms, key=lambda a: a.m3) for m in a.members])
    return out


def receivers(gen, a):
    if a.short in gen.enums:
        return "enum constants"
    parts = (["static " + ",".join(a.instances)] if a.instances else []) + (
        ["recipe L%d %s%s" % (gen.recipe_depth(a.short), a.recipe[0], a.recipe[1])] if a.recipe else []) + (
        ["proxy (%s)" % ",".join(sorted(gen.proxied[a.short]))] if a.short in gen.proxied else [])
    return "; ".join(parts) or "-"


def perf_name(a):
    return "%s %s%s" % (a.perf["kind"], a.perf["name"], a.perf["desc"]) if a.perf else "-"


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
    perturbed = [a for a in tested if any(m["kind"] == "PERTURB" for m in a.members)]
    print("atoms=%d tested=%d blocked_natives=%d not_applicable=%d members_tested=%d cases=%d files=%d "
          "max_lines=%d api_diffs=%d perturbed=%d domains=%d proxied=%d contracts=%d perf_same=%d changed=%d "
          "stale=%d" % (
              len(gen.atoms), counts["TESTED"], counts["BLOCKED_NATIVES"], counts["NOT_APPLICABLE"],
              sum(len(a.tested()) for a in tested), sum(a.total_cases() for a in tested), len(out),
              max(t.count("\n") for r, t in out.items() if r.endswith(".java")),
              sum(len(a.api_diff) for a in gen.atoms), len(perturbed), len(domain_entries(gen)), len(gen.proxied),
              sum(a.scope == "CONTRACT" for a in gen.atoms), sum(a.perf_code == "SAME" for a in tested if a.perf),
              len(differ), len(stale)))
    mismatches = 0
    for a in gen.atoms:
        text, match = gen.oracle_variant(a)
        if text != "-":
            print("MR_VARIANT\t%s\t%s" % (a.m3, text))
            mismatches += 0 if match else 1
        for d in a.api_diff:
            print("API_DIFF\t%s\t%s" % (a.m3, "\t".join(d)))
    for d in gen.m.rename_mismatches:
        print("RENAME_RULE_MISMATCH\t%s\t%s\t%s" % (d, gen.m.donor_to_m3[d], rename_rule(d)))
    if mismatches or gen.m.rename_mismatches:
        return 1
    if check and (differ or stale):
        print("NOT_FIXED_POINT\t" + " ".join(differ + stale))
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
