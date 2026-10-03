#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
module="$(cd "$(dirname "$0")" && pwd)"
[[ "$(uname -s)-$(uname -m)" == Linux-x86_64 ]] || { echo 'Unverified JavaFX target' >&2; exit 2; }
jdk="${M3_JDK:-${JAVA_HOME:-$(dirname "$(dirname "$(readlink -f "$(command -v java)")")")}}"
version=21.0.12
url="https://download2.gluonhq.com/openjfx/$version/openjfx-${version}_linux-x64_bin-jmods.zip"
mkdir -p "$module/target"
work="$(mktemp -d "$module/target/javafx.XXXXXX")"
curl --fail --location --proto '=https' --tlsv1.2 --retry 3 "$url" -o "$work/javafx.zip"
actual="$(sha256sum "$work/javafx.zip" | cut -d' ' -f1)"
printf 'version\tplatform\tsha256\turl\n%s\tlinux-x86_64\t%s\t%s\n' "$version" "$actual" "$url" > "$work/ACQUISITION.tsv"
echo "JAVAFX_ARCHIVE_SHA256=$actual"
if [[ "${1:-}" == --inventory-only ]]; then
  echo 'DISCOVERY_ONLY: candidate identity recorded; no module or native code executed'
  exit 0
fi
expected="$(cat "$module/javafx-21.0.12-linux-x64.sha256")"
[[ "$expected" =~ ^[0-9a-f]{64}$ && "$actual" == "$expected" ]] || { echo 'Unreviewed or changed JavaFX archive' >&2; exit 1; }
# Validate the ZIP namespace before extraction; no symlinks or path traversal are admitted.
python3 - "$work/javafx.zip" "$work/unpacked" <<'PY'
import pathlib, stat, sys, zipfile
archive, destination = sys.argv[1:]
with zipfile.ZipFile(archive) as source:
    for item in source.infolist():
        name = pathlib.PurePosixPath(item.filename)
        if name.is_absolute() or '..' in name.parts or '\\' in item.filename or stat.S_ISLNK(item.external_attr >> 16):
            raise SystemExit('Unsafe JavaFX archive member: ' + item.filename)
    source.extractall(destination)
PY
mapfile -t fx < <(find "$work/unpacked" -name javafx.controls.jmod -print)
[[ ${#fx[@]} == 1 ]] || { echo 'Expected one JavaFX module set' >&2; exit 1; }
mods="$(dirname "${fx[0]}")"
printf 'path\tsha256\tmodule\n' > "$mods/LOCK.tsv"
# Use the actual descriptor identity through jmod; our Java inspector independently verifies it.
for module_name in javafx.base javafx.graphics javafx.controls; do
  printf '%s.jmod\t%s\t%s\n' "$module_name" "$(sha256sum "$mods/$module_name.jmod" | cut -d' ' -f1)" "$module_name" >> "$mods/LOCK.tsv"
  "$jdk/bin/jmod" describe "$mods/$module_name.jmod" >> "$work/DESCRIPTORS.txt"
  "$jdk/bin/jmod" list "$mods/$module_name.jmod" | grep '^legal/' >> "$work/LEGAL_ENTRIES.txt"
done
bash "$module/assemble.sh" "$mods/LOCK.tsv" javafx.controls "$work/desktop"
# Compile against the exact linked system image; JMODs are never runtime classpath entries.
"$jdk/bin/javac" --system "$work/desktop/image" -source 21 -target 21 --add-modules javafx.controls -d "$work/probes" "$module/src/it/JavaFxSmoke.java"
timeout 45s xvfb-run -a "$work/desktop/image/bin/java" -Dprism.order=sw --add-modules javafx.controls -cp "$work/probes" JavaFxSmoke | tee "$work/NATIVE_LAUNCH.txt"
printf 'gate\tstatus\narchive_identity\tPINNED\njavafx_descriptors\tPASS\njlink\tPASS\ncontrol_snapshot_native_launch\tPASS\nrelease_approval\tNOT_GRANTED\n' > "$work/JAVAFX_PROOF.tsv"
echo "PASS: JavaFX desktop proof $work"
