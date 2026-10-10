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
import argparse, csv, hashlib, io, json, os, re, shutil, sys, zipfile
from pathlib import Path

CENTRAL = "https://repo.maven.apache.org/maven2/"
SUFFIX = "-synexia-source-1"
LEGAL_RE = re.compile(r"(?i)(.*[-_.])?(license|licence|notice|copying|copyright)([._-].*)?$")
RELEASE = {"caffeine": 11, "tomlj": 11, "antlr4-runtime": 11, "snakeyaml-engine": 11, "ecj": 17, "commonmark": 11, "fastdoubleparser": 21, "jackson-core": 21, "jackson-databind": 21, "jackson-dataformat-yaml": 21, "jackson-datatype-jsr310": 21, "lucene-core": 11, "lucene-queries": 11, "lucene-sandbox": 11,
           "lucene-analysis-common": 11, "lucene-queryparser": 11}
LEGACY = {"zero-allocation-hashing", "guava", "eclipse-collections", "agrona", "lz4-java", "asm", "jspecify"}
ENCODING = {"commons-compress": "ISO-8859-1"}
EXCLUDE_MODULE_INFO = set()  # artifacts whose module descriptor must stay uncompiled (filled after the first reactor build)
NATIVE_IN_BINARY = {"sqlite-jdbc", "jna", "zstd-jni"}  # consumers keep the published binary; module is compile proof only
BINARY_INPUT_SCOPE = "provided"
EXISTING_MODULES = {"re2j": "third_party/re2j"}  # already inlined by hand from the upstream tag at the same coordinate; never regenerated


def sha256(p: Path) -> str:
    h = hashlib.sha256()
    with open(p, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def donor_id(a: str, v: str) -> str:
    return (a + "-" + v).replace("_", "-")


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
    args = ap.parse_args()
    global SUFFIX
    SUFFIX = args.suffix
    only = set(x for x in args.only.split(",") if x)

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
        p = plan.get(did, {"build": "none", "reason": "NOT_IN_LOCK", "requires": "", "source": ""})
        rec = receipts.get(did, ({}, None))[0]
        legal = sorted(x for x in os.listdir(image) if x.startswith("UPSTREAM_"))
        manifest = {m["path"]: (int(m["bytes"]), m["sha256"]) for m in csv.DictReader(io.open(image / "SOURCES_MANIFEST.tsv", encoding="utf-8", newline=""), delimiter="\t")}
        in_jar_license = sorted(x for x in manifest if re.search(r"(?i)^(META-INF/)?(LICENSE|COPYING)", x))
        license_src = (image / in_jar_license[0]) if in_jar_license else next((image / x for x in legal if x.startswith("UPSTREAM_LICENSE.") and not x.endswith(".json")), None)
        if st == "PASS":
            mdir = args.repo / args.module_root / module_dir(r)
            kind = "MODULE"
        else:
            mdir = args.repo / args.image_root / did
            kind = "IMAGE"
        if mdir.exists():
            shutil.rmtree(mdir)
        mdir.mkdir(parents=True)
        files = []  # provenance rows
        excluded = []
        for path, (nbytes, digest) in sorted(manifest.items()):
            src = image / path
            if path in ("META-INF/MANIFEST.MF",) or path.startswith("META-INF/maven/") or (path.startswith("META-INF/") and path.endswith((".SF", ".RSA", ".DSA", ".EC"))):
                target_rel = "upstream-excluded/" + path if kind == "MODULE" else path
                fk = "unmodified-build-metadata"
            elif path.endswith(".java"):
                if kind == "MODULE" and (path.startswith("META-INF/versions/") or (path.endswith("module-info.java") and r["artifact"] in EXCLUDE_MODULE_INFO)):
                    target_rel = "upstream-excluded/" + path
                    fk = "unmodified-production-excluded-from-compilation"
                    excluded.append(path)
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
        for b in build_inputs.get(did, []):
            g, a, v = b["coordinate"].split(":")
            deps.append((g, a, v, BINARY_INPUT_SCOPE, True, "binary build input for an optional adapter (BUILD_INPUTS.tsv)"))
        provenance = {
            "decision": "DIRECT_IMPORT",
            "donor": did, "group": r["group"], "artifact": r["artifact"], "version": r["version"],
            "coordinate": "%s:%s:%s" % (r["group"], r["artifact"], r["version"] + SUFFIX if kind == "MODULE" else r["version"]),
            "kind": kind, "status": st, "build": p["build"], "build_reason": p["reason"],
            "sources_jar": central + r["artifact"] + "-" + r["version"] + "-sources.jar",
            "sources_jar_sha1": r["sources_sha1"], "sources_jar_sha256": sha256(jar) if jar.is_file() else "",
            "pom": central + r["artifact"] + "-" + r["version"] + ".pom",
            "license": lic_name, "license_file": "LICENSE", "upstream": r["scm"] or r["url"], "parent": r["parent"],
            "automatic_module_name": amn, "excluded_from_compilation": excluded,
            "dependencies": [{"group": g, "artifact": a, "version": v, "scope": s, "optional": o, "form": f} for g, a, v, s, o, f in deps],
            "receipt": {k: rec.get(k, "") for k in ("status", "lock", "sourceRoot", "buildRoot", "distributionSha256", "message") if k in rec},
            "modifications": "No upstream source changes; Maven descriptor and provenance files are Synexia composition (Apache-2.0) and do not relicense the donor.",
            "consumers": r["consumers"], "files": files,
        }
        (mdir / "PROVENANCE.json").write_text(json.dumps(provenance, indent=2, sort_keys=True) + "\n", encoding="utf-8", newline="\n")
        if kind == "MODULE":
            release = RELEASE.get(r["artifact"], 8)
            legacy = r["artifact"] in LEGACY
            dep_xml = "".join(
                "    <dependency><groupId>%s</groupId><artifactId>%s</artifactId><version>%s</version>%s%s<!-- %s --></dependency>\n"
                % (xml(g), xml(a), xml(v), "<scope>%s</scope>" % s if s != "compile" else "", "<optional>true</optional>" if o else "", xml(f))
                for g, a, v, s, o, f in deps)
            compiler_cfg = ("<source>8</source><target>8</target>" if legacy else "<release>%d</release>" % release) + "<encoding>%s</encoding>" % ENCODING.get(r["artifact"], "UTF-8")
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
        <configuration>{compiler_cfg}<proc>none</proc><compilerArgs><arg>-Xlint:all,-options</arg></compilerArgs></configuration>
      </plugin>
      <plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-surefire-plugin</artifactId><version>3.5.6</version>
        <!-- Published sources jars carry no test suite; the compile and the consumers' own tests are the proof lane. -->
        <configuration><skipTests>true</skipTests></configuration>
      </plugin>
      <plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-jar-plugin</artifactId><version>3.5.0</version>
        <configuration><archive><manifestEntries>{("<Automatic-Module-Name>%s</Automatic-Module-Name>" % xml(amn)) if amn else "<Synexia-Source-Inline>no upstream Automatic-Module-Name</Synexia-Source-Inline>"}</manifestEntries></archive></configuration>
      </plugin>
    </plugins>
  </build>
</project>
"""
            (mdir / "pom.xml").write_text(pom, encoding="utf-8", newline="\n")
            verify = verify_template.replace('"META-INF/third-party/re2j/"', '"META-INF/third-party/%s/"' % module_dir(r)).replace('"Automatic-Module-Name: re2j"', '"Automatic-Module-Name: %s"' % amn if amn else '"Synexia-Source-Inline: no upstream Automatic-Module-Name"')
            (mdir / "verify_source.py").write_text(verify, encoding="utf-8", newline="\n")
            (mdir / ".gitattributes").write_text("src/** -text\nupstream-excluded/** -text\nLICENSE -text\nUPSTREAM_* -text\nSOURCES_MANIFEST.tsv -text\n", encoding="utf-8", newline="\n")
        notices = f"""# {r["artifact"]} {r["version"]} source inclusion

{r["artifact"]} is the work of its upstream authors and contributors. Copyright remains with the copyright
holders named in each unchanged source file and in the retained legal texts. This directory does not
relabel upstream implementation as Synexia-original.

Upstream: {r["scm"] or r["url"] or central}
Published sources jar: {central}{r["artifact"]}-{r["version"]}-sources.jar (SHA-1 {r["sources_sha1"]})
License as declared by the published pom: {lic_name}

The complete upstream license text is retained in `LICENSE` ({"from the sources jar" if in_jar_license else "fetched from the upstream tag; see UPSTREAM_LICENSE.json"}){" and packaged with the source-built JAR under META-INF/third-party/" + module_dir(r) if kind == "MODULE" else ""},
alongside this notice and `PROVENANCE.json`. Every file listed in `SOURCES_MANIFEST.tsv` retains its exact
upstream bytes and header. {"The Maven build descriptor and integration metadata are Synexia composition, licensed Apache-2.0; that license does not relicense the donor implementation." if kind == "MODULE" else "No build descriptor is generated: donor-superset status " + st + " (" + p["reason"] + ")."}
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
