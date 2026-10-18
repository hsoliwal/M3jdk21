#!/bin/sh
# Plain javac validation of the dependency-free Maven recipe (mvn is not needed).
set -eu
ROOT=$(CDPATH='' cd -- "$(dirname "$0")/../../.." && pwd)
MAIN="$ROOT/make/m3/recipes/src/main/java/org/synexia/m3/recipe/M3InventoryRecipe.java"
TEST="$ROOT/make/m3/recipes/src/test/java/org/synexia/m3/recipe/M3InventoryRecipeTest.java"
CLASSES="$ROOT/out/recipes"
mkdir -p "$CLASSES"
javac -d "$CLASSES" "$MAIN" "$TEST"
java -cp "$CLASSES" org.synexia.m3.recipe.M3InventoryRecipeTest
java -cp "$CLASSES" org.synexia.m3.recipe.M3InventoryRecipe \
   "$ROOT/src/java.base/share/classes" "$ROOT/docs/m3/source-inventory.jsonl"
cp "$ROOT/docs/m3/source-inventory.jsonl" "$ROOT/out/inventory-first.jsonl"
java -cp "$CLASSES" org.synexia.m3.recipe.M3InventoryRecipe \
   "$ROOT/src/java.base/share/classes" "$ROOT/docs/m3/source-inventory.jsonl"
cmp "$ROOT/docs/m3/source-inventory.jsonl" "$ROOT/out/inventory-first.jsonl"
echo 'M3InventoryRecipe fixed point PASS'
