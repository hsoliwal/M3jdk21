# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""lombok_census.py <stage_dir> <out_dir> <java> <LOMBOK_API.tsv> <LOMBOK_CONFIG_KEYS.tsv> <origin>... :
LOMBOK-CENSUS v1 (CPU, read-only on every origin).

origin  git:<name>=<repo>@<rev>@<pathspec>  Java blobs that contain the text "lombok" (git grep -l -F at <rev>) are
                                            read with git cat-file and staged; lombok.config / UPSTREAM_LOMBOK.config
                                            (and their .txt copies) come from the tree listing. GIT_NO_LAZY_FETCH=1:
                                            a blob that is not local is an error, never a network fetch (prefetch it
                                            in one batch first; see README).
        fs:<name>=<root>                    checked-out files on disk, listed by ripgrep (env M3_RG, else rg on PATH;
                                            .gitignore honoured); never git grep.
Steps: list -> stage -> LombokCensus.java (javac parser, one AST) -> config-line parse -> reduce.
Writes <stage_dir>/MANIFEST.tsv, OCCURRENCES.tsv, CONFIG_LINES.tsv and <out_dir>/USAGE_CENSUS.tsv. Rows are sorted;
a re-run over the same inputs is byte-identical."""
import collections
import hashlib
import os
import re
import shutil
import subprocess
import sys

CONFIG_NAME = re.compile(r"(^|/)(lombok\.config|UPSTREAM_LOMBOK\.config)(\.txt)*$")
CONFIG_LINE = re.compile(r"^\s*(?P<key>[A-Za-z][A-Za-z0-9.]*)\s*(?P<op>\+=|-=|=)\s*(?P<value>.*?)\s*$")
CONFIG_CLEAR = re.compile(r"^\s*clear\s+(?P<key>[A-Za-z][A-Za-z0-9.]*)\s*$")
COMMENT = "#"
NEEDLE = "lombok"
NONE = "-"
UNKNOWN = "UNKNOWN:"
SHA_SHORT = 12
GIT_ENV = dict(os.environ, GIT_NO_LAZY_FETCH="1")
VENDOR = ("m3", "vendor")
UPSTREAM_VARIANTS = ("upstream-lombok", "src")
INPUT_VARIANTS = {"upstream-lombok", "image", NONE}
MANIFEST_HEADER = "origin\trev\tmodule\tvariant\tpath\tsha\tstaged\n"
CONFIG_HEADER = "origin\tmodule\tpath\tline\tkey\top\tvalue\tknown\n"
CENSUS_HEADER = "section\torigin\titem\ttarget\tattributes\tfiles\toccurrences\tmodules\texample\n"
OCC = ("origin", "module", "variant", "path", "line", "kind", "item", "target", "attributes", "owner", "member")
CENSUS_TOOL = "LombokCensus.java"


NO_MATCH = 1


def run(args, cwd=None, data=None):
    """stdout of a listing/reading command; exit status 1 (grep/rg: nothing matched) is an empty result."""
    p = subprocess.run(args, cwd=cwd, input=data, capture_output=True, env=GIT_ENV)
    if p.returncode not in (0, NO_MATCH) or (p.returncode == NO_MATCH and p.stderr.strip()):
        raise SystemExit("command failed (%d): %s\n%s" % (p.returncode, " ".join(args), p.stderr.decode()[:400]))
    return p.stdout


def rel(path):
    p = path.replace("\\", "/")
    return p[2:] if p.startswith("./") else p


def module_of(origin, path):
    """m3/vendor/<area>/<module>/<variant>/... for M3JDK; the first three segments elsewhere."""
    parts = path.split("/")
    if tuple(parts[:2]) == VENDOR and len(parts) > 4:
        variant = parts[4] if parts[4] in UPSTREAM_VARIANTS else "image"
        return "/".join(parts[:4]), variant
    return "/".join(parts[:3]), NONE


def git_origin(name, spec, stage):
    repo, rev, pathspec = spec.split("@", 2)
    rev_id = run(["git", "-C", repo, "rev-parse", rev]).decode().strip()
    listed = run(["git", "-C", repo, "grep", "-l", "-F", NEEDLE, rev_id, "--", pathspec]).decode().splitlines()
    paths = sorted(p.split(":", 1)[1] for p in listed)
    names = run(["git", "-C", repo, "ls-tree", "-r", "--name-only", rev_id]).decode().splitlines()
    configs = sorted(p for p in names if CONFIG_NAME.search(p))
    blobs = cat_blobs(repo, rev_id, paths + configs)
    sources = []
    for p in paths:
        oid, data = blobs[p]
        staged = os.path.join(stage, name, oid[:SHA_SHORT], p.rsplit("/", 1)[-1])
        write_bytes(staged, data)
        module, variant = module_of(name, p)
        sources.append((name, rev_id[:SHA_SHORT], module, variant, p, oid, os.path.abspath(staged)))
    config_texts = [(name, module_of(name, p)[0], p, blobs[p][1]) for p in configs]
    return sources, config_texts


def cat_blobs(repo, rev, paths):
    request = "".join("%s:%s\n" % (rev, p) for p in paths).encode()
    raw = run(["git", "-C", repo, "cat-file", "--batch"], data=request)
    out, pos = {}, 0
    for p in paths:
        end = raw.index(b"\n", pos)
        header = raw[pos:end].decode().split()
        if len(header) < 3 or header[1] != "blob":
            raise SystemExit("blob not local (prefetch first): %s:%s -> %s" % (rev, p, " ".join(header)))
        size = int(header[2])
        out[p] = (header[0], raw[end + 1:end + 1 + size])
        pos = end + 1 + size + 1
    return out


def ripgrep():
    return os.environ.get("M3_RG") or shutil.which("rg") or "rg"


def fs_origin(name, root, stage):
    rg = ripgrep()
    listed = run([rg, "-l", "-F", NEEDLE, "-g", "*.java", "."], cwd=root).decode().splitlines()
    configs = run([rg, "--files", "--no-ignore", "--hidden", "-g", "lombok.config", "-g", "lombok.config.txt",
                   "-g", "!.git"], cwd=root).decode().splitlines()
    head = rev_of(root)
    sources = []
    for p in sorted(rel(x) for x in listed):
        data = read_bytes(os.path.join(root, p))
        module, variant = module_of(name, p)
        sources.append((name, head, module, variant, p, "sha256:" + hashlib.sha256(data).hexdigest()[:SHA_SHORT],
                        os.path.abspath(os.path.join(root, p))))
    config_texts = [(name, module_of(name, c)[0], c, read_bytes(os.path.join(root, c)))
                    for c in sorted(rel(x) for x in configs)]
    return sources, config_texts


def rev_of(root):
    try:
        return run(["git", "-C", root, "rev-parse", "--short=12", "HEAD"]).decode().strip()
    except subprocess.CalledProcessError:
        return NONE


def read_bytes(path):
    with open(path, "rb") as f:
        return f.read()


def write_bytes(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(data)


def config_rows(config_texts, known):
    rows = []
    for origin, module, path, data in config_texts:
        for n, line in enumerate(data.decode("utf-8", "replace").splitlines(), 1):
            if not line.strip() or line.lstrip().startswith(COMMENT):
                continue
            m, c = CONFIG_LINE.match(line), CONFIG_CLEAR.match(line)
            key, op, value = (m.group("key"), m.group("op"), m.group("value")) if m else \
                (c.group("key"), "clear", NONE) if c else (line.strip(), "?", NONE)
            rows.append((origin, module, path, str(n), key, op, value, "1" if key in known else "0"))
    return rows


def read_tsv(path):
    with open(path, encoding="utf-8") as f:
        next(f)
        return [line.rstrip("\n").split("\t") for line in f]


def reduce(occurrences, configs, sources):
    groups = collections.defaultdict(lambda: [set(), 0, set(), None])
    for r in occurrences:
        o = dict(zip(OCC, r))
        if o["kind"] == "PARSE_ERROR":
            continue
        key = (o["kind"], o["origin"], o["item"], o["target"], o["attributes"])
        g = groups[key]
        g[0].add(o["path"])
        g[1] += 1
        g[2].add(o["module"])
        example = "%s:%s" % (o["path"], o["line"])
        g[3] = example if g[3] is None else min(g[3], example)
    for origin, module, path, line, k, op, value, known in configs:
        g = groups[("CONFIG_KEY", origin, k if known == "1" else UNKNOWN + k, op, value)]
        g[0].add(path)
        g[1] += 1
        g[2].add(module)
        example = "%s:%s" % (path, line)
        g[3] = example if g[3] is None else min(g[3], example)
    rows = [(k[0], k[1], k[2], k[3], k[4], str(len(g[0])), str(g[1]), str(len(g[2])), g[3])
            for k, g in groups.items()]
    return sorted(rows) + module_rows(occurrences, sources)


def module_rows(occurrences, sources):
    per = collections.defaultdict(lambda: [set(), set(), 0, 0, None])
    for s in sources:
        per[(s[0], s[2], s[3])][0].add(s[4])
        e = per[(s[0], s[2], s[3])]
        e[4] = s[4] if e[4] is None else min(e[4], s[4])
    for r in occurrences:
        o = dict(zip(OCC, r))
        e = per[(o["origin"], o["module"], o["variant"])]
        if o["kind"] == "ANNOTATION":
            e[1].add(o["path"])
            e[2] += 1
        elif o["kind"] == "PARSE_ERROR":
            e[3] += 1
    return sorted(("MODULE", k[0], k[1], k[2], "lombok_text_files=%d;parse_errors=%d" % (len(e[0]), e[3]),
                   str(len(e[1])), str(e[2]), "1", e[4] or NONE) for k, e in per.items())


def main(argv):
    stage, out_dir, java, api, keys = argv[1:6]
    known = {line.strip() for line in open(keys, encoding="utf-8")} - {"config_key"}
    sources, config_texts = [], []
    for origin in argv[6:]:
        kind, rest = origin.split(":", 1)
        name, spec = rest.split("=", 1)
        s, c = git_origin(name, spec, stage) if kind == "git" else fs_origin(name, spec, stage)
        sources += s
        config_texts += c
    manifest = os.path.join(stage, "MANIFEST.tsv")
    with open(manifest, "w", encoding="utf-8", newline="\n") as f:
        f.write(MANIFEST_HEADER)
        f.writelines("\t".join(s) + "\n" for s in sources)
    occ_path = os.path.join(stage, "OCCURRENCES.tsv")
    tool = os.path.join(os.path.dirname(os.path.abspath(__file__)), CENSUS_TOOL)
    print(subprocess.run([java, tool, manifest, api, occ_path], check=True, capture_output=True,
                         text=True).stdout.strip())
    configs = config_rows(config_texts, known)
    with open(os.path.join(stage, "CONFIG_LINES.tsv"), "w", encoding="utf-8", newline="\n") as f:
        f.write(CONFIG_HEADER)
        f.writelines("\t".join(r) + "\n" for r in configs)
    rows = reduce(read_tsv(occ_path), configs, sources)
    os.makedirs(out_dir, exist_ok=True)
    with open(os.path.join(out_dir, "USAGE_CENSUS.tsv"), "w", encoding="utf-8", newline="\n") as f:
        f.write(CENSUS_HEADER)
        f.writelines("\t".join(r) + "\n" for r in rows)
    sections = collections.Counter(r[0] for r in rows)
    print("USAGE_CENSUS rows=%d %s sources=%d configs=%d" % (len(rows), dict(sorted(sections.items())),
                                                              len(sources), len(config_texts)))


if __name__ == "__main__":
    main(sys.argv)
