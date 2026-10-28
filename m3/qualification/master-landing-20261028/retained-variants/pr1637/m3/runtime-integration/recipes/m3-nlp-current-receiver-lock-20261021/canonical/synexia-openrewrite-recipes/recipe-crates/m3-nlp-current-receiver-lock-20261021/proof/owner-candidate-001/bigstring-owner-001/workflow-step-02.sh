set -euo pipefail
out="$RUNNER_TEMP/m3-bigstring-classes"
rm -rf "$out"
mkdir -p "$out"
javac --release 21 -Xlint:all -Werror -d "$out" \
  m3/core/src/com/m3/text/StorageIdentity.java \
  m3/core/src/com/m3/text/M3StringPiece.java \
  m3/core/src/com/m3/text/LocalM3StringPiece.java \
  m3/core/src/com/m3/text/JoinedM3StringPiece.java \
  m3/core/src/com/m3/text/M3BigString.java \
  m3/core/test/com/m3/text/M3BigStringSelfTest.java
printf 'M3_BIGSTRING_CLASSES=%s\n' "$out" >> "$GITHUB_ENV"
