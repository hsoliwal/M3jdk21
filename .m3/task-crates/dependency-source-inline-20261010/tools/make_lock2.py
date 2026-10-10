#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""C6 DEPENDENCY-SOURCE-INLINE, phase COMPOSE (attribution lock): write the donor-superset lock in
the develop format (SYNEXIA-DONOR-ATTRIBUTION-1: license + licenseSha256 + every conventionally named
legal file declared as a notice + upstreamRepository/copyright/licenseName/reuseDisposition) for every
inventoried artifact.

* Donors whose published sources jar carries its license: kind=zip, repository = the Maven Central
  sources jar URL, revision = SHA-256 of the jar bytes.
* Donors whose sources jar carries no license: a composed archive (every jar entry byte-for-byte plus
  UPSTREAM_LICENSE.* and UPSTREAM_LICENSE.json fetched from the upstream tag) is written under
  --composed and referenced by file: URI; COMPOSED_ARCHIVES.tsv records jar sha256, license origin and
  archive sha256 so the composition is reproducible.
* requires = the import closure of the image resolved against the package index of all images
  (same-version sibling preferred, then the pom-declared version, then the newest); pom compile deps
  are unioned in. Unresolvable packages (not in any image, not JDK) go to UNRESOLVED_IMPORTS.tsv.
* build: none (Lombok at source level), commands (ModuleCompile helper: module descriptor /
  multi-release sources excluded, legacy source level, extra binary classpath), else java.
CPU only; no network.
"""
from __future__ import annotations
import argparse, csv, hashlib, io, json, os, re, sys, zipfile
from pathlib import Path

CENTRAL = "https://repo.maven.apache.org/maven2/"
LEGAL_RE = re.compile(r"(?i)(.*[-_.])?(license|licence|notice|copying|copyright)([._-].*)?$")  # superset of the runner rule: also *-LICENSE
JDK_PACKAGES = set(io.open(Path(__file__).with_name("jdk21_packages.txt"), encoding="utf-8").read().split())
RELEASE = {"caffeine": 11, "tomlj": 11, "antlr4-runtime": 11, "snakeyaml-engine": 11, "ecj": 17, "commonmark": 11, "fastdoubleparser": 21, "jackson-core": 21, "jackson-databind": 21, "jackson-dataformat-yaml": 21, "jackson-datatype-jsr310": 21, "lucene-core": 11, "lucene-queries": 11, "lucene-sandbox": 11,
           "lucene-analysis-common": 11, "lucene-queryparser": 11}
LEGACY = {  # sun.misc.Unsafe / sun.misc.Signal / sun.nio.ch.DirectBuffer are not visible under --release 8 on JDK 21
    "zero-allocation-hashing": "legacy:8", "guava": "legacy:8", "eclipse-collections": "legacy:8", "agrona": "legacy:8", "lz4-java": "legacy:8", "asm": "legacy:8", "jspecify": "legacy:8"}  # jspecify: ElementType.MODULE is Java 9 API  # asm: @Deprecated(forRemoval) is Java 9 API
TIMEOUT = {"eclipse-collections": 1800, "eclipse-collections-api": 1800, "lucene-core": 1200, "jackson-databind": 900, "guava": 900, "ecj": 1200}
EXTRA_BINARY_CLASSPATH = {  # donor id -> store-relative BINARY build inputs for optional/provided adapters (BUILD_INPUTS.tsv; never inlined)
    "ecj-3.46.100": ["org/apache/ant/ant/1.10.14/ant-1.10.14.jar"],
    "cglib-3.3.0": ["org/apache/ant/ant/1.10.14/ant-1.10.14.jar"],
    "commons-logging-1.3.2": ["avalon-framework/avalon-framework/4.1.5/avalon-framework-4.1.5.jar", "logkit/logkit/2.0/logkit-2.0.jar",
                              "org/apache/logging/log4j/log4j-api/2.23.1/log4j-api-2.23.1.jar", "org/apache/logging/log4j/log4j-1.2-api/2.23.1/log4j-1.2-api-2.23.1.jar",
                              "javax/servlet/javax.servlet-api/4.0.1/javax.servlet-api-4.0.1.jar"],
    "org.osgi.core-6.0.0": ["org/osgi/org.osgi.annotation/6.0.0/org.osgi.annotation-6.0.0.jar"],
    "commons-compress-1.27.1": ["com/github/luben/zstd-jni/1.5.6-4/zstd-jni-1.5.6-4.jar"],  # zstd-jni cannot build from its sources jar (generated ZstdVersion); optional codec input stays binary
}
REQUIRES_DROP = {"commons-compress-1.27.1": {"zstd-jni-1.5.6-4"}}  # served by EXTRA_BINARY_CLASSPATH instead
ENCODING = {"commons-compress": "ISO-8859-1"}  # one Latin-1 comment byte (TarArchiveOutputStream.java); every other file is ASCII-safe under both charsets
REQUIRES_OVERRIDE = {  # requirer -> {(group, artifact): donor id}; shaded inputs whose version the reduced pom no longer states
    "jackson-core-2.15.3": {("ch.randelshofer", "fastdoubleparser"): "fastdoubleparser-0.9.0"},
    "jackson-core-2.16.1": {("ch.randelshofer", "fastdoubleparser"): "fastdoubleparser-1.0.0"},
    "jackson-core-2.19.2": {("ch.randelshofer", "fastdoubleparser"): "fastdoubleparser-1.0.90"},
}
BUILD_NONE = {  # artifact -> reason: retained as SOURCE_ONLY (no compile lane without its platform)
    "org.eclipse.jdt.core": "ECLIPSE_PLATFORM_DEPENDENCIES (org.eclipse.core.resources/runtime/text etc. are not inventoried)",
}
LICENSE_NAME_OVERRIDE = {  # pom lacks <licenses>; taken from the parent pom fetched into store/parents
    "zero-allocation-hashing": "Apache License, Version 2.0 (upstream LICENSE at tag zero-allocation-hashing-0.16; parent net.openhft:java-parent-pom:1.1.28 declares none)",
    "dec": "MIT License (parent pom org.brotli:parent:0.1.2)",
    "failureaccess": "The Apache Software License, Version 2.0 (parent pom com.google.guava:guava-parent:26.0-android)",
    "antlr4-runtime": "BSD-3-Clause (parent pom org.antlr:antlr4-master:4.11.1)",
    "cglib": "Apache License, Version 2.0 (META-INF/LICENSE in the sources jar; parent cglib:cglib-parent:3.3.0)",
}


def vkey(v: str):
    return tuple(int(x) if x.isdigit() else -1 for x in re.split(r"[.\-]", v))


def donor_id(a: str, v: str) -> str:
    return (a + "-" + v).replace("_", "-")


def manifest(image: Path) -> dict:
    out = {}
    for m in csv.DictReader(io.open(image / "SOURCES_MANIFEST.tsv", encoding="utf-8", newline=""), delimiter="\t"):
        out[m["path"]] = (int(m["bytes"]), m["sha256"])
    return out


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def java_files(image: Path):
    for dp, _, fs in os.walk(image):
        for f in fs:
            if f.endswith(".java"):
                rel = os.path.relpath(os.path.join(dp, f), image).replace("\\", "/")
                yield rel


IMPORT_RE = re.compile(r"^import\s+(static\s+)?([A-Za-z_$][\w$]*(?:\.[A-Za-z_$][\w$]*)*)(?:\.\*)?\s*;", re.M)


def imports_of(image: Path):
    pk, ext = set(), set()
    for rel in java_files(image):
        if rel.endswith("module-info.java") or rel.startswith("META-INF/versions/"):
            continue
        pk.add(rel.rsplit("/", 1)[0].replace("/", ".") if "/" in rel else "")
        text = io.open(image / rel, encoding="utf-8", errors="replace").read()
        for m in IMPORT_RE.finditer(text):
            name = m.group(2)
            if m.group(1):  # static: drop member
                name = name.rsplit(".", 1)[0]
            ext.add(name)
    return pk, ext


def normalize_upstream(raw: str) -> str:
    """scm/url from the pom -> plain https URL without query/userinfo (the runner's attribution rule)."""
    u = raw.strip()
    for prefix in ("scm:git:", "scm:svn:", "scm:"):
        if u.startswith(prefix):
            u = u[len(prefix):]
    if u.startswith("git@github.com:"):
        u = "https://github.com/" + u[len("git@github.com:"):]
    if "://" in u:
        u = "https://" + u.rsplit("://", 1)[1]
    else:
        u = "https://" + u
    u = re.sub(r"\?p=([^&;]+).*$", r"/\1", u)  # gitbox.apache.org/repos/asf?p=name -> /repos/asf/name
    u = u.split("?", 1)[0].split("#", 1)[0]
    if u.endswith(".git"):
        u = u[:-4]
    return u


def copyright_line(image: Path, legal: list, upstream: str) -> str:
    for p in legal:
        if "NOTICE" in p.upper():
            for line in io.open(image / p, encoding="utf-8", errors="replace").read().splitlines()[:12]:
                if "copyright" in line.lower():
                    return line.strip()[:200]
    counts = {}
    for n, rel in enumerate(java_files(image)):
        if n > 60:
            break
        for line in io.open(image / rel, encoding="utf-8", errors="replace").read().splitlines()[:30]:
            if "copyright" in line.lower() and "(c)" in line.lower() or line.lower().strip().startswith(("* copyright", "copyright")):
                key = re.sub(r"^[\s*/#]+", "", line).strip()[:160]
                counts[key] = counts.get(key, 0) + 1
    if counts:
        return max(counts.items(), key=lambda kv: kv[1])[0]
    return "Copyright the upstream authors of " + upstream + ", as stated in the pinned source"


def license_name(r: dict, image: Path, lic_path: str) -> str:
    if r["artifact"] in LICENSE_NAME_OVERRIDE:
        return LICENSE_NAME_OVERRIDE[r["artifact"]]
    if not r["licenses"].startswith("(none"):
        return r["licenses"].split(";")[0].split(" <")[0].strip()
    head = io.open(image / lic_path, encoding="utf-8", errors="replace").read(400).lower()
    for k, v in (("apache license", "Apache License, Version 2.0"), ("mit license", "MIT License"), ("bsd", "BSD License"), ("eclipse public", "Eclipse Public License")):
        if k in head:
            return v + " (from the license text; pom declares none)"
    return "UNDECLARED (see license text)"


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--inventory", type=Path, nargs="+", required=True)
    ap.add_argument("--store", type=Path, required=True)
    ap.add_argument("--composed", type=Path, required=True)
    ap.add_argument("--helper", type=Path, required=True)
    ap.add_argument("--lock", type=Path, required=True)
    ap.add_argument("--ledger-dir", type=Path, required=True)
    ap.add_argument("--max-files", type=int, default=20000)
    ap.add_argument("--max-bytes", type=int, default=512 * 1024 * 1024)
    ap.add_argument("--only", default="", help="comma-separated donor ids; the lock keeps them plus their requires closure")
    args = ap.parse_args()
    args.composed.mkdir(parents=True, exist_ok=True)
    rows = []
    for inv in args.inventory:
        rows += [r for r in csv.DictReader(io.open(inv, encoding="utf-8", newline=""), delimiter="\t") if r["status"].endswith("OK")]
    ids = {donor_id(r["artifact"], r["version"]): r for r in rows}
    # package index + imports per donor (one walk per image)
    pk_index, imports, own = {}, {}, {}
    for did, r in ids.items():
        pk, ext = imports_of(Path(r["sources_dir"]))
        own[did] = pk
        imports[did] = ext
        for p in pk:
            pk_index.setdefault(p, []).append(did)
    by_ga = {}
    for did, r in ids.items():
        by_ga.setdefault((r["group"], r["artifact"]), []).append(did)

    def pick(cands: list, requirer: dict) -> str:
        forced = REQUIRES_OVERRIDE.get(donor_id(requirer["artifact"], requirer["version"]), {})
        for c in cands:
            if forced.get((ids[c]["group"], ids[c]["artifact"])) == c:
                return c
        same = [c for c in cands if ids[c]["version"] == requirer["version"]]
        if same:
            return same[0]
        for dep in requirer["compile_deps"].split(";"):
            parts = dep.split(":")
            if len(parts) >= 3:
                for c in cands:
                    if ids[c]["group"] == parts[0] and ids[c]["artifact"] == parts[1] and ids[c]["version"] == parts[2]:
                        return c
        return sorted(cands, key=lambda c: vkey(ids[c]["version"]))[-1]

    unresolved, lines, composed_rows, ledger = [], [], [], []
    for did, r in ids.items():
        image = Path(r["sources_dir"])
        man = manifest(image)
        legal = sorted(p for p in man if LEGAL_RE.match(p.rsplit("/", 1)[-1]))
        upstream_files = sorted(p for p in os.listdir(image) if p.startswith("UPSTREAM_") and not p.endswith(".json"))
        in_jar_license = [p for p in legal if re.search(r"(?i)^(META-INF/)?(LICENSE|COPYING)", p)]
        composed = not in_jar_license
        if composed:
            lic_path = next((p for p in upstream_files if p.startswith("UPSTREAM_LICENSE.")), "")
            if not lic_path:
                ledger.append((did, "EXCLUDED", "NO_LICENSE_TEXT", "", "", ""))
                continue
        else:
            lic_path = in_jar_license[0]
        notices = [p for p in legal if p != lic_path] + [p for p in upstream_files if p != lic_path]
        jar = args.store / r["group"].replace(".", "/") / r["artifact"] / r["version"] / (r["artifact"] + "-" + r["version"] + "-sources.jar")
        jar_sha = sha256(jar)
        if composed:
            archive = args.composed / (did + ".zip")
            with zipfile.ZipFile(jar) as src, zipfile.ZipFile(archive, "w", zipfile.ZIP_DEFLATED) as dst:
                for info in sorted(src.infolist(), key=lambda i: i.filename):
                    if info.is_dir():
                        continue
                    zi = zipfile.ZipInfo(info.filename, date_time=(1980, 1, 1, 0, 0, 0))
                    zi.compress_type = zipfile.ZIP_DEFLATED
                    dst.writestr(zi, src.read(info))
                for extra in sorted(p for p in os.listdir(image) if p.startswith("UPSTREAM_")):
                    zi = zipfile.ZipInfo(extra, date_time=(1980, 1, 1, 0, 0, 0))
                    zi.compress_type = zipfile.ZIP_DEFLATED
                    dst.writestr(zi, (image / extra).read_bytes())
            revision = sha256(archive)
            repository = archive.resolve().as_uri()
            meta = json.loads((image / "UPSTREAM_LICENSE.json").read_text(encoding="utf-8"))
            composed_rows.append((did, jar_sha, meta.get("origin", ""), meta.get("caveat", "EXACT_TAG"), sha256(image / lic_path), revision, repository))
        else:
            revision = jar_sha
            repository = CENTRAL + r["group"].replace(".", "/") + "/" + r["artifact"] + "/" + r["version"] + "/" + r["artifact"] + "-" + r["version"] + "-sources.jar"
        lic_sha = man[lic_path][1] if lic_path in man else sha256(image / lic_path)
        # requires: import closure + pom deps
        req = set()
        for name in imports[did]:
            parts = name.split(".")
            hit = None
            for n in range(len(parts), 0, -1):
                prefix = ".".join(parts[:n])
                if prefix in own[did]:
                    hit = "own"; break
                if prefix in JDK_PACKAGES:
                    hit = "jdk"; break
                if prefix in pk_index:
                    cands = [c for c in pk_index[prefix] if c != did]
                    hit = pick(cands, r) if cands else "own"; break
            if hit is None:
                unresolved.append((did, name))
            elif hit not in ("own", "jdk"):
                req.add(hit)
        req -= REQUIRES_DROP.get(did, set())
        for dep in r["compile_deps"].split(";"):
            parts = dep.split(":")
            if len(parts) >= 3 and (parts[0], parts[1]) in by_ga:
                cands = [c for c in by_ga[(parts[0], parts[1])] if c != did]
                if cands:
                    req.add(pick(cands, r))
        req -= REQUIRES_DROP.get(did, set())
        lombok = any(io.open(image / f, encoding="utf-8", errors="replace").read().find("import lombok") >= 0 for f in list(java_files(image))[:400]) if r["artifact"].startswith("rewrite") else False
        has_module = any(f.endswith("module-info.java") or f.startswith("META-INF/versions/") for f in java_files(image))
        signed = any(x.startswith("META-INF/") and x.endswith((".SF", ".RSA", ".DSA", ".EC")) for x in man)
        release = RELEASE.get(r["artifact"], 8)
        legacy = LEGACY.get(r["artifact"])
        extra_cp = EXTRA_BINARY_CLASSPATH.get(did, [])
        if lombok or r["artifact"] in BUILD_NONE:
            build, reason = "none", BUILD_NONE.get(r["artifact"], "LOMBOK_AT_SOURCE_LEVEL")
            req = set()
        elif has_module or legacy or extra_cp or signed:
            build, reason = "commands", ";".join(x for x in ("MODULE_DESCRIPTOR_OR_MULTI_RELEASE" if has_module else "", "LEGACY_SOURCE_LEVEL" if legacy else "", "EXTRA_BINARY_CLASSPATH" if extra_cp else "", "SIGNED_SOURCES_JAR" if signed else "") if x)
        else:
            build, reason = "java", ""
        upstream = normalize_upstream(r["scm"] or r["url"] or (CENTRAL + r["group"].replace(".", "/") + "/" + r["artifact"] + "/" + r["version"] + "/"))
        lic_name = license_name(r, image, lic_path)
        cp_line = copyright_line(image, legal, upstream)
        lines += ["donor.%s.kind=zip" % did, "donor.%s.repository=%s" % (did, repository), "donor.%s.revision=%s" % (did, revision),
                  "donor.%s.license=%s" % (did, lic_path), "donor.%s.licenseSha256=%s" % (did, lic_sha),
                  "donor.%s.upstreamRepository=%s" % (did, upstream),
                  "donor.%s.copyright=%s" % (did, cp_line.replace("\\", "\\\\").replace(":", "\\:").replace("=", "\\=")),
                  "donor.%s.licenseName=%s" % (did, lic_name.replace(":", "\\:").replace("=", "\\=")),
                  "donor.%s.reuseDisposition=%s" % (did, "source-build"),  # the runner accepts only source-build; build=none donors are still reported SOURCE_ONLY
                  "donor.%s.notices=%d" % (did, len(notices))]
        for n, p in enumerate(notices):
            lines += ["donor.%s.notice.%d.path=%s" % (did, n, p), "donor.%s.notice.%d.sha256=%s" % (did, n, man[p][1] if p in man else sha256(image / p))]
        lines += ["donor.%s.capabilities=%s" % (did, did),
                  "donor.%s.requires=%s" % (did, ",".join(sorted(req))), "donor.%s.build=%s" % (did, build), "donor.%s.source=." % did,
                  "donor.%s.release=%d" % (did, release), "donor.%s.timeoutSeconds=%d" % (did, TIMEOUT.get(r["artifact"], 600))]
        if r["artifact"] in ENCODING:
            lines.append("donor.%s.encoding=%s" % (did, ENCODING[r["artifact"]]))
        if build == "commands":
            argv = ["${java}", str(args.helper.resolve()), "${source}", "${output}", legacy or str(release), ENCODING.get(r["artifact"], "UTF-8"), "${dependencies}"] + [str((args.store / p).resolve()) for p in extra_cp]
            lines += ["donor.%s.steps=1" % did, "donor.%s.step.0.argc=%d" % (did, len(argv))]
            lines += ["donor.%s.step.0.arg.%d=%s" % (did, i, a.replace("\\", "\\\\").replace(":", "\\:")) for i, a in enumerate(argv)]
            lines += ["donor.%s.artifacts=donor.jar" % did]
        ledger.append((did, "LOCKED", build, reason, "composed" if composed else "central", ",".join(sorted(req))))
    donors = [l[0] for l in ledger if l[1] == "LOCKED"]
    if args.only:
        req_of = {l[0]: [x for x in l[5].split(",") if x] for l in ledger}
        keep, pending = set(), [x.strip() for x in args.only.split(",") if x.strip()]
        while pending:
            d = pending.pop()
            if d in keep:
                continue
            keep.add(d); pending += req_of.get(d, [])
        donors = [d for d in donors if d in keep]
        lines = [ln for ln in lines if any(ln.startswith("donor.%s." % d) for d in keep)]
    text = "\n".join(["donors=" + ",".join(donors), "maxFiles=%d" % args.max_files, "maxBytes=%d" % args.max_bytes] + lines) + "\n"
    args.lock.write_bytes(text.encode("utf-8"))
    args.ledger_dir.mkdir(parents=True, exist_ok=True)
    (args.ledger_dir / "LOCK_PLAN.tsv").write_bytes(("\n".join(["donor\tstate\tbuild\treason\tsource\trequires"] + ["\t".join(l) for l in ledger]) + "\n").encode("utf-8"))
    (args.ledger_dir / "COMPOSED_ARCHIVES.tsv").write_bytes(("\n".join(["donor\tsources_jar_sha256\tlicense_origin\tlicense_caveat\tlicense_sha256\tarchive_sha256\trepository"] + ["\t".join(c) for c in composed_rows]) + "\n").encode("utf-8"))
    (args.ledger_dir / "UNRESOLVED_IMPORTS.tsv").write_bytes(("\n".join(["donor\tpackage_or_type"] + ["\t".join(u) for u in sorted(set(unresolved))]) + "\n").encode("utf-8"))
    print("lock donors", len(donors), "composed", len(composed_rows), "unresolved-imports", len(set(unresolved)), "lock sha256", hashlib.sha256(text.encode("utf-8")).hexdigest())
    return 0


if __name__ == "__main__":
    sys.exit(main())
