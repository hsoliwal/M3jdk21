#!/bin/bash
# SPDX-License-Identifier: Apache-2.0
# usage: gen-example.sh <module-dir> [<module-dir>...]
# Fills Example.java.tpl from <module-dir>/examples/example.spec into <module-dir>/examples/<CLASS>.java.
# The spec is a sequence of "@@KEY" header lines, each followed by the key's body lines (up to the next header).
# Whole-line placeholders (@IMPORTS@, @DECLS@, @DEMO@, @FILL@, @STEADY@) take the body verbatim; inline
# placeholders (@CLASS@, @OWNED_CLASS@, @PROVIDER_CLASS@, @TYPE@, @PROVIDER_NEW@, @OWNED_NEW@) take one line.
set -e
HERE=$(cd "$(dirname "$0")" && pwd)
TPL=$HERE/Example.java.tpl
for MOD in "$@"; do
  SPEC=$MOD/examples/example.spec
  [ -f "$SPEC" ] || { echo "missing $SPEC" >&2; exit 2; }
  CLASS=$(awk '/^@@CLASS$/{getline; print; exit}' "$SPEC")
  OUT=$MOD/examples/$CLASS.java
  awk -v spec="$SPEC" '
    function replace(line, key, value,    out, at, tag) {
      tag = "@" key "@"; out = ""
      while ((at = index(line, tag)) > 0) { out = out substr(line, 1, at - 1) value; line = substr(line, at + length(tag)) }
      return out line
    }
    BEGIN {
      key = ""
      while ((getline line < spec) > 0) {
        if (line ~ /^@@[A-Z_]+$/) { key = substr(line, 3); seen[key] = 0; vals[key] = ""; continue }
        if (key != "") { vals[key] = (seen[key] ? vals[key] "\n" : "") line; seen[key] = 1 }
      }
      close(spec)
    }
    {
      line = $0
      if (line ~ /^[ \t]*@[A-Z_]+@[ \t]*$/) {
        k = line; gsub(/[ \t@]/, "", k)
        if (k in vals) { if (vals[k] != "") print vals[k]; next }
      }
      for (k in vals) line = replace(line, k, vals[k])
      print line
    }' "$TPL" > "$OUT"
  grep -q "@[A-Z_]*@" "$OUT" && { echo "unfilled placeholder in $OUT" >&2; grep -n "@[A-Z_]*@" "$OUT" >&2; exit 3; }
  echo "$OUT"
done
