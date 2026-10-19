#!/bin/sh
set -eu
ROOT=$(CDPATH='' cd -- "$(dirname "$0")/../.." && pwd)
sh "$ROOT/make/m3/test-local.sh"
sh "$ROOT/make/m3/recipes/test-local.sh"
