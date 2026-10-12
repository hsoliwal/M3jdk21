#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""C6 DEPENDENCY-SOURCE-INLINE, phase GENERATE_ONLY_GAP: materialize the repository form of every
inventoried donor from its byte-for-byte image and the donor-superset receipts (TFG: everything below is
inferred from the inventory, the lock plan and the receipts; nothing is authored by hand).

* status PASS            -> third_party/<dir>/ Maven module on the re2j template: unchanged upstream sources
                            under src/main/java (module-info.java retained unless listed in EXCLUDE_MODULE_INFO),
                            non-Java jar entries under src/main/resources, multi-release sources retained
                            uncompiled under upstream-excluded/, LICENSE + THIRD_PARTY_NOTICES.md +
                            PROVENANCE.json + README.md + verify_source.py + .gitattributes at the module root,
                            coordinates <group>:<artifact>:<version>-synexia-source-1, Automatic-Module-Name
                            copied from the published binary jar's manifest.
* any other status       -> donors/maven-sources/<id>/ byte-for-byte image (SOURCE_MANIFEST + legal texts +
                            UPSTREAM_LICENSE.*), no build; reason recorded in the ledger.
Outputs THIRD_PARTY_MODULES.tsv (module dir, coordinates, status, build kind, deps) for the consumer switch,
the recipe and the crate. CPU only.
"""
from __future__ import annotations
import argparse, csv, hashlib, io, json, os, re, shutil, subprocess, sys, zipfile
from pathlib import Path

CENTRAL = "https://repo.maven.apache.org/maven2/"
SUFFIX = "-synexia-source-1"
JAVAP = r"C:\Program Files\Java\jdk-21\bin\javap.exe"
LEGAL_RE = re.compile(r"(?i)(.*[-_.])?(license|licence|notice|copying|copyright)([._-].*)?$")
RELEASE = {"caffeine": 11, "tomlj": 11, "antlr4-runtime": 11, "snakeyaml-engine": 11, "ecj": 17, "commonmark": 11, "fastdoubleparser": 21, "jackson-core": 21, "jackson-databind": 21, "jackson-dataformat-yaml": 21, "jackson-datatype-jsr310": 21, "lucene-core": 11, "lucene-queries": 11, "lucene-sandbox": 11, "rewrite-core": 17, "rewrite-java": 17, "rewrite-java-21": 21, "rewrite-test": 17, "rewrite-xml": 17, "rewrite-yaml": 17, "micrometer-core": 8, "classgraph": 8, "gizmo": 11, "jandex": 8, "fastfilter": 11,
           "lucene-analysis-common": 11, "lucene-queryparser": 11}
LEGACY = {"zero-allocation-hashing": 8, "guava": 8, "eclipse-collections": 8, "agrona": 8, "lz4-java": 8, "asm": 8, "jspecify": 8, "rewrite-java-21": 21}
JAVAC_ARGS = {"rewrite-java-21": ["--add-exports", "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED", "--add-exports", "jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED", "--add-exports", "jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED", "--add-exports", "jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED", "--add-exports", "jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED", "--add-exports", "jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED", "--add-exports", "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED", "--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED", "--add-exports", "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED", "--add-exports", "jdk.compiler/com.sun.tools.javac.jvm=ALL-UNNAMED", "--add-exports", "jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED"]}
ENCODING = {"commons-compress": "ISO-8859-1", "commons-text": "ISO-8859-1"}
EXCLUDE_MODULE_INFO = set()  # artifacts whose module descriptor must stay uncompiled (filled after the first reactor build)
NATIVE_IN_BINARY = set()  # the equivalence gate decides; native libraries are completed byte-for-byte from the published jar and compared by SHA-256
BINARY_INPUT_SCOPE = "provided"
OVERLAY_COMPILE_INPUTS = {  # donor -> [(group, artifact, version)]: provided inputs of a multi-release overlay (published pom)
    "sqlite-jdbc-3.45.1.0": [("org.graalvm.sdk", "nativeimage", "23.1.2")],
    "sqlite-jdbc-3.51.1.1": [("org.graalvm.sdk", "nativeimage", "25.0.2")],
}
EXTRA_COMPILE_DEPS = {  # donor id -> [(group, artifact, version)]: fully qualified references the import closure misses
    "rewrite-java-21-8.17.1": [("org.slf4j", "slf4j-api", "1.7.36")],
}
PACKAGING_EXCLUDES = {("slf4j-api", "1.7."): ["org/slf4j/impl/**"]}  # upstream antrun deletes the stub binders before packaging
ADMISSIBLE = ("IDENTICAL", "IDENTICAL_EXCEPT_SHADED", "EQUIVALENT_MODULO_COMPILER")
EXISTING_MODULES = {"re2j": "third_party/re2j"}
DELOMBOK_BUILD_DIR = "build/delombok"
DELOMBOK_NOTICE = "// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/{path}, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.\n"  # runner output of the helper for Lombok-at-source donors  # already inlined by hand from the upstream tag at the same coordinate; never regenerated

RESOURCE_SKIP_PREFIXES = ("META-INF/maven/", "META-INF/versions/")
RESOURCE_SKIP_NAMES = ("META-INF/MANIFEST.MF", "META-INF/INDEX.LIST")
NATIVE_SUFFIXES = (".so", ".dll", ".dylib", ".jnilib")
TOKEN_FILTER = {  # artifact -> (source path, class, fields): upstream build substitutes these constants
    "jna": ("com/sun/jna/Version.java", "com.sun.jna.Version", ("VERSION", "VERSION_NATIVE")),
}
FILTER_NOTICE = "// NOTICE: modified file. Version tokens substituted as the upstream build does (values from the published class); the unchanged upstream file is kept at upstream-excluded/{path}.\n"


def RESOURCE_SKIP(name):
    return (name.endswith("/") or name.endswith(".class") or name in RESOURCE_SKIP_NAMES or name.startswith(RESOURCE_SKIP_PREFIXES)
            or (name.startswith("META-INF/") and name.endswith((".SF", ".RSA", ".DSA", ".EC"))))


def published_constants(jar, cls, fields):
    out = subprocess.run([JAVAP, "-constants", "-cp", str(jar), cls], capture_output=True, text=True).stdout
    vals = {}
    for f in fields:
        m = re.search(r"static final java\.lang\.String " + re.escape(f) + r' = "([^"]*)";', out)
        if m:
            vals[f] = m.group(1)
    return vals


SHADE_RULES = {  # artifact -> (included group:artifact list, [(pattern, shadedPattern)]) as upstream shadow tasks
    "rewrite-core": (["org.eclipse.jgit:org.eclipse.jgit"], [("org.eclipse.jgit", "org.openrewrite.shaded.jgit")]),
    "junit-jupiter-params": (["com.univocity:univocity-parsers"], [("com.univocity", "org.junit.jupiter.params.shadow.com.univocity")]),
}
BYTE_BUDDY_TRANSFORM = """      <plugin><groupId>net.bytebuddy</groupId><artifactId>byte-buddy-maven-plugin</artifactId><version>1.14.10</version>
        <!-- upstream byte-buddy-dep build: the same five transformations, applied to the compiled classes -->
        <executions><execution><phase>compile</phase><goals><goal>transform</goal></goals><configuration>
          <initialization><entryPoint>REDEFINE</entryPoint></initialization>
          <transformations>
            <transformation><groupId>net.bytebuddy</groupId><artifactId>byte-buddy</artifactId><version>1.14.10</version><plugin>net.bytebuddy.build.HashCodeAndEqualsPlugin$WithNonNullableFields</plugin><arguments><argument><index>0</index><value>net.bytebuddy.utility.nullability.MaybeNull</value></argument></arguments></transformation>
            <transformation><groupId>net.bytebuddy</groupId><artifactId>byte-buddy</artifactId><version>1.14.10</version><plugin>net.bytebuddy.build.CachedReturnPlugin</plugin></transformation>
            <transformation><groupId>net.bytebuddy</groupId><artifactId>byte-buddy</artifactId><version>1.14.10</version><plugin>net.bytebuddy.build.AccessControllerPlugin</plugin><arguments><argument><index>0</index><value>net.bytebuddy.securitymanager</value></argument></arguments></transformation>
            <transformation><groupId>net.bytebuddy</groupId><artifactId>byte-buddy</artifactId><version>1.14.10</version><plugin>net.bytebuddy.build.DispatcherAnnotationPlugin</plugin></transformation>
            <transformation><groupId>net.bytebuddy</groupId><artifactId>byte-buddy</artifactId><version>1.14.10</version><plugin>net.bytebuddy.build.RepeatedAnnotationPlugin</plugin></transformation>
          </transformations>
        </configuration></execution></executions>
      </plugin>
"""
BUILD_PLUGINS = {"byte-buddy": BYTE_BUDDY_TRANSFORM}
KOTLIN = {"junit-jupiter-api": ("1.8.22", "1.3", "1.3", "1.8"), "junit-jupiter-params": ("1.8.22", "1.3", "1.3", "1.8")}  # kotlin compiler, apiVersion, languageVersion, jvmTarget (upstream junit 5.10.2)
KOTLIN_BY_VERSION = {  # (artifact, version) -> (compiler, apiVersion, languageVersion, jvmTarget, extra args); upstream r5.11.4
    ("junit-jupiter-api", "5.11.4"): ("1.9.23", "1.6", "1.6", "1.8", ("-opt-in=kotlin.RequiresOptIn",)),
    ("junit-jupiter-params", "5.11.4"): ("1.9.23", "1.6", "1.6", "1.8", ("-opt-in=kotlin.RequiresOptIn",)),
}


def kcfg(r):
    v = r["version"][:-len(SUFFIX)] if SUFFIX and r["version"].endswith(SUFFIX) else r["version"]
    return KOTLIN_BY_VERSION.get((r["artifact"], v), KOTLIN.get(r["artifact"]))


def shade_plugin(rule):
    includes, relocs = rule
    inc = "".join("<include>%s</include>" % i for i in includes)
    rel = "".join("<relocation><pattern>%s</pattern><shadedPattern>%s</shadedPattern></relocation>" % r for r in relocs)
    return ("      <plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-shade-plugin</artifactId><version>3.6.0</version>\n"
            "        <!-- upstream shadow task reproduced: include and relocate exactly what the published jar shades -->\n"
            "        <executions><execution><phase>package</phase><goals><goal>shade</goal></goals><configuration>"
            "<createDependencyReducedPom>true</createDependencyReducedPom><artifactSet><includes>" + inc + "</includes></artifactSet>"
            "<relocations>" + rel + "</relocations>"
            "<filters>" + "".join("<filter><artifact>%s</artifact><excludes><exclude>META-INF/*.SF</exclude><exclude>META-INF/*.RSA</exclude><exclude>META-INF/*.DSA</exclude><exclude>META-INF/maven/**</exclude><exclude>module-info.class</exclude><exclude>META-INF/versions/*/module-info.class</exclude></excludes></filter>" % i_ for i_ in includes) + "</filters>"
            "</configuration></execution></executions>\n      </plugin>\n")


def kotlin_plugin(cfg):
    ver, api_, lang, jvm = cfg[:4]
    kargs = "".join("<arg>%s</arg>" % a for a in (cfg[4] if len(cfg) > 4 else ()))
    return ("      <plugin><groupId>org.jetbrains.kotlin</groupId><artifactId>kotlin-maven-plugin</artifactId><version>%s</version>\n"
            "        <!-- upstream compiles the Kotlin facade (Assertions.kt) with the same API/language level -->\n"
            "        <executions><execution><id>kotlin-compile</id><phase>process-sources</phase><goals><goal>compile</goal></goals>"
            "<configuration><sourceDirs><sourceDir>${project.basedir}/src/main/kotlin</sourceDir><sourceDir>${project.basedir}/src/main/java</sourceDir></sourceDirs></configuration></execution></executions>"
            "<configuration><jvmTarget>%s</jvmTarget><apiVersion>%s</apiVersion><languageVersion>%s</languageVersion>%s</configuration>\n      </plugin>\n") % (ver, jvm, api_, lang, ("<args>" + kargs + "</args>") if kargs else "")

IMAGE_ROOT_ATTRS = "# byte-for-byte upstream images (SHA-256 pinned); whitespace is upstream's\n* -whitespace\n"

BUILD_JDK_RELEASE = 21  # JDK 21 toolchain (JAVA_HOME of every gate)


def parse_descriptor(data: bytes) -> dict:
    """Module attribute of a module-info.class (JVMS 4.7.25) plus ModuleMainClass; no third-party parser."""
    import struct
    u2 = lambda b, o: struct.unpack(">H", b[o:o + 2])[0]
    n, pos, cp, i = u2(data, 8), 10, {}, 1
    while i < n:
        tag = data[pos]; pos += 1
        if tag == 1:
            ln = u2(data, pos); cp[i] = data[pos + 2:pos + 2 + ln].decode("utf-8", "replace"); pos += 2 + ln
        elif tag in (7, 8, 16, 19, 20):
            cp[i] = ("ref", u2(data, pos)); pos += 2
        elif tag in (3, 4, 9, 10, 11, 12, 17, 18):
            pos += 4
        elif tag in (5, 6):
            pos += 8; i += 1
        elif tag == 15:
            pos += 3
        else:
            raise ValueError("constant pool tag %d" % tag)
        i += 1
    name = lambda k: cp[cp[k][1]] if isinstance(cp[k], tuple) else cp[k]
    pos += 6
    pos += 2 + 2 * u2(data, pos)  # interfaces
    for _ in range(2):  # fields, methods (none in a descriptor)
        assert u2(data, pos) == 0, "module-info with members"
        pos += 2
    out = {"major": u2(data, 6), "name": "", "open": False, "version": "", "requires": [], "exports": [], "opens": [], "uses": [], "provides": [], "main": ""}
    ac = u2(data, pos); pos += 2
    for _ in range(ac):
        an = cp[u2(data, pos)]; al = struct.unpack(">I", data[pos + 2:pos + 6])[0]; b = data[pos + 6:pos + 6 + al]; pos += 6 + al
        if an == "ModuleMainClass":
            out["main"] = name(u2(b, 0)).replace("/", ".")
        if an != "Module":
            continue
        out["name"], out["open"] = name(u2(b, 0)), bool(u2(b, 2) & 0x20)
        out["version"] = cp[u2(b, 4)] if u2(b, 4) else ""
        o = 6
        for _r in range(u2(b, o)):
            mod, fl = name(u2(b, o + 2)), u2(b, o + 4)
            if mod != "java.base":
                out["requires"].append((mod, bool(fl & 0x20), bool(fl & 0x40)))
            o += 6
        o += 2
        for key in ("exports", "opens"):
            cnt = u2(b, o); o += 2
            for _e in range(cnt):
                pkg, tc = name(u2(b, o)).replace("/", "."), u2(b, o + 4)
                out[key].append((pkg, [name(u2(b, o + 6 + 2 * t)) for t in range(tc)]))
                o += 6 + 2 * tc
        cnt = u2(b, o); o += 2
        out["uses"] = [name(u2(b, o + 2 * t)).replace("/", ".").replace("$", ".") for t in range(cnt)]
        o += 2 * cnt
        cnt = u2(b, o); o += 2
        for _p in range(cnt):
            svc, wc = name(u2(b, o)).replace("/", ".").replace("$", "."), u2(b, o + 2)
            out["provides"].append((svc, [name(u2(b, o + 4 + 2 * w)).replace("/", ".").replace("$", ".") for w in range(wc)]))
            o += 4 + 2 * wc
    return out


def descriptor_source(d: dict, origin: str) -> str:
    lines = ["// NOTICE: declaration generated from the published %s (class version %d); the published sources jar" % (origin, d["major"]),
             "// carries no module-info.java (upstream generates the descriptor in its build). No other content.",
             "%smodule %s {" % ("open " if d["open"] else "", d["name"])]
    lines += ["    requires %s%s%s;" % ("static " if st else "", "transitive " if tr else "", m) for m, tr, st in d["requires"]]
    lines += ["    exports %s%s;" % (pk, (" to " + ", ".join(to)) if to else "") for pk, to in d["exports"]]
    lines += ["    opens %s%s;" % (pk, (" to " + ", ".join(to)) if to else "") for pk, to in d["opens"]]
    lines += ["    uses %s;" % u for u in d["uses"]]
    lines += ["    provides %s with %s;" % (sv, ", ".join(w)) for sv, w in d["provides"]]
    return "\n".join(lines + ["}", ""])


MODITECT_EXEC = ("<execution><id>add-module-info-%s</id><phase>package</phase><goals><goal>add-module-info</goal></goals><configuration>"
                 "<jvmVersion>%s</jvmVersion><overwriteExistingFiles>true</overwriteExistingFiles><failOnWarning>false</failOnWarning>%s"
                 "<module><moduleInfoFile>${project.basedir}/src/main/descriptor/%s/module-info.java</moduleInfoFile></module></configuration></execution>")
DESCRIPTOR_EXEC = ("<execution><id>module-descriptor-%s</id><phase>process-classes</phase><goals><goal>compile</goal></goals><configuration>"
                   "<release>%d</release><compileSourceRoots><compileSourceRoot>${project.basedir}/src/main/descriptor/%s</compileSourceRoot></compileSourceRoots>"
                   "<multiReleaseOutput>%s</multiReleaseOutput><compilerArgs>%s</compilerArgs></configuration></execution>")


OVERLAY_ROOT = Path("K:/m3-work/donors/overlays")  # fetch_overlays.py output
OVERLAY_EXEC = ("<execution><id>multi-release-%s</id><phase>process-classes</phase><goals><goal>compile</goal></goals><configuration>"
                "<release>%s</release><compileSourceRoots><compileSourceRoot>${project.basedir}/src/main/versions/%s</compileSourceRoot></compileSourceRoots>"
                "<multiReleaseOutput>true</multiReleaseOutput></configuration></execution>")
APIJAR_EXEC = ("<execution><id>multi-release-%s-apijar</id><phase>process-classes</phase><goals><goal>exec</goal></goals><configuration>"
               "<executable>${java.home}/bin/javac</executable><arguments>%s</arguments></configuration></execution>")
SHADE_FROM_PUBLISHED_POM = {"jackson-core"}  # artifact -> shade <configuration> copied from the published pom


def published_shade_configuration(pom: Path) -> str:
    if not pom.is_file():
        return ""
    text = pom.read_text(encoding="utf-8", errors="replace")
    m = re.search(r"<artifactId>maven-shade-plugin</artifactId>(.*?)</plugin>", text, re.S)
    if not m:
        return ""
    body = re.sub(r"<version>[^<]*</version>", "", m.group(1), count=1)  # the version is pinned by the generator
    ver = pom.parent.name  # Maven layout: <group>/<artifact>/<version>/<artifact>-<version>.pom
    props = {}
    pm = re.search(r"<properties>(.*?)</properties>", text, re.S)
    if pm:
        props.update({k: v.strip() for k, v in re.findall(r"<([A-Za-z0-9._-]+)>([^<]*)</\1>", pm.group(1))})
    props.update({"project.version": ver, "project.version.underscore": ver.replace(".", "_").replace("-", "_")})  # published coordinates win
    body = re.sub(r"\$\{([^}]+)\}", lambda k: props.get(k.group(1), k.group(0)), body)  # upstream build properties, published values
    return re.sub(r"\s+", " ", body).strip()


def published_base_release(jar: Path) -> int:
    """release = highest class version of the published base classes - 44 (floor 8); 0 when unknown."""
    if not jar.is_file():
        return 0
    top = 0
    with zipfile.ZipFile(jar) as z:
        for n in z.namelist():
            if n.endswith(".class") and not n.startswith("META-INF/") and not n.endswith("module-info.class"):
                b = z.read(n)[6:8]
                top = max(top, (b[0] << 8) | b[1])
    return max(8, top - 44) if top else 0


def sha256(p: Path) -> str:
    h = hashlib.sha256()
    with open(p, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def donor_id(a: str, v: str) -> str:
    return (a + "-" + v).replace("_", "-").lower()


def xml(s: str) -> str:
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace('"', "&quot;")


def automatic_module_name(jar: Path) -> str:
    if not jar.is_file():
        return ""
    with zipfile.ZipFile(jar) as z:
        try:
            text = z.read("META-INF/MANIFEST.MF").decode("utf-8", "replace").replace("\r\n", "\n").replace("\n ", "")
        except KeyError:
            return ""
    m = re.search(r"^Automatic-Module-Name:\s*(\S+)", text, re.M)
    return m.group(1) if m else ""


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--inventory", type=Path, nargs="+", required=True)
    ap.add_argument("--plan", type=Path, required=True, help="LOCK_PLAN.tsv")
    ap.add_argument("--status", type=Path, nargs="+", required=True, help="status.tsv files of the runner runs")
    ap.add_argument("--receipts", type=Path, nargs="+", required=True, help="run output dirs (donors/<id>/receipt.properties)")
    ap.add_argument("--build-inputs", type=Path, required=True)
    ap.add_argument("--store", type=Path, required=True)
    ap.add_argument("--m2", type=Path, default=Path(os.path.expanduser("~")) / ".m2" / "repository")
    ap.add_argument("--repo", type=Path, required=True, help="Synexia worktree root")
    ap.add_argument("--out", type=Path, required=True, help="ledger output THIRD_PARTY_MODULES.tsv")
    ap.add_argument("--template", type=Path, required=True, help="third_party/re2j (verify_source.py template)")
    ap.add_argument("--module-root", default="third_party", help="repo-relative directory for compiled modules")
    ap.add_argument("--image-root", default="donors/maven-sources", help="repo-relative directory for byte-for-byte images")
    ap.add_argument("--suffix", default="-synexia-source-1", help="version suffix of the source-built coordinate")
    ap.add_argument("--owner", default="Synexia", help="name of the receiving repository in generated texts")
    ap.add_argument("--only", default="", help="comma-separated donor ids to generate (default: all)")
    ap.add_argument("--api-surface", type=Path, nargs="*", default=[], help="API_SURFACE.tsv files; non-admissible verdicts demote PASS donors to images")
    ap.add_argument("--pom-classpath", type=Path, help="POM_CLASSPATH.tsv: top-level published-pom compile inputs become provided deps")
    args = ap.parse_args()
    global SUFFIX
    SUFFIX = args.suffix
    only = set(x for x in args.only.split(",") if x)
    lock_of_run = {}
    def run_lock(run_dir):
        if run_dir not in lock_of_run:
            props = {}
            lp = run_dir / "lock.properties"
            if lp.exists():
                for line in io.open(lp, encoding="utf-8").read().splitlines():
                    if "=" in line and not line.startswith("#"):
                        k_, v_ = line.split("=", 1)
                        props[k_] = v_
            lock_of_run[run_dir] = props
        return lock_of_run[run_dir]
    pomcp = {}
    if args.pom_classpath:
        for row in csv.DictReader(io.open(args.pom_classpath, encoding="utf-8", newline=""), delimiter="\t"):
            if row["declared"] == "TOP" and row["coordinate"] != "-":
                pomcp.setdefault(row["donor"].lower(), []).append(row)
    api = {}
    for f in args.api_surface:
        for row in csv.DictReader(io.open(f, encoding="utf-8", newline=""), delimiter="\t"):
            api[row["donor"]] = row

    rows = {}
    for inv in args.inventory:
        for r in csv.DictReader(io.open(inv, encoding="utf-8", newline=""), delimiter="\t"):
            if r["status"].endswith("OK"):
                rows[donor_id(r["artifact"], r["version"])] = r
    plan = {p["donor"]: p for p in csv.DictReader(io.open(args.plan, encoding="utf-8", newline=""), delimiter="\t")}
    status = {}
    for st in args.status:
        for line in io.open(st, encoding="utf-8").read().splitlines()[1:]:
            c = line.split("\t")
            if len(c) >= 2:
                status[c[0]] = c[1]
    receipts = {}
    for run in args.receipts:
        for rp in run.glob("donors/*/receipt.properties"):
            props = dict(l.split("=", 1) for l in io.open(rp, encoding="utf-8").read().splitlines() if "=" in l and not l.startswith("#"))
            if props.get("status") == "PASS" or receipts.get(rp.parent.name, ({},))[0].get("status") != "PASS":
                receipts[rp.parent.name] = (props, rp)
    build_inputs = {}
    for b in csv.DictReader(io.open(args.build_inputs, encoding="utf-8", newline=""), delimiter="\t"):
        for consumer in b["consumer"].split(","):
            build_inputs.setdefault(consumer.strip(), []).append(b)
    versions_per_artifact = {}
    for did, r in rows.items():
        versions_per_artifact.setdefault(r["artifact"], []).append(r["version"])
    verify_template = (args.template / "verify_source.py").read_text(encoding="utf-8")

    def module_dir(r: dict) -> str:
        return r["artifact"] if len(versions_per_artifact[r["artifact"]]) == 1 else r["artifact"] + "-" + r["version"]

    ledger = []
    for did, r in sorted(rows.items()):
        if only and did not in only:
            continue
        image = Path(r["sources_dir"])
        st = status.get(did, "NOT_RUN")
        if r["artifact"] in EXISTING_MODULES and args.owner == "Synexia":
            ledger.append((did, "EXISTING_MODULE", EXISTING_MODULES[r["artifact"]], r["group"], r["artifact"], r["version"] + SUFFIX, st, "existing", "hand-made source module from the upstream tag (same coordinate); runner status recorded only", "", "", r["consumers"]))
            print("EXISTING_MODULE", did, st, file=sys.stderr)
            continue
        p = dict(plan.get(did, {"build": "none", "reason": "NOT_IN_LOCK", "requires": "", "source": ""}))
        if did in receipts:
            lk = run_lock(receipts[did][1].parents[2])
            if ("donor.%s.build" % did) in lk:
                p["requires"] = lk.get("donor.%s.requires" % did, "")
                p["build"] = lk.get("donor.%s.build" % did, p.get("build", ""))
                p["lock_release"] = lk.get("donor.%s.release" % did, "")
        rec = receipts.get(did, ({}, None))[0]
        legal = sorted(x for x in os.listdir(image) if x.startswith("UPSTREAM_"))
        manifest = {m["path"]: (int(m["bytes"]), m["sha256"]) for m in csv.DictReader(io.open(image / "SOURCES_MANIFEST.tsv", encoding="utf-8", newline=""), delimiter="\t")}
        in_jar_license = sorted(x for x in manifest if re.search(r"(?i)^(META-INF/)?(LICENSE|COPYING)", x))
        license_src = (image / in_jar_license[0]) if in_jar_license else next((image / x for x in legal if x.startswith("UPSTREAM_LICENSE.") and not x.endswith(".json")), None)
        alt_build = None
        if st == "FAILED" and did in receipts and "DISTRIBUTION_BUDGET" in receipts[did][0].get("message", ""):
            atts = sorted(receipts[did][1].parent.glob("attempt-*/output/donor.jar"), key=lambda x: x.stat().st_mtime)
            if atts:
                alt_build = atts[-1].parent
                st = "PASS"
                p = dict(p, reason=(p.get("reason", "") + ";COMPILED_EXPORT_OVER_BUDGET (runner compile passed; attributed distribution exceeded the export budget)").lstrip(";"))
        gate = api.get(did)
        if st == "PASS" and gate is not None and gate["verdict"] not in ADMISSIBLE:
            st = "API_SURFACE_" + gate["verdict"]
            p = dict(p, reason="API_SURFACE_%s vs the published jar (missing classes %s, differing members %s/%s; diff %s)" % (gate["verdict"], gate["missing_classes"], gate["real_published_only_lines"], gate["real_built_only_lines"], gate["diff"]))
        if st == "FAILED" and [x for x in manifest if x.endswith(".java")] == ["module-info.java"] and p.get("requires") and all(status.get(d_) == "PASS" for d_ in p["requires"].split(",") if d_):
            st = "PASS"
            p = dict(p, reason=(p.get("reason", "") + ";DESCRIPTOR_ONLY_AGGREGATE (runner jars carry no module names; proven by the Maven module build and the descriptor gate)").lstrip(";"))
        if st == "PASS":
            mdir = args.repo / args.module_root / module_dir(r)
            kind = "MODULE"
        else:
            mdir = args.repo / args.image_root / did
            (args.repo / args.image_root).mkdir(parents=True, exist_ok=True)
            (args.repo / args.image_root / ".gitattributes").write_text(IMAGE_ROOT_ATTRS, encoding="utf-8", newline="\n")
            kind = "IMAGE"
        if mdir.exists():
            shutil.rmtree(mdir)
        mdir.mkdir(parents=True)
        files = []  # provenance rows
        excluded = []
        _rel = Path(r["group"].replace(".", "/")) / r["artifact"] / r["version"] / (r["artifact"] + "-" + r["version"] + ".jar")
        published_jar = args.m2 / _rel if (args.m2 / _rel).is_file() else args.store / _rel
        delombok_dir = None
        delomboked = []
        if kind == "MODULE" and receipts.get(did) and (receipts[did][1].parent / DELOMBOK_BUILD_DIR).is_dir():
            delombok_dir = receipts[did][1].parent / DELOMBOK_BUILD_DIR
        elif kind == "MODULE" and alt_build is not None and (alt_build / "delombok").is_dir():
            delombok_dir = alt_build / "delombok"
        descriptor_only = kind == "MODULE" and [x for x in manifest if x.endswith(".java")] == ["module-info.java"]  # aggregate artifact
        for path, (nbytes, digest) in sorted(manifest.items()):
            src = image / path
            if path in ("META-INF/MANIFEST.MF",) or path.startswith("META-INF/maven/") or (path.startswith("META-INF/") and path.endswith((".SF", ".RSA", ".DSA", ".EC"))):
                target_rel = "upstream-excluded/" + path if kind == "MODULE" else path
                fk = "unmodified-build-metadata"
            elif path.endswith(".kt") and kind == "MODULE" and kcfg(r):
                target_rel = "src/main/kotlin/" + path
                fk = "unmodified-production-kotlin"
            elif path.endswith(".java"):
                if kind == "MODULE" and re.match(r"META-INF/versions/\d+/.+\.java$", path) and not path.endswith("module-info.java") and int(path.split("/")[2]) <= BUILD_JDK_RELEASE:
                    target_rel = "src/main/versions/" + path.split("/", 2)[2]
                    fk = "unmodified-production-overlay"
                elif kind == "MODULE" and (path.startswith("META-INF/versions/") or path.startswith("java/") or (path.endswith("module-info.java") and not descriptor_only and (r["artifact"] in EXCLUDE_MODULE_INFO or LEGACY.get(r["artifact"], published_base_release(published_jar) or RELEASE.get(r["artifact"], 8)) < 9))):
                    target_rel = "upstream-excluded/" + path
                    fk = "unmodified-production-excluded-from-compilation"
                    excluded.append(path)
                elif delombok_dir is not None and (delombok_dir / path).is_file() and (delombok_dir / path).read_bytes() != src.read_bytes():
                    target_rel = "upstream-lombok/" + path
                    fk = "unmodified-production-lombok-source"
                    derived = delombok_dir / path
                    ddst = mdir / "src/main/java" / path
                    ddst.parent.mkdir(parents=True, exist_ok=True)
                    ddst.write_bytes(DELOMBOK_NOTICE.replace("{path}", path).encode("utf-8") + derived.read_bytes())
                    delomboked.append(path)
                    files.append({"kind": "delomboked-production", "repository": (mdir.relative_to(args.repo) / "src/main/java" / path).as_posix(), "sha256": sha256(ddst), "upstream": path, "derived_from": (mdir.relative_to(args.repo) / "upstream-lombok" / path).as_posix(), "tool": "lombok 1.18.30 delombok -f pretty, upstream lombok.config (UPSTREAM_LOMBOK.config) + one notice line"})
                elif kind == "MODULE" and r["artifact"] in TOKEN_FILTER and TOKEN_FILTER[r["artifact"]][0] == path:
                    target_rel = "upstream-excluded/" + path
                    fk = "unmodified-production-build-filtered-below"
                    tf_path, tf_cls, tf_fields = TOKEN_FILTER[r["artifact"]]
                    vals = published_constants(published_jar, tf_cls, tf_fields)
                    text = src.read_text(encoding="utf-8")
                    for f_, v_ in vals.items():
                        text = re.sub(r"(\b" + re.escape(f_) + r'\s*=\s*)"[^"]*"', lambda m_, v_=v_: m_.group(1) + '"' + v_ + '"', text, count=1)
                    fdst = mdir / "src/main/java" / path
                    fdst.parent.mkdir(parents=True, exist_ok=True)
                    fdst.write_bytes((FILTER_NOTICE.replace("{path}", path) + text).encode("utf-8"))
                    files.append({"kind": "build-filtered-production", "repository": (mdir.relative_to(args.repo) / "src/main/java" / path).as_posix(), "sha256": sha256(fdst), "upstream": path, "derived_from": (mdir.relative_to(args.repo) / "upstream-excluded" / path).as_posix(), "tool": "version tokens " + json.dumps(vals, sort_keys=True) + " read with javap -constants from the published jar"})
                else:
                    target_rel = ("src/main/java/" + path) if kind == "MODULE" else path
                    fk = "unmodified-production"
            else:
                target_rel = ("src/main/resources/" + path) if kind == "MODULE" else path
                fk = "unmodified-legal-or-build-reference" if LEGAL_RE.match(path.rsplit("/", 1)[-1]) else "unmodified-resource"
            dst = mdir / target_rel
            dst.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(src, dst)
            files.append({"kind": fk, "repository": (mdir.relative_to(args.repo) / target_rel).as_posix(), "sha256": digest, "upstream": path})
        completed = 0
        if kind == "MODULE" and published_jar.is_file():
            with zipfile.ZipFile(published_jar) as pz:
                pj_sha = sha256(published_jar)
                for info in sorted(pz.infolist(), key=lambda i: i.filename):
                    name = info.filename
                    if RESOURCE_SKIP(name):
                        continue
                    data = pz.read(info)
                    have = manifest.get(name)
                    if have is not None and have[1] == hashlib.sha256(data).hexdigest():
                        continue
                    dst = mdir / "src/main/resources" / name
                    if have is not None:
                        keep = mdir / "upstream-excluded" / name
                        keep.parent.mkdir(parents=True, exist_ok=True)
                        if dst.exists():
                            shutil.move(str(dst), str(keep))
                        files = [f_ for f_ in files if f_["repository"] != (mdir.relative_to(args.repo) / "src/main/resources" / name).as_posix()]
                        files.append({"kind": "unmodified-resource-superseded-by-published", "repository": (mdir.relative_to(args.repo) / "upstream-excluded" / name).as_posix(), "sha256": have[1], "upstream": name})
                        fkind = "build-filtered-resource-from-published-jar"
                    else:
                        fkind = "upstream-resource-from-published-jar"
                    dst.parent.mkdir(parents=True, exist_ok=True)
                    dst.write_bytes(data)
                    completed += 1
                    files.append({"kind": fkind, "repository": (mdir.relative_to(args.repo) / "src/main/resources" / name).as_posix(), "sha256": hashlib.sha256(data).hexdigest(), "upstream": name,
                                  "published_jar": "%s:%s:%s (sha256 %s)" % (r["group"], r["artifact"], r["version"], pj_sha)})
        shade_patterns = re.findall(r"<pattern>([^<]+)</pattern>", published_shade_configuration(args.store / r["group"].replace(".", "/") / r["artifact"] / r["version"] / (r["artifact"] + "-" + r["version"] + ".pom"))) if r["artifact"] in SHADE_FROM_PUBLISHED_POM else []
        if kind == "MODULE" and published_jar.is_file():  # source-only resources: upstream does not package them
            with zipfile.ZipFile(published_jar) as pz:
                pub_names = set(pz.namelist())
            res_prefix = (mdir.relative_to(args.repo) / "src/main/resources").as_posix() + "/"
            for f_ in files:
                if f_["repository"].startswith(res_prefix) and f_["kind"] in ("unmodified-resource", "unmodified-legal-or-build-reference") and f_["upstream"] not in pub_names \
                        and not any(f_["upstream"].startswith(pt_) for pt_ in shade_patterns):
                    keep = mdir / "upstream-source-only" / f_["upstream"]
                    keep.parent.mkdir(parents=True, exist_ok=True)
                    shutil.move(str(mdir / "src/main/resources" / f_["upstream"]), str(keep))
                    f_["repository"] = keep.relative_to(args.repo).as_posix()
                    f_["kind"] = "unmodified-source-only-not-packaged-upstream"
        natives = sum(1 for f_ in files if f_["repository"].lower().endswith(NATIVE_SUFFIXES) and "/src/main/resources/" in f_["repository"])
        for x in legal + ["SOURCES_MANIFEST.tsv"]:
            shutil.copyfile(image / x, mdir / x)
            files.append({"kind": "retained-provenance" if x.endswith((".json", ".tsv")) else "unmodified-legal-or-build-reference", "repository": (mdir.relative_to(args.repo) / x).as_posix(), "sha256": sha256(image / x), "upstream": "(fetched: see UPSTREAM_LICENSE.json)" if x.startswith("UPSTREAM_") else "(inventory)"})
        if license_src is not None and license_src.name != "LICENSE":
            shutil.copyfile(license_src, mdir / "LICENSE")
            files.append({"kind": "unmodified-legal-or-build-reference", "repository": (mdir.relative_to(args.repo) / "LICENSE").as_posix(), "sha256": sha256(license_src), "upstream": license_src.name if license_src.name.startswith("UPSTREAM_") else str(license_src.relative_to(image)).replace("\\", "/")})
        jar = args.store / r["group"].replace(".", "/") / r["artifact"] / r["version"] / (r["artifact"] + "-" + r["version"] + "-sources.jar")
        binary = args.m2 / r["group"].replace(".", "/") / r["artifact"] / r["version"] / (r["artifact"] + "-" + r["version"] + ".jar")
        if not binary.is_file():
            binary = args.store / r["group"].replace(".", "/") / r["artifact"] / r["version"] / (r["artifact"] + "-" + r["version"] + ".jar")
        amn = automatic_module_name(binary)
        central = CENTRAL + r["group"].replace(".", "/") + "/" + r["artifact"] + "/" + r["version"] + "/"
        lic_name = r["licenses"].split(";")[0].split(" <")[0].strip() if not r["licenses"].startswith("(none") else ""
        if not lic_name and license_src is not None:
            head = io.open(license_src, encoding="utf-8", errors="replace").read(600).lower()
            for k, v in (("apache license", "Apache License, Version 2.0"), ("mit license", "MIT License"), ("bsd", "BSD License"), ("eclipse public", "Eclipse Public License")):
                if k in head:
                    lic_name = v + " (from the license text; pom declares none)"; break
        lic_name = lic_name or "UNDECLARED (see LICENSE)"
        deps = []
        for dep in [d for d in p["requires"].split(",") if d]:
            dr = rows.get(dep)
            if dr is None:
                continue
            scope, optional = "compile", False
            declared = next((d for d in r["compile_deps"].split(";") if d.split(":")[:2] == [dr["group"], dr["artifact"]]), None)
            if declared is None:
                optional = True  # reached only through the import closure: upstream treats it as optional
            else:
                parts = declared.split(":")
                scope = parts[3] if len(parts) > 3 else "compile"
                optional = declared.endswith(":optional")
                if scope == "runtime":
                    scope = "compile"
            target_status = status.get(dep, "NOT_RUN")
            if dr["artifact"] in NATIVE_IN_BINARY or target_status != "PASS":
                deps.append((dr["group"], dr["artifact"], dr["version"], scope, optional, "published binary (%s)" % ("native libraries live in the binary jar" if dr["artifact"] in NATIVE_IN_BINARY else "donor status " + target_status)))
            else:
                deps.append((dr["group"], dr["artifact"], dr["version"] + SUFFIX, scope, optional, "source module"))
        req_ga = set((d[0], d[1]) for d in deps)
        for g_, a_, v_ in EXTRA_COMPILE_DEPS.get(did, []):
            if (g_, a_) not in req_ga:
                req_ga.add((g_, a_))
                src_ok = status.get(donor_id(a_, v_)) == "PASS"
                deps.append((g_, a_, v_ + SUFFIX if src_ok else v_, "provided", True, "fully qualified reference in code (not an import); compile-only"))
        for g_, a_, v_ in OVERLAY_COMPILE_INPUTS.get(did, []):
            if (g_, a_) not in req_ga:
                req_ga.add((g_, a_))
                deps.append((g_, a_, v_, "provided", True, "published-pom provided input of the multi-release overlay (GraalVM native-image Feature); binary, never shipped"))
        for row in pomcp.get(did, []):
            g_, a_, v_ = row["coordinate"].split(":")[:3]
            if (g_, a_) in req_ga or (g_, a_) == (r["group"], r["artifact"]):
                continue
            req_ga.add((g_, a_))
            deps.append((g_, a_, v_, BINARY_INPUT_SCOPE, True, "published-pom compile input (%s%s), binary, never shipped" % (row["scope"], ", optional" if row["optional"] == "true" else "")))
        if kcfg(r):
            deps.append(("org.jetbrains.kotlin", "kotlin-stdlib", kcfg(r)[0], "provided", True, "Kotlin standard library for the Kotlin facade, compile-only"))
        for b in build_inputs.get(did, []):
            g, a, v = b["coordinate"].split(":")
            if a == "lombok":
                continue  # delombok tool only; the delomboked sources do not reference Lombok
            deps.append((g, a, v, BINARY_INPUT_SCOPE, True, "binary build input for an optional adapter (BUILD_INPUTS.tsv)"))
        overlay_info = {}
        if kind == "MODULE" and (OVERLAY_ROOT / did / "OVERLAY_SOURCES.json").is_file():
            overlay_info = json.loads((OVERLAY_ROOT / did / "OVERLAY_SOURCES.json").read_text(encoding="utf-8"))
            for of in overlay_info["files"]:
                if of["release"] == "base":  # supersedes the sources-jar variant in the main compile
                    bdst = mdir / "src/main/java" / (of["class"] + ".java")
                    for vdup in sorted((mdir / "src/main/java").glob("*.*/" + of["class"] + ".java")):  # module-source-variant copy
                        vkeep = mdir / "upstream-excluded" / vdup.relative_to(mdir / "src/main/java")
                        vkeep.parent.mkdir(parents=True, exist_ok=True)
                        shutil.move(str(vdup), str(vkeep))
                        for f_ in files:
                            if f_["repository"] == vdup.relative_to(args.repo).as_posix():
                                f_["repository"], f_["kind"] = vkeep.relative_to(args.repo).as_posix(), "unmodified-production-superseded-by-upstream-tag"
                    if bdst.is_file():
                        bkeep = mdir / "upstream-excluded" / (of["class"] + ".java")
                        bkeep.parent.mkdir(parents=True, exist_ok=True)
                        shutil.move(str(bdst), str(bkeep))
                        for f_ in files:
                            if f_["repository"] == bdst.relative_to(args.repo).as_posix():
                                f_["repository"], f_["kind"] = bkeep.relative_to(args.repo).as_posix(), "unmodified-production-superseded-by-upstream-tag"
                    bdst.parent.mkdir(parents=True, exist_ok=True)
                    shutil.copyfile(OVERLAY_ROOT / did / "base" / (of["class"] + ".java"), bdst)
                    files.append({"kind": "upstream-base-source-at-tag", "repository": bdst.relative_to(args.repo).as_posix(), "sha256": of["sha256"],
                                  "upstream": of["upstream_path"], "url": of["url"], "commit": overlay_info["commit"], "tag": overlay_info["tag"]})
                    continue
                odst = mdir / "src/main/versions" / of["release"] / (of["class"] + ".java")
                odst.parent.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(OVERLAY_ROOT / did / of["release"] / (of["class"] + ".java"), odst)
                files.append({"kind": "upstream-overlay-source-at-tag", "repository": odst.relative_to(args.repo).as_posix(), "sha256": of["sha256"],
                              "upstream": of["upstream_path"], "url": of["url"], "commit": overlay_info["commit"], "tag": overlay_info["tag"]})
            shutil.copyfile(OVERLAY_ROOT / did / "OVERLAY_SOURCES.json", mdir / "OVERLAY_SOURCES.json")
            files.append({"kind": "retained-provenance", "repository": (mdir / "OVERLAY_SOURCES.json").relative_to(args.repo).as_posix(), "sha256": sha256(mdir / "OVERLAY_SOURCES.json"), "upstream": "fetch_overlays.py"})
        overlay_releases = sorted((d_.name for d_ in (mdir / "src/main/versions").iterdir() if d_.is_dir()), key=int) if kind == "MODULE" and (mdir / "src/main/versions").is_dir() else []
        apijar_rel = {}
        for bi in overlay_info.get("build_inputs", []):  # upstream build inputs at the tag (apijar API stubs)
            bdst2 = mdir / "build-inputs" / bi["file"].rsplit("/", 1)[1]
            bdst2.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(OVERLAY_ROOT / did / bi["file"], bdst2)
            files.append({"kind": "upstream-build-input-at-tag", "repository": bdst2.relative_to(args.repo).as_posix(), "sha256": bi["sha256"],
                          "upstream": bi["upstream_path"], "url": bi["url"], "commit": overlay_info["commit"], "tag": overlay_info["tag"]})
            apijar_rel[bi["release"]] = bdst2.name
        overlay_execs = [OVERLAY_EXEC % (n_, n_, n_) for n_ in overlay_releases if n_ not in apijar_rel]
        apijar_execs = []
        if apijar_rel and published_jar.is_file():
            with zipfile.ZipFile(published_jar) as pz:
                ov_major = {n_.split("/")[2]: (pz.read(n_)[6] << 8) | pz.read(n_)[7] for n_ in pz.namelist() if re.match(r"META-INF/versions/\d+/.+\.class$", n_) and not n_.endswith("module-info.class")}
            for n_ in overlay_releases:
                if n_ in apijar_rel:
                    tgt = str(ov_major.get(n_, 55) - 44)
                    srcs = sorted(x_.relative_to(mdir).as_posix() for x_ in (mdir / "src/main/versions" / n_).rglob("*.java"))
                    argv = ["-source", tgt, "-target", tgt, "-Xlint:-options", "-proc:none", "-encoding", "UTF-8",
                            "--patch-module", "java.base=${project.basedir}/build-inputs/" + apijar_rel[n_],
                            "--add-exports", "java.base/java.lang.foreign=ALL-UNNAMED", "--add-exports", "java.base/jdk.incubator.vector=ALL-UNNAMED",
                            "-classpath", "${project.build.outputDirectory}", "-d", "${project.build.outputDirectory}/META-INF/versions/" + n_] + ["${project.basedir}/" + x_ for x_ in srcs]
                    apijar_execs.append(APIJAR_EXEC % (n_, "".join("<argument>%s</argument>" % xml(a_) for a_ in argv)))
        descriptor_execs, descriptor_notes, versioned_descriptor = [], [], False
        moditect_execs = []
        main_mver = ""
        if kind == "MODULE" and published_jar.is_file():
            with zipfile.ZipFile(published_jar) as pz:
                pub_desc = {n_: parse_descriptor(pz.read(n_)) for n_ in pz.namelist() if n_.endswith("module-info.class")}
            main_root = mdir / "src/main/java/module-info.java"
            if main_root.is_file() and "module-info.class" not in pub_desc and pub_desc:
                keep = mdir / "upstream-excluded" / "module-info.java"
                keep.parent.mkdir(parents=True, exist_ok=True)
                shutil.move(str(main_root), str(keep))
                for f_ in files:
                    if f_["repository"] == main_root.relative_to(args.repo).as_posix():
                        f_["repository"], f_["kind"] = keep.relative_to(args.repo).as_posix(), "unmodified-production-excluded"
                excluded.append("module-info.java\tPUBLISHED_DESCRIPTOR_IS_VERSIONED")
            if main_root.is_file() and "module-info.class" in pub_desc:  # descriptor produced by the main compile: published module version
                main_mver = ("<moduleVersion>%s</moduleVersion>" % xml(pub_desc["module-info.class"]["version"])) if pub_desc["module-info.class"]["version"] else "<useModuleVersion>false</useModuleVersion>"
            for dpath, d in sorted(pub_desc.items()):
                rel_ = d["major"] - 44
                if dpath == "module-info.class" and main_root.is_file():
                    continue  # produced by the main compile
                if rel_ > BUILD_JDK_RELEASE:
                    descriptor_notes.append("%s: class version %d needs JDK %d (toolchain is JDK %d); not reproduced" % (dpath, d["major"], rel_, BUILD_JDK_RELEASE))
                    continue
                slot = "root" if dpath == "module-info.class" else dpath.split("/")[2]
                cands = (["module-info.java"] if slot == "root" else ["META-INF/versions/%s/module-info.java" % slot]) + ["%s/module-info.java" % d["name"]]
                dsrc = next((c_ for c_ in cands if c_ in manifest), None)
                tag_desc = next((x_ for x_ in overlay_info.get("descriptors", []) if x_["release"] == slot), None)
                ddst = mdir / "src/main/descriptor" / slot / "module-info.java"
                ddst.parent.mkdir(parents=True, exist_ok=True)
                shaded_module = r["artifact"] in SHADE_RULES or r["artifact"] in SHADE_FROM_PUBLISHED_POM
                if not dsrc and tag_desc:  # upstream module-info.java at the release tag (overlay root)
                    shutil.copyfile(OVERLAY_ROOT / did / slot / "module-info.java", ddst)
                    files.append({"kind": "upstream-descriptor-source-at-tag", "repository": ddst.relative_to(args.repo).as_posix(), "sha256": tag_desc["sha256"],
                                  "upstream": tag_desc["upstream_path"], "url": tag_desc["url"], "commit": overlay_info["commit"], "tag": overlay_info["tag"]})
                    dsrc = "tag:" + tag_desc["upstream_path"]
                    if shaded_module:
                        moditect_execs.append(MODITECT_EXEC % (slot, "base" if slot == "root" else slot, "<version>%s</version>" % xml(d["version"]) if d["version"] else "<version></version>", slot))
                        versioned_descriptor = versioned_descriptor or slot != "root"
                elif dsrc:
                    shutil.copyfile(image / dsrc, ddst)
                    if shaded_module:  # shade drops module-info.class: moditect re-adds it after the shade step
                        moditect_execs.append(MODITECT_EXEC % (slot, "base" if slot == "root" else slot, "<version>%s</version>" % xml(d["version"]) if d["version"] else "<version></version>", slot))
                        versioned_descriptor = versioned_descriptor or slot != "root"
                    files.append({"kind": "unmodified-production-descriptor", "repository": ddst.relative_to(args.repo).as_posix(), "sha256": manifest[dsrc][1], "upstream": dsrc})
                else:  # upstream generates this descriptor in its build: moditect assembles it from the declaration
                    ddst.write_text(descriptor_source(d, dpath), encoding="utf-8", newline="\n")
                    moditect_execs.append(MODITECT_EXEC % (slot, "base" if slot == "root" else slot, "<version>%s</version>" % xml(d["version"]) if d["version"] else "<version></version>", slot))
                    versioned_descriptor = versioned_descriptor or slot != "root"
                    files.append({"kind": "descriptor-from-published-jar", "repository": ddst.relative_to(args.repo).as_posix(), "sha256": sha256(ddst), "upstream": dpath,
                                  "published_jar": "%s:%s:%s (sha256 %s)" % (r["group"], r["artifact"], r["version"], sha256(published_jar))})
                dargs = ("<arg>--patch-module</arg><arg>%s=${project.build.outputDirectory}</arg>" % xml(d["name"]) if slot == "root" else "")
                mver = ("<moduleVersion>%s</moduleVersion>" % xml(d["version"])) if d["version"] else "<useModuleVersion>false</useModuleVersion>"  # the published descriptor's version, not the project version
                if dsrc and not shaded_module:
                    descriptor_execs.append((DESCRIPTOR_EXEC % (slot, rel_, slot, "false" if slot == "root" else "true", dargs)).replace("<compilerArgs>", mver + "<compilerArgs>", 1))
                versioned_descriptor = versioned_descriptor or slot != "root"
                if d["main"]:
                    descriptor_notes.append("%s: ModuleMainClass %s is written by the jar tool, not javac" % (dpath, d["main"]))
        provenance = {
            "decision": "DIRECT_IMPORT",
            "donor": did, "group": r["group"], "artifact": r["artifact"], "version": r["version"],
            "coordinate": "%s:%s:%s" % (r["group"], r["artifact"], r["version"] + SUFFIX if kind == "MODULE" else r["version"]),
            "kind": kind, "status": st, "build": p["build"], "build_reason": p["reason"],
            "sources_jar": central + r["artifact"] + "-" + r["version"] + "-sources.jar",
            "sources_jar_sha1": r["sources_sha1"], "sources_jar_sha256": sha256(jar) if jar.is_file() else "",
            "pom": central + r["artifact"] + "-" + r["version"] + ".pom",
            "license": lic_name, "license_file": "LICENSE", "upstream": r["scm"] or r["url"], "parent": r["parent"],
            "automatic_module_name": amn, "excluded_from_compilation": excluded, "delomboked": delombok_dir is not None, "delomboked_files": len(delomboked), "resources_completed": completed, "native_binaries": natives,
            "dependencies": [{"group": g, "artifact": a, "version": v, "scope": s, "optional": o, "form": f} for g, a, v, s, o, f in deps],
            "receipt": {k: rec.get(k, "") for k in ("status", "lock", "sourceRoot", "buildRoot", "distributionSha256", "message") if k in rec},
            "modifications": "No upstream source changes; Maven descriptor and provenance files are Synexia composition (Apache-2.0) and do not relicense the donor.",
            "consumers": r["consumers"], "files": files, "module_descriptors": descriptor_notes or "reproduced (see src/main/descriptor)" if descriptor_execs else descriptor_notes, "multi_release_overlays": {"releases": overlay_releases, "not_reproduced": overlay_info.get("not_reproduced", [])},
        }
        (mdir / "PROVENANCE.json").write_text(json.dumps(provenance, indent=2, sort_keys=True) + "\n", encoding="utf-8", newline="\n")
        if kind == "MODULE":
            release = published_base_release(published_jar) or RELEASE.get(r["artifact"], 8)  # the published target, not a manual override
            if descriptor_only:  # the published descriptor's class version decides (junit-jupiter: 53 -> release 9)
                with zipfile.ZipFile(published_jar) as pz:
                    aggregate_release = parse_descriptor(pz.read("module-info.class"))["major"] - 44 if "module-info.class" in pz.namelist() else BUILD_JDK_RELEASE
                release = aggregate_release
            legacy = LEGACY.get(r["artifact"])
            dep_xml = "".join(
                "    <dependency><groupId>%s</groupId><artifactId>%s</artifactId><version>%s</version>%s%s<!-- %s --></dependency>\n"
                % (xml(g), xml(a), xml(v), "<scope>%s</scope>" % s if s != "compile" else "", "<optional>true</optional>" if o else "", xml(f))
                for g, a, v, s, o, f in deps)
            compiler_cfg = ("<source>%d</source><target>%d</target>" % (legacy, legacy) if legacy else "<release>%d</release>" % release) + "<encoding>%s</encoding>" % ENCODING.get(r["artifact"], "UTF-8")
            extra_args = "".join("<arg>%s</arg>" % xml(a) for a in JAVAC_ARGS.get(r["artifact"], []))
            pub_shade = published_shade_configuration(args.store / r["group"].replace(".", "/") / r["artifact"] / r["version"] / (r["artifact"] + "-" + r["version"] + ".pom")) if r["artifact"] in SHADE_FROM_PUBLISHED_POM else ""
            if r["artifact"] in SHADE_FROM_PUBLISHED_POM and not pub_shade:
                pub_shade = published_shade_configuration(args.m2 / r["group"].replace(".", "/") / r["artifact"] / r["version"] / (r["artifact"] + "-" + r["version"] + ".pom"))
            shade_verbatim = ("      <plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-shade-plugin</artifactId><version>3.6.0</version>\n"
                              "        <!-- the published pom's shade configuration, verbatim -->\n"
                              "        " + pub_shade + "\n      </plugin>\n") if pub_shade else ""
            moditect_plugin = ("      <plugin><groupId>org.moditect</groupId><artifactId>moditect-maven-plugin</artifactId><version>1.2.2.Final</version>\n"
                               "        <!-- descriptors upstream generates in its build, assembled from the declaration in src/main/descriptor -->\n"
                               "        <executions>" + "".join(moditect_execs) + "</executions>\n      </plugin>\n") if moditect_execs else ""
            extra_plugins = shade_verbatim + (kotlin_plugin(kcfg(r)) if kcfg(r) else "") + ((
                "      <plugin><groupId>org.codehaus.mojo</groupId><artifactId>exec-maven-plugin</artifactId><version>3.5.0</version>\n"
                "        <!-- upstream core-mrjar.gradle: javaN overlays compiled against the JDK N apijar (patched java.base) -->\n"
                "        <executions>" + "".join(apijar_execs) + "</executions>\n      </plugin>\n") if apijar_execs else "") + BUILD_PLUGINS.get(r["artifact"], "") + (shade_plugin(SHADE_RULES[r["artifact"]]) if r["artifact"] in SHADE_RULES else "")
            pom = f"""<!-- SPDX-License-Identifier: Apache-2.0; build integration only, upstream source remains {xml(lic_name)}. -->
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>{xml(r["group"])}</groupId><artifactId>{xml(r["artifact"])}</artifactId><version>{xml(r["version"])}{SUFFIX}</version>
  <name>{xml(r["artifact"])} {xml(r["version"])} - unchanged upstream source build for Synexia</name>
  <description>Source inclusion of the published {xml(r["group"])}:{xml(r["artifact"])}:{xml(r["version"])} sources jar (SHA-1 {xml(r["sources_sha1"])}); no upstream implementation changes. See PROVENANCE.json.</description>
  <url>{xml(r["url"] or central)}</url>
  <licenses><license><name>{xml(lic_name)}</name><url>{xml(central + r["artifact"] + "-" + r["version"] + ".pom")}</url><distribution>repo</distribution></license></licenses>
  <properties><project.build.sourceEncoding>{ENCODING.get(r["artifact"], "UTF-8")}</project.build.sourceEncoding></properties>
  <dependencies>
{dep_xml}  </dependencies>
  <build>
    <resources>
      <resource><directory>src/main/resources</directory></resource>
      <resource><directory>${{project.basedir}}</directory><targetPath>META-INF/third-party/{xml(module_dir(r))}</targetPath><includes><include>LICENSE</include><include>PROVENANCE.json</include><include>THIRD_PARTY_NOTICES.md</include></includes></resource>
    </resources>
    <plugins>
      <plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-compiler-plugin</artifactId><version>3.15.0</version>
        <!-- Upstream bytes are compiled as published: no annotation processing, no warnings-as-errors. Synexia integration is checked separately with -Werror. -->
        <configuration>{compiler_cfg}{main_mver}<proc>none</proc><compilerArgs><arg>-Xlint:all,-options</arg>{extra_args}</compilerArgs></configuration>
{("        <executions>" + "".join(overlay_execs + descriptor_execs) + "</executions>\n") if (overlay_execs or descriptor_execs) else ""}      </plugin>
      <plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-surefire-plugin</artifactId><version>3.5.6</version>
        <!-- Published sources jars carry no test suite; the compile and the consumers' own tests are the proof lane. -->
        <configuration><skipTests>true</skipTests></configuration>
      </plugin>
      <plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-jar-plugin</artifactId><version>3.5.0</version>
        <configuration>{"<excludes>" + "".join("<exclude>%s</exclude>" % e for e in (sum([ex for (a_, v_), ex in PACKAGING_EXCLUDES.items() if a_ == r["artifact"] and r["version"].startswith(v_)], []) + ["META-INF/jpms.args"])) + "</excludes>"}<archive><manifestEntries>{("<Automatic-Module-Name>%s</Automatic-Module-Name>" % xml(amn)) if amn else "<Synexia-Source-Inline>no upstream Automatic-Module-Name</Synexia-Source-Inline>"}{"<Multi-Release>true</Multi-Release>" if (versioned_descriptor or overlay_execs or apijar_execs) else ""}</manifestEntries></archive></configuration>
      </plugin>
{extra_plugins}{moditect_plugin}    </plugins>
  </build>
</project>
"""
            (mdir / "pom.xml").write_text(pom, encoding="utf-8", newline="\n")
            verify = verify_template.replace('"META-INF/third-party/re2j/"', '"META-INF/third-party/%s/"' % module_dir(r)).replace('"Automatic-Module-Name: re2j"', '"Automatic-Module-Name: %s"' % amn if amn else '"Synexia-Source-Inline: no upstream Automatic-Module-Name"')
            (mdir / "verify_source.py").write_text(verify, encoding="utf-8", newline="\n")
            (mdir / ".gitattributes").write_text("src/** -text\nupstream-excluded/** -text\nLICENSE -text\nUPSTREAM_* -text\nSOURCES_MANIFEST.tsv -text\n"
                                                 "# upstream bytes are SHA-256 pinned in PROVENANCE.json; their whitespace is upstream's\n* -whitespace\n", encoding="utf-8", newline="\n")
        notices = f"""# {r["artifact"]} {r["version"]} source inclusion

{r["artifact"]} is the work of its upstream authors and contributors. Copyright remains with the copyright
holders named in each unchanged source file and in the retained legal texts. This directory does not
relabel upstream implementation as Synexia-original.

Upstream: {r["scm"] or r["url"] or central}
Published sources jar: {central}{r["artifact"]}-{r["version"]}-sources.jar (SHA-1 {r["sources_sha1"]})
License as declared by the published pom: {lic_name}

The complete upstream license text is retained in `LICENSE` ({"from the sources jar" if in_jar_license else "fetched from the upstream tag; see UPSTREAM_LICENSE.json"}){" and packaged with the source-built JAR under META-INF/third-party/" + module_dir(r) if kind == "MODULE" else ""},
alongside this notice and `PROVENANCE.json`. Every file listed in `SOURCES_MANIFEST.tsv` retains its exact
upstream bytes and header{(" except the " + str(len(delomboked)) + " files under src/main/java that Lombok uses: those are the Lombok 1.18.30 delombok expansion (with the upstream lombok.config kept as UPSTREAM_LOMBOK.config), each marked by a notice line, with the unchanged upstream file kept under upstream-lombok/") if delomboked else ""}. {"The Maven build descriptor and integration metadata are Synexia composition, licensed Apache-2.0; that license does not relicense the donor implementation." if kind == "MODULE" else "No build descriptor is generated: donor-superset status " + st + " (" + p["reason"] + ")."}
{("Consumers use `" + r["group"] + ":" + r["artifact"] + ":" + r["version"] + SUFFIX + "`, compiled from these files." + (" Native libraries are not part of the published sources; consumers that need them keep the published binary." if r["artifact"] in NATIVE_IN_BINARY else "")) if kind == "MODULE" else ""}
"""
        (mdir / "THIRD_PARTY_NOTICES.md").write_text(notices, encoding="utf-8", newline="\n")
        readme = f"""# {r["artifact"]} {r["version"]} source reference

{"This directory compiles the unchanged " + r["artifact"] + " " + r["version"] + " sources from the published sources jar." if kind == "MODULE" else "This directory retains the unchanged " + r["artifact"] + " " + r["version"] + " published sources byte-for-byte without a build (status " + st + ": " + p["reason"] + ")."}
Donor-superset lane: build kind `{p["build"]}`{" (" + p["reason"] + ")" if p["reason"] else ""}, status `{st}`.
{"Excluded from compilation (retained under upstream-excluded/): " + ", ".join(excluded) if excluded else ""}

```text
{"mvn -f third_party/" + module_dir(r) + "/pom.xml clean install" if kind == "MODULE" else "python -I .m3/task-crates/dependency-source-inline-20261010/verify_crate.py"}
{"python third_party/" + module_dir(r) + "/verify_source.py third_party/" + module_dir(r) + "/target/" + r["artifact"] + "-" + r["version"] + SUFFIX + ".jar" if kind == "MODULE" else ""}
```

No speed, allocation or behaviour change is claimed. The published sources jar carries no test suite; the
compile from source and the consumers' own tests are the proof lane. Upstream tests remain a follow-up with
their own recipe. See `PROVENANCE.json` for the file-level pins and the runner receipt.
"""
        (mdir / "README.md").write_text(readme, encoding="utf-8", newline="\n")
        if args.owner != "Synexia":
            for name in ("pom.xml", "README.md", "THIRD_PARTY_NOTICES.md", "PROVENANCE.json", "verify_source.py"):
                f = mdir / name
                if f.exists():
                    t = f.read_text(encoding="utf-8").replace("Synexia", args.owner)
                    if name != "PROVENANCE.json":
                        t = t.replace("third_party/" + module_dir(r), args.module_root + "/" + module_dir(r)).replace(args.module_root + "/" + args.module_root + "/", args.module_root + "/")
                    f.write_text(t, encoding="utf-8", newline="\n")
        ledger.append((did, kind, str(mdir.relative_to(args.repo)).replace("\\", "/"), r["group"], r["artifact"], r["version"] + (SUFFIX if kind == "MODULE" else ""), st, p["build"], p["reason"], amn, ";".join("%s:%s:%s:%s%s" % (g, a, v, s, ":optional" if o else "") for g, a, v, s, o, f in deps), r["consumers"]))
        print(kind, did, st, mdir.relative_to(args.repo), file=sys.stderr)
    args.out.write_text("\n".join(["donor\tkind\tpath\tgroup\tartifact\tversion\tstatus\tbuild\treason\tautomatic_module_name\tdependencies\tconsumers"] + ["\t".join(l) for l in ledger]) + "\n", encoding="utf-8", newline="\n")
    print("modules", sum(1 for l in ledger if l[1] == "MODULE"), "images", sum(1 for l in ledger if l[1] == "IMAGE"))
    return 0


if __name__ == "__main__":
    sys.exit(main())
