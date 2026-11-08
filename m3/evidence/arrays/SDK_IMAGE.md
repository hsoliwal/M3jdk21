<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Build a full M3JDK SDK with packed integers

The existing module-pack assembler supports `--profile sdk` on Linux. It retains
every module from the supplied, source-built JDK and installs the selected arrays
JMOD, including its JNI library. Its input authority is that exact JDK image:
compiler, linker, runtime, platform JMODs, release metadata and auxiliary assets.
Use a completed source build with no concurrent writes to its image directory.

This profile preserves class and native debug information, JNI/JVMTI headers,
manual pages, original platform JMOD bytes, `lib/src.zip`, external debug symbols,
configuration, legal notices and other original SDK assets. It inventories file
types, permissions and literal internal symlink targets. Its runtime module set
must equal the selected descriptor closure, containing all source modules and
the requested product modules. The assembler's diagnostic and inventory fixtures
remain outside the installed SDK. Existing `diagnostics` and `developer` profiles
retain their previous behavior.

## Build and pin the arrays input

Requirements are the completed Linux M3JDK 21 source image, Maven, CMake, a C11
compiler and Python 3.11 or later. Start from the intended M3JDK source checkout.
Set these paths for your environment; both packaging output directories below
must be fresh. The source image must include its original `jmods`, source ZIP,
headers and manual pages. Ambient `JAVA_TOOL_OPTIONS`, `JDK_JAVA_OPTIONS` and
`_JAVA_OPTIONS` must be unset or empty for the SDK profile.

```sh
export M3_SOURCE_ROOT=/absolute/path/to/M3jdk21
export JAVA_HOME=/absolute/path/to/completed-source-image/jdk
export PATH="$JAVA_HOME/bin:$PATH"
export M3_PACK_ROOT=/absolute/path/to/fresh/arrays-sdk-build
cd "$M3_SOURCE_ROOT"

mvn -B -ntp -f m3/arrays/pom.xml -Pnative-linux -DargLine=-Xcheck:jni verify
python3 -m unittest discover -s m3/tooling/module-packs -p test_sdk_profile.py
JAVA_HOME21="$JAVA_HOME" PYTHONDONTWRITEBYTECODE=1 \
  python3 -m unittest discover -s m3/tooling/jmod-recipes/tools -p test_jmod_resolution.py -v
```

The JMOD regression suite uses the supplied source JDK through `JAVA_HOME21`.
It checks default and opt-in module resolution, unchanged classpath launches and
explicit module activation for both published and generated descriptor inputs.

The Maven profile compiles the Java module, builds `libm3_arrays.so`, executes the
native CTest suite and runs the JUnit suite with the native-library path supplied
explicitly. Retain those build/test reports. Create the existing JMOD packer's
data inputs and record hashes of the actual build products and source SDK inputs:

```sh
python3 - <<'PY'
import csv
import hashlib
import json
import os
import shutil
import subprocess
from pathlib import Path

source = Path(os.environ["M3_SOURCE_ROOT"]).resolve(strict=True)
jdk = Path(os.environ["JAVA_HOME"]).resolve(strict=True)
output = Path(os.environ["M3_PACK_ROOT"]).absolute()
output.mkdir(parents=True, exist_ok=False)
jar = source / "m3/arrays/target/m3-arrays-0.1.0-SNAPSHOT.jar"
library = source / "m3/arrays/target/native/libm3_arrays.so"

def sha256(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()

inputs = [jar, library, jdk / "release", jdk / "lib/modules"]
inputs += sorted((jdk / "jmods").glob("*.jmod"))
legal = [source / "m3/arrays" / name for name in
         ("LICENSE", "NOTICE", "PACKED_NOTICES.md")]
inputs += legal
pins = [{"file": str(path), "sha256": sha256(path)} for path in inputs]
revision = subprocess.run(["git", "-C", str(source), "rev-parse", "HEAD"],
                          check=True, capture_output=True, text=True).stdout.strip()
(output / "BUILD_INPUTS.json").write_text(json.dumps({
    "source_commit": revision, "inputs": pins,
    "identity": "exact file hashes; source commit is additional provenance"
}, indent=2, sort_keys=True) + "\n")

(output / "native").mkdir()
(output / "legal").mkdir()
shutil.copy2(library, output / "native" / library.name)
for path in legal:
    shutil.copy2(path, output / "legal" / path.name)
for path in [library, *legal]:
    staged = output / ("native" if path == library else "legal") / path.name
    assert sha256(path) == sha256(staged), "staging input changed"

with (output / "JMOD_IMAGE.tsv").open("w", newline="") as stream:
    writer = csv.writer(stream, delimiter="\t", lineterminator="\n")
    writer.writerow(("module", "jar", "disposition"))
    writer.writerow(("com.m3.arrays", str(jar), "MODULAR_JAR"))
with (output / "DESCRIPTOR_MANIFEST.tsv").open("w", newline="") as stream:
    csv.writer(stream, delimiter="\t", lineterminator="\n").writerow(
        ("module", "unit_jars", "target"))
PY
```

The arrays JAR already has its published module descriptor, so the descriptor
manifest contains only its header. The native staging directory contains exactly
the installed library; CMake caches, test executables and build intermediates are
not runtime-library inputs. Existing legal files and packed-code attribution are
packaged together without modifying their text.

## Package the JMOD and select the product pack

Use the existing JMOD packer's `--jmods-only` operation with
`--do-not-resolve-by-default` for the arrays extension. The SDK profile below
owns image composition. This native JMOD setting keeps the installed extension
out of the default root set for ordinary classpath launches, preserving lookup
of existing classpath classes in the same packages.

```sh
python3 m3/tooling/jmod-recipes/tools/jmod_pack.py \
  --jdk-home "$JAVA_HOME" \
  --image-tsv "$M3_PACK_ROOT/JMOD_IMAGE.tsv" \
  --manifest "$M3_PACK_ROOT/DESCRIPTOR_MANIFEST.tsv" \
  --layer-root "$M3_SOURCE_ROOT" \
  --out "$M3_PACK_ROOT/jmod" \
  --jmods-only --do-not-resolve-by-default \
  --release 21 --date 2026-01-01T00:00:00Z \
  --native-library "com.m3.arrays=$M3_PACK_ROOT/native" \
  --legal-notices "$M3_PACK_ROOT/legal"

python3 - <<'PY'
import hashlib
import json
import os
from pathlib import Path

root = Path(os.environ["M3_PACK_ROOT"]).resolve(strict=True)
archive = root / "jmod/jmods/com.m3.arrays.jmod"
inputs = json.loads((root / "BUILD_INPUTS.json").read_text())
for item in inputs["inputs"]:
    with Path(item["file"]).open("rb") as stream:
        actual = hashlib.file_digest(stream, "sha256").hexdigest()
    if actual != item["sha256"]:
        raise ValueError("Build input changed during packaging: " + item["file"])
with archive.open("rb") as stream:
    checksum = hashlib.file_digest(stream, "sha256").hexdigest()
revision = inputs["source_commit"]
lock = {"schema_version": 1, "packs": [{
    "name": "arrays", "version": "0.1.0-SNAPSHOT",
    "source": "https://github.com/hsoliwal/M3jdk21/tree/" + revision + "/m3/arrays",
    "license": "Apache-2.0", "roots": ["com.m3.arrays"],
    "modules": [{"module": "com.m3.arrays", "file": str(archive), "sha256": checksum}],
    "assets": []
}]}
(root / "arrays-lock.json").write_text(json.dumps(lock, indent=2, sort_keys=True) + "\n")
PY

python3 m3/tooling/module-packs/build_pack.py \
  --jdk "$JAVA_HOME" \
  --out "$M3_PACK_ROOT/sdk" \
  --profile sdk \
  --tool-pack-lock "$M3_PACK_ROOT/arrays-lock.json" \
  --tool-pack arrays
```

The lock contains the computed SHA-256 of this exact product JMOD. The assembler
validates the selected descriptor graph, copies the selected archives, rechecks
their identities and retains them with native `jlink --keep-packaged-modules`.
The full installed JMOD set is available under `sdk/image/jmods`; the existing
developer-tool proof reuses those archives directly.

The native library is installed at `sdk/image/lib/libm3_arrays.so`, the new module
at `sdk/image/jmods/com.m3.arrays.jmod`, and its classes are also linked into
`sdk/image/lib/modules`. Activate it explicitly with
`--add-modules com.m3.arrays` or a `requires com.m3.arrays;` declaration in the
application's module descriptor. These explicit requests still resolve the
installed module; ordinary classpath launches leave it outside the default root
set. The packer option is off by default, so existing callers select this
resolution policy only when they request it.
JNI callers load the installed library using the existing explicit library-path
API. Packed types remain ordinary Java classes; primitive keywords and existing
array/JNI contracts are unchanged.

## Retention checks and acceptance evidence

The source `release` file supplies native `jlink --release-info`. Every original
property except `MODULES` must survive unchanged. The assembler then preserves
the original release-file layout and changes only that module-list value.
`lib/modules` is relinked. Existing `lib/server/classes.jsa` and
`lib/server/classes_nocoops.jsa`, when present, are regenerated by the native
`--generate-cds-archive` plugin and individually verified with named archives,
matching compressed-oops modes and `-Xshare:on`. Original archives are never
copied onto the new runtime. Receipts distinguish the plugin's actual generation
arguments from the bounded-heap verification arguments. Unknown CDS paths,
unsafe symlinks, unexplained linked-byte changes and unaccounted final files
prevent a successful SDK receipt.

The assembler also executes its existing diagnostic and developer-tool proofs:
Java compilation/execution, JAR and JMOD tools, `javap`, `jdeps`, `javadoc`,
`jlink`, `jshell`, `keytool` and an app-image `jpackage` native launch. Inspect:

- `sdk/RECEIPT.json`: selected roots, complete installed module list, JMOD hashes,
  CDS regeneration/verification and the existing proof scope fields.
- `sdk/SDK_SOURCE_ASSETS.json` and `sdk/SDK_IMAGE_ASSETS.json`: exact file/link
  inventories with hashes, modes, sizes and literal symlink targets.
- `sdk/SDK_ASSET_COVERAGE.tsv`: one row for every final asset, identifying
  retained source assets, regenerated runtime data and selected product inputs.
- `sdk/MODULES.tsv`, `sdk/IMAGE_SHA256.tsv`, `sdk/logs/` and
  `sdk/developer-proof/`: descriptor, tool and image evidence.

Basic installed-image inspection uses the produced SDK itself:

```sh
"$M3_PACK_ROOT/sdk/image/bin/java" --validate-modules
"$M3_PACK_ROOT/sdk/image/bin/java" --describe-module com.m3.arrays
"$M3_PACK_ROOT/sdk/image/bin/javac" -version
```

The assembler deliberately retains `m3jdk21_product_acceptance: false` and
`maven_junit_jacoco: NOT_EXECUTED_BY_THIS_SCRIPT`; separate Maven/JUnit/native
reports and product acceptance evidence remain necessary. Complete qualification
also executes packed/VInt correctness checks against the installed module and
installed native library, runs previously compiled legacy-client bytecode
unchanged, and compares the relevant original/candidate JDK regression outcomes.
Synthetic filesystem tests establish inventory/refusal mechanics only. Timing
and memory probes describe their recorded workload and environment; they do not
establish a universal performance improvement.
