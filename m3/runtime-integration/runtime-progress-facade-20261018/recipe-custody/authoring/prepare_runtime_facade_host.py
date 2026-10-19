#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Prepare a separate original-host successor for recipe qualification.

This emits inputs for the original Maven snapshot recipes. It never modifies
the frozen host, its tests, its four engines or any receiving product source.
"""
from pathlib import Path
import argparse
import hashlib
import importlib.util
import json
import shutil

PREFIX = Path("synexia-openrewrite-recipes/recipe-crates/m3-runtime-progress-facade-20261018")
CONTROLS = ("prepare.py", "receive.py", "ENGINE_SOURCES.tsv",
    "src/test/java/com/synexia/rewrite/M3TaskRecipeTest.java",
    "contract/test_prepare.py", "contract/verify_receiver.py")
POM_SHA = "4c90faaa9e64aebeeba14fb2196731c6334293781d0fdcafbf190f2f5662eb7d"
COORDINATE_PATH = "com/synexia/sourcebuilt/openrewrite/rewrite-java-21/8.17.1-m3-javadoc-1/rewrite-java-21-8.17.1-m3-javadoc-1.jar"


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def once(text, before, after):
    if text.count(before) != 1:
        raise ValueError("original host insertion anchor drift")
    return text.replace(before, after, 1)


def main():
    parser = argparse.ArgumentParser()
    for name in ("original-crate", "recipe-owner", "parser-proof", "facade-candidates", "facade-baseline", "output"):
        parser.add_argument("--" + name, type=Path, required=True)
    args = parser.parse_args()
    if args.output.exists():
        raise ValueError("a fresh host input directory is required")
    origin = args.original_crate.resolve()
    if sha(origin / "pom.xml") != POM_SHA:
        raise ValueError("frozen capped host POM drift")
    parser_proof = json.loads(args.parser_proof.read_text())
    if parser_proof.get("status") != "PASS":
        raise ValueError("source-built parser qualification has not passed")
    artifact_sha = parser_proof["artifact_sha256"]
    spec = importlib.util.spec_from_file_location("original_host_successor_receiver", origin / "receive.py")
    receiver = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(receiver)
    qualified_parser = receiver.qualification(origin, "m3-openrewrite-javadoc", args.recipe_owner.resolve())
    if parser_proof["sdk_inputs_sha256"] != sha(qualified_parser["candidate"] / "SDK_INPUTS.tsv"):
        raise ValueError("parser proof source binding drift")
    destination = args.output / PREFIX
    for relative in CONTROLS:
        target = destination / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(origin / relative, target)
    shutil.copytree(origin / "datasets/fixture", destination / "datasets/fixture")
    for relative, data in qualified_parser["outputs"].items():
        target = destination / "parser-source" / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(data)
    for name in ("HARNESS_LINEAGE.json", "HOST_CAP_LINEAGE.json"):
        target = destination / "history" / name
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(origin / name, target)
    original_pom = (origin / "pom.xml").read_text()
    pom = once(original_pom,
        "<artifactId>m3-collections-contract-refinement-recipe-proof</artifactId>",
        "<artifactId>m3-runtime-progress-facade-recipe-proof</artifactId>")
    pom = once(pom,
        "<dependency><groupId>org.openrewrite</groupId><artifactId>rewrite-java-21</artifactId><version>${rewrite.version}</version><scope>runtime</scope></dependency>",
        "<dependency><groupId>com.synexia.sourcebuilt.openrewrite</groupId><artifactId>rewrite-java-21</artifactId><version>8.17.1-m3-javadoc-1</version><scope>runtime</scope></dependency>")
    validation = """      <plugin><groupId>org.codehaus.mojo</groupId><artifactId>exec-maven-plugin</artifactId><version>3.5.0</version>
        <executions><execution><id>verify-source-built-parser-identity</id><phase>validate</phase><goals><goal>exec</goal></goals>
          <configuration><executable>python3</executable><arguments>
            <argument>${project.basedir}/contract/verify_parser_dependency.py</argument>
            <argument>--jar</argument><argument>${settings.localRepository}/PARSER_COORDINATE</argument>
          </arguments><environmentVariables><PYTHONDONTWRITEBYTECODE>1</PYTHONDONTWRITEBYTECODE></environmentVariables></configuration>
        </execution></executions>
      </plugin>
""".replace("PARSER_COORDINATE", COORDINATE_PATH)
    pom = once(pom, "    </plugins>\n  </build>\n  <profiles>", validation + "    </plugins>\n  </build>\n  <profiles>")
    dependency_tests = """          <execution><id>verify-parser-dependency-contract</id><phase>verify</phase><goals><goal>exec</goal></goals>
            <configuration><executable>${m3.contract.python}</executable><arguments>
              <argument>${project.basedir}/contract/test_parser_dependency.py</argument>
              <argument>--jar</argument><argument>${settings.localRepository}/PARSER_COORDINATE</argument>
            </arguments><environmentVariables><PYTHONDONTWRITEBYTECODE>1</PYTHONDONTWRITEBYTECODE></environmentVariables></configuration>
          </execution>
""".replace("PARSER_COORDINATE", COORDINATE_PATH)
    pom = once(pom, "        </executions>\n      </plugin>\n    </plugins>\n  </build>\n</profile>",
        dependency_tests + "        </executions>\n      </plugin>\n    </plugins>\n  </build>\n</profile>")
    (destination / "pom.xml").write_text(pom)
    checker = '''#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Refuse a parser dependency that differs from the source-qualified artifact."""
from pathlib import Path
import argparse
import hashlib

EXPECTED_SHA256 = "ARTIFACT_SHA"

def verify(path):
    if path.is_symlink() or not path.is_file():
        raise ValueError("source-built parser must be a regular artifact file")
    if hashlib.sha256(path.read_bytes()).hexdigest() != EXPECTED_SHA256:
        raise ValueError("source-built parser artifact identity drift")

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--jar", type=Path, required=True)
    args = parser.parse_args()
    verify(args.jar)
    print("M3_SOURCE_BUILT_PARSER_IDENTITY_PASS " + EXPECTED_SHA256)
'''.replace("ARTIFACT_SHA", artifact_sha)
    (destination / "contract/verify_parser_dependency.py").write_text(checker)
    (destination / "contract/test_parser_dependency.py").write_text('''#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Positive and negative evidence for the additional parser identity gate."""
from pathlib import Path
import argparse
import tempfile
import unittest
import verify_parser_dependency as owner

parser = argparse.ArgumentParser()
parser.add_argument("--jar", type=Path, required=True)
args = parser.parse_args()

class ParserDependencyContract(unittest.TestCase):
    def test_source_qualified_artifact(self):
        owner.verify(args.jar)

    def test_modified_artifact_refused(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "modified.jar"
            path.write_bytes(args.jar.read_bytes() + b"drift")
            with self.assertRaisesRegex(ValueError, "identity drift"):
                owner.verify(path)

    def test_missing_artifact_refused(self):
        with tempfile.TemporaryDirectory() as directory:
            with self.assertRaisesRegex(ValueError, "regular artifact"):
                owner.verify(Path(directory) / "missing.jar")

    def test_directory_refused(self):
        with tempfile.TemporaryDirectory() as directory:
            with self.assertRaisesRegex(ValueError, "regular artifact"):
                owner.verify(Path(directory))

if __name__ == "__main__":
    unittest.main(argv=[__file__])
''')
    lineage = {"schema": "M3_RUNTIME_PARSER_HOST_SUCCESSOR_V1",
        "original_canonical_revision": "8f8200375d8d403b58e855ee5db02f2e8fac33a8",
        "original_crate": "synexia-openrewrite-recipes/recipe-crates/m3-collections-contract-refinement-20261017",
        "original_capped_pom_sha256": POM_SHA, "successor_pom_sha256": sha(destination / "pom.xml"),
        "unchanged_controls": {relative: sha(origin / relative) for relative in CONTROLS},
        "four_recipe_engines": "unchanged; supplied by the exact original canonical or existing public sdk-owner",
        "parser_coordinate": parser_proof["coordinate"], "parser_artifact_sha256": artifact_sha,
        "parser_sdk_inputs_sha256": parser_proof["sdk_inputs_sha256"],
        "parser_sdk_results_sha256": parser_proof["sdk_results_sha256"],
        "parser_module_receipt_sha256": sha(args.parser_proof),
        "new_gate": "validate the source-qualified parser digest before Maven compilation/tests; four explicit dependency refusal witnesses",
        "old_reports": "history/ contains historical receipts only; fresh fixture and Runtime qualifications are separate",
        "runtime_receiving_phase": "STRING", "phase_promotion": False}
    (destination / "HOST_PARSER_LINEAGE.json").write_text(json.dumps(lineage, indent=2) + "\n")
    (destination / "README.md").write_text('''<!--
SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
SPDX-License-Identifier: Apache-2.0
-->
# Runtime progress recipe host

This is a separately qualified successor of the existing Synexia Maven task
host. The original Java/text recipe engines, five SDK tests, eight sealing
tests and nine receiver cases are retained. The parser dependency is the
source-built OpenRewrite 8.17.1 module under `parser-source/rewrite-java-21`.
An additional four-case contract checks the exact parser artifact identity.

The original Java21 parser cannot losslessly parse Runtime's existing `@spec`
blocks or local `##fragment` links. The source-built module adds two bounded
visitor branches using the existing opaque Javadoc tag model and cursor
helpers. Its tests preserve exact Runtime preimage/postimage text, line endings,
whitespace, inline title markup and ordinary Java type/member references.
The original donor source/Apache license remains alongside the Maven build;
the original Lombok configuration is staged byte-for-byte during that build.

## Replay

Use the qualified Java21 toolchain and a Maven cache populated with the pinned
dependencies. `M3_SOURCE_ROOT` denotes the receiving repository and
`M3_RECIPE_CACHE` a task-specific Maven cache. Set `M3_HOST` to this directory.

```sh
mvn -f "$M3_HOST/parser-source/rewrite-java-21/pom.xml" \\
  -Dmaven.repo.local="$M3_RECIPE_CACHE" install

mvn -f "$M3_HOST/pom.xml" -Dmaven.repo.local="$M3_RECIPE_CACHE" \\
  -Drecipe.owner.root="$M3_SOURCE_ROOT/m3/arrays/qualification/empty-segments-20261017/recipe-custody/sdk-owner" \\
  -Dm3.dataset=fixture -Pcontract verify

mvn -f "$M3_HOST/pom.xml" -Dmaven.repo.local="$M3_RECIPE_CACHE" \\
  -Drecipe.owner.root="$M3_SOURCE_ROOT/m3/arrays/qualification/empty-segments-20261017/recipe-custody/sdk-owner" \\
  -Dm3.dataset=m3jdk21-runtime-facade -Pcontract verify

python3 "$M3_HOST/receive.py" --target "$M3_SOURCE_ROOT" \\
  --recipe-owner "$M3_SOURCE_ROOT/m3/arrays/qualification/empty-segments-20261017/recipe-custody/sdk-owner" \\
  --dataset m3jdk21-runtime-facade --check
```

The Maven validate phase checks the source-qualified parser SHA-256 before
compilation or tests. Offline replay uses `-o` after the declared dependencies
and source-built parser have been populated. Source-origin pins and the
receiving integration baseline are recorded in the generated receiving map.
Historical host receipts are kept under `history/`; fresh qualification results
belong to this parser-host epoch.

## Evidence scope

The parser's paired source builds reproduce three failing syntax witnesses in
the original four-test corpus and pass all four with the repair. All 21 compiled
class owners are retained: one outer visitor changes behavior, two nested
visitors differ only in source line numbers, and the other 18 class files are
byte-identical. The donor's two existing main-source compiler diagnostics are
recorded in both builds; the recipe host, new parser tests and target Runtime
compiler/doclint checks retain their separate strict gates.

The Runtime recipe adds the two opt-in facade methods and their compatibility
tests. Its current map keeps the existing STRING phase and pending runtime
promotion. Full-image and platform results are separate from the finite patch,
frozen-client and scoped/executor/child evidence.
''')
    prepare_spec = importlib.util.spec_from_file_location("original_successor_sealer", origin / "prepare.py")
    prepare = importlib.util.module_from_spec(prepare_spec)
    prepare_spec.loader.exec_module(prepare)
    prepare.seal(destination, "m3jdk21-runtime-facade", "hsoliwal/M3jdk21", args.facade_baseline,
        [args.facade_candidates], "4b24824c3aa09d802121447f8970d442a855f064", args.recipe_owner.resolve())
    print(json.dumps({"host": str(destination), "parser_artifact_sha256": artifact_sha,
        "files": sum(p.is_file() for p in destination.rglob("*"))}))


if __name__ == "__main__":
    main()
