# Scoped wrapper convergence verification

`verify.py` verifies the exact collections sources installed by the canonical
Synexia recipe. Author and seal the script and its `SOURCE_MANIFEST.json` as
TEXT recipe inputs, execute the recipe, and run the received script against
the received repository. This directory contains orchestration and evidence
contracts; the existing Java tests and JNI source files own runtime behavior.

## Invocation

```sh
python3 m3/collections/verification/wrapper-convergence-20261018/verify.py \
  --target /absolute/path/to/received/M3JDK21 \
  --preimage-tree /absolute/path/to/exact/baseline \
  --java-home /absolute/path/to/jdk21 \
  --maven /absolute/path/to/mvn \
  --maven-repository /absolute/path/to/maven-repository \
  --out /absolute/path/to/new/qualification-output \
  --offline
```

Use Python 3.9 or newer, Java 21, Maven, Linux GCC and GNU `nm`. The output
directory must be new. The preimage has repository-shaped paths, starting
with `m3/collections/`, and includes the complete baseline module source,
POM and resources. Omit `--offline` only when dependency downloads are wanted.
The `--manifest` option can name another sealed manifest; its SHA-256 is
recorded in the receipt. The runner never edits either source tree or changes
the historical `verify_donation.py` guards.

## Manifest contract

The default manifest is adjacent to the received script. Its structure is:

```json
{
  "schema": "m3-wrapper-verification/v1",
  "provenance": {
    "baseline_repository": "https://github.com/hsoliwal/M3JDK21",
    "baseline_revision": "<exact commit>",
    "canonical_recipe": "<canonical repository, revision and recipe path>",
    "generation_receipt": "<exact execution receipt and hash>"
  },
  "files": [
    {
      "path": "m3/collections/src/main/java/com/m3/collections/M3FilteredViews.java",
      "before_sha256": "<64 lowercase hexadecimal characters>",
      "after_sha256": "<64 lowercase hexadecimal characters>",
      "role": "runtime"
    }
  ]
}
```

The example is a schema illustration, not an executable or qualified manifest.
Root-generated final hashes bind the corrected successor, separately from
the catalogue's preserved initial 26-file candidate inventory.

Each file row has a unique repository-relative path, a role, an exact before
SHA-256 (or `ABSENT`) and an exact after SHA-256. Equal before/after hashes
identify unchanged dependencies. Deletions are outside this successor scope.
The manifest must include every regular file under `m3/collections/src`, the
module POM, the six POM resource files (`LICENSE`, `NOTICE`, `provenance.json`,
`synexia-donation.json`, `name-mapping.json`,
`SYNEXIA-DONATION-NOTICE.txt`), this runner, and any additional metadata whose
source custody is being claimed. Include the corrected twenty frozen test
owners, their current baseline dependencies, and both navigation regression
contracts and JUnit adapters. The manifest excludes itself to avoid a cyclic
hash. The canonical recipe receipt must seal the manifest and runner together;
the runner's recorded manifest hash is not an independent signature.

The source manifest rejects missing inputs, duplicate or unsafe paths,
symlinked source components, changed postimages, missing required proof owners,
and an executing script that differs from the received script. With a preimage
tree it also checks every declared before hash and every `ABSENT` assertion.
The source/build closure is checked again after execution.

## Gates and evidence

| Gate | Verification |
|---|---|
| Input custody | Exact postimages, optional exact preimages, all module source/build inputs, provenance and executing runner hash |
| Toolchain | Java, `javac` and `javap` report version 21; Maven reports Java 21; executable and JNI header hashes are recorded |
| Strict main compile | Actual received main Java source closure compiled with `--release 21 -proc:none -Xlint:all,-module -Werror`; `javac -h` emits JNI declarations |
| Existing API | When supplied, the full baseline main source closure is compiled with the same options. `javap -protected -s -constants` checks accessible class declarations and existing members; removed/changed declarations and new abstract obligations fail. Additive members/types are enumerated, never described as an unchanged API |
| Native ABI | Actual path-mask C is linked separately; actual compactor and window C are linked together. Generated JNI headers are force-included, checking C signatures. GCC C17, warnings as errors, UBSan and nonrecovering sanitizer behavior are required. `nm` exports must exactly match the selected generated JNI declarations, including retained int and new long path-mask symbols |
| Existing Maven module | A byte-identical copy of the POM, resources and complete `src` tree builds in the fresh output directory. The existing module runs all tests; missing reports, failures, errors, skipped tests and annotated test owners absent from successful reports fail |
| Java/JNI contracts | Existing int, new long, alias, zero-mask, impossible-stage, bounded path/window and order owners execute in Java mode and in explicitly required native mode with `-Xcheck:jni`. Alias and Java-only JVMs also exercise missing-library refusal |
| Wrapper contracts | Filtered navigation, strict read-only boundaries, wrapper composition, 65,536-layer traversal with `-Xss256k`, comparator/opaque-owner failure, current bounded-navigation regression and nullable ordering execute as standalone owners |
| Final custody | Recheck manifest, target and optional baseline hashes, and ensure Maven did not alter copied inputs |

The runner uses the existing module POM and existing standalone proof classes.
Its source/hash and command orchestration follows the existing
`m3/collections/verify.py` and
`m3/collections/verification/zero-slot-growth-20261017/verify.py` conventions.
No donor algorithm or challenge solution is copied into this runner.

`RECEIPT.json` records each exact command argument vector, working directory,
start/end timestamps, return code, timeout, expected output markers, output
log hashes, gate results, controlled environment and artifact hashes. Raw
stdout/stderr logs, received/preimage/final source inventories, strict compile
argument files, API reports, generated JNI headers, `JNI_ABI.json`, toolchain
hashes and Surefire totals/report hashes remain in the output directory.
The full module's test classpath JAR hashes are taken from its Surefire reports.

A failed command, missing marker, JNI check warning, sanitizer diagnostic or
failed source gate returns a nonzero status. A complete invocation with the
preimage tree returns `PASS_SCOPED`; omitting that optional tree returns the
distinct `PASS_SCOPED_API_NOT_RUN` status and leaves the API comparison gate
explicitly unrun. No success status should be recorded until this generated
runner has executed against the actual received source manifest.

## Limits and independent historical evidence

The result covers this collections module, its declared source closure and
the named Java/JNI proof owners. It does not qualify a complete JDK image,
jtreg, the entire SDK, or a speed or allocation improvement. Order tests run
their contract and nullable modes; the allocation-sampling mode is not used.

Historical donation guards remain a separate requirement and are explicitly
`NOT_RUN_IN_THIS_SCOPED_RUNNER`. Existing guard drift must be retained and
reported in its own baseline/successor record. A successful scoped run does
not turn a failing historical admission check into a pass, authorize a
different source epoch, or expand the recipe's transformation scope.
