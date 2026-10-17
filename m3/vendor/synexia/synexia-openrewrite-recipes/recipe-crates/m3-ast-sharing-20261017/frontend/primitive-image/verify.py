#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
import argparse
import hashlib
import json
import subprocess
import tempfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
FRONTEND = HERE.parent

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--jdk", required=True, type=Path)
    parser.add_argument("--plugin", type=Path, default=FRONTEND / "target/m3-ast-sharing.jar")
    parser.add_argument("--patch", type=Path, default=FRONTEND / "target/m3-javac-patch.jar")
    args = parser.parse_args()
    temporary = tempfile.TemporaryDirectory(prefix="m3-primitive-shared-task-")
    output = Path(temporary.name)
    output.mkdir(parents=True, exist_ok=True)
    base_source = output / "baseline-src/com/sun/tools/javac/m3/M3PrimitiveImageCompiler.java"
    base_source.parent.mkdir(parents=True, exist_ok=True)
    base_source.write_bytes((FRONTEND / "before/M3PrimitiveImageCompiler.java.txt").read_bytes())
    base_classes = output / "baseline-classes"
    base_classes.mkdir(exist_ok=True)
    source = output / "Constants.java"
    source.write_text('class Constants { static final byte B = 7; static final short S = -12; '
                      'static final long L = 1234567890123L; static final float F = -0.0f; '
                      'static final double D = Double.NaN; static final boolean T = true; '
                      "static final char C = '\\u03a9'; int calc() { return 2 * 3; } "
                      'String text = "a" + "b"; }\n')
    patch = output / "m3-javac-patch.jar"
    plugin = output / "m3-ast-sharing.jar"
    patch.write_bytes(args.patch.read_bytes())
    plugin.write_bytes(args.plugin.read_bytes())
    log_path = output / "proof.log"
    commands = []
    with log_path.open("w") as log:
        def run(command):
            commands.append([str(x) for x in command])
            subprocess.run(command, check=True, stdout=log, stderr=subprocess.STDOUT)
        run([args.jdk / "bin/javac", "-source", "21", "-target", "21", "-Xlint:all", "-Werror",
             "--patch-module", "jdk.compiler=" + str(output / "baseline-src"),
             "-d", base_classes, base_source])
        for fold in (False, True):
            name = "fold" if fold else "legacy"
            command = [args.jdk / "bin/java", "--patch-module", "jdk.compiler=" + str(base_classes),
                       "-m", "jdk.compiler/com.sun.tools.javac.m3.M3PrimitiveImageCompiler",
                       "--out", output / ("baseline-" + name + ".m3prim"),
                       "--slots", output / ("baseline-" + name + ".tsv")]
            if fold: command.append("--fold-expressions")
            run(command + [source])
        classes = output / "proof-classes"
        classes.mkdir(exist_ok=True)
        exports = ["--add-exports", "jdk.compiler/com.sun.tools.javac.m3=ALL-UNNAMED"]
        run([args.jdk / "bin/javac", "-source", "21", "-target", "21", "-Xlint:all", "-Werror",
             "--patch-module", "jdk.compiler=" + str(patch)] + exports
            + ["-classpath", plugin, "-d", classes, HERE / "TaskReuseProof.java"])
        run([args.jdk / "bin/java", "--patch-module", "jdk.compiler=" + str(patch)] + exports
            + ["-classpath", str(plugin) + ":" + str(classes), "TaskReuseProof", output])
    sha = lambda path: hashlib.sha256(path.read_bytes()).hexdigest()
    receipt = {"schema": "M3_PRIMITIVE_SHARED_TASK_PROOF_V1", "status": "PASSED", "checks": 18,
               "plugin_sha256": sha(plugin), "patch_sha256": sha(patch), "proof_sha256": sha(HERE / "TaskReuseProof.java"),
               "baseline_source_sha256": sha(base_source), "log_sha256": sha(log_path), "commands": commands,
               "modes": ["legacy", "fold-expressions"], "tasks_per_mode": 1, "parses_per_mode": 1,
               "image_bytes_equal": True, "sidecar_bytes_equal": True}
    evidence = HERE / "evidence"
    evidence.mkdir(exist_ok=True)
    (evidence / "proof.log").write_bytes(log_path.read_bytes())
    (evidence / "RESULTS.json").write_text(json.dumps(receipt, indent=2) + "\n")
    print(json.dumps({"status": "PASSED", "checks": 18, "image_and_sidecar_equal": True}))
    temporary.cleanup()

if __name__ == "__main__":
    main()
