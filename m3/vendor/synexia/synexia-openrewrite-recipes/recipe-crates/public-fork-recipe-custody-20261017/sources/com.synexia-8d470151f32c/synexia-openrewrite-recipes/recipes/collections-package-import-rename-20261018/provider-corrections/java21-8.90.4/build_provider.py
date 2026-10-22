#!/usr/bin/env python3
"""Build the hash-pinned parser correction without changing the original provider."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import zipfile


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--jdk", type=Path, required=True)
    parser.add_argument("--classpath-file", type=Path, required=True)
    parser.add_argument("--original-jar", type=Path, required=True)
    parser.add_argument("--output-directory", type=Path, required=True)
    args = parser.parse_args()
    root = Path(__file__).resolve().parent
    provenance = json.loads((root / "PROVENANCE.json").read_text())
    sources = [root / row["path"] for row in provenance["sources"]]
    for source, row in zip(sources, provenance["sources"]):
        if digest(source) != row["corrected_source_sha256"]:
            raise SystemExit("Corrected provider source hash mismatch: " + str(source))
    if digest(args.original_jar) != provenance["original_jar_sha256"]:
        raise SystemExit("Original official provider jar hash mismatch")
    output = args.output_directory.resolve()
    output.mkdir(parents=True, exist_ok=True)
    classes = output / "classes"
    if classes.exists():
        raise SystemExit("Use a fresh output directory; stale classes are refused")
    classes.mkdir()
    command = [str(args.jdk / "bin/javac"), "-J-Xmx512m", "-source", "21", "-target", "21",
               "-proc:none", "-cp", args.classpath_file.read_text().strip(), "-d", str(classes)]
    for package in ("api", "code", "comp", "file", "jvm", "main", "model", "parser",
                    "processing", "tree", "util"):
        command += ["--add-exports", "jdk.compiler/com.sun.tools.javac." + package + "=ALL-UNNAMED"]
    subprocess.run(command + [str(source) for source in sources], check=True)
    jar = output / "rewrite-java-21-8.90.4-m3-varargs-doc-anchors-20261018.jar"
    replacements = {path.relative_to(classes).as_posix(): path.read_bytes()
                    for path in classes.rglob("*.class")}
    with zipfile.ZipFile(args.original_jar) as original, zipfile.ZipFile(jar, "w", zipfile.ZIP_DEFLATED) as result:
        for info in original.infolist():
            result.writestr(info, replacements.pop(info.filename, original.read(info)))
        if replacements:
            raise SystemExit("Unexpected new visitor class entries: " + repr(sorted(replacements)))
    receipt = dict(provenance)
    receipt["actual_corrected_jar_sha256"] = digest(jar)
    receipt["build_command"] = command + [str(source) for source in sources]
    (output / "BUILD_RECEIPT.json").write_text(json.dumps(receipt, indent=2) + "\n")
    if digest(jar) != provenance["corrected_jar_sha256"]:
        raise SystemExit("Jar bytes differ from qualified build; inspect BUILD_RECEIPT.json before use")
    print(jar)


if __name__ == "__main__":
    main()
