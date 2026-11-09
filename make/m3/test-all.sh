#!/bin/sh
set -eu
ROOT=$(CDPATH='' cd -- "$(dirname "$0")/../.." && pwd)
"$ROOT/make/m3/test-local.sh"
"$ROOT/make/m3/recipes/test-local.sh"
