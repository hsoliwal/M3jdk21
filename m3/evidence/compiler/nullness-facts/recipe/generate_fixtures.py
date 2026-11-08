#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Seeded generator of the differential corpus (gate 3 of the relocation, recipe/differential.sh).

Writes N Java sources (G000.java ... ), each a top-level class whose constructor, instance, static and nested-class
methods take Object and String parameters and hold if statements whose guards are built from the pure leaves the
plugin models (null comparisons with the literal on either side, instanceof with and without a binding, boolean
literals, negation, parentheses, && and || nested to depth 3) and from the decoys it must refuse (method calls, field
reads, assignments inside the guard, a local that is not a parameter, comparisons without a null literal, code-looking
string literals and text blocks, guards beyond the 12-operator and 96-node budgets, methods without parameters,
methods with 33 to 40 parameters across the 32-slot packing boundary). Deterministic for a seed: the same files,
byte for byte, on every run on every platform; the corpus is generated into a scratch directory and never committed.

usage: python -I generate_fixtures.py --out DIR [--count 200] [--seed 20261106]
"""
from __future__ import annotations

import argparse
import random
from pathlib import Path

PARAM_TYPES = ("Object", "Object", "Object", "String")
PARAM_COUNTS = (1, 1, 2, 2, 2, 3, 3, 4, 5, 33, 40)
DECOY_TEMPLATES = ("sideEffect()", "field != null", "({p} = null) == null", "{p} == {q}", "local != null",
                   "{p}.hashCode() > 0", "count > 0", "other() == null", "null == field", "{q} != null && mutate()")


class Generator:
    def __init__(self, seed: int) -> None:
        self.rng = random.Random(seed)
        self.binders = 0

    def params(self) -> list[tuple[str, str]]:
        count = self.rng.choice(PARAM_COUNTS)
        return [("p%d" % i, self.rng.choice(PARAM_TYPES)) for i in range(count)]

    def leaf(self, params: list[tuple[str, str]]) -> str:
        objects = [name for name, kind in params if kind == "Object"]
        p = self.rng.choice(params)[0]
        q = self.rng.choice(params)[0]
        kind = self.rng.randrange(12)
        if kind == 0:
            return "%s == null" % p
        if kind == 1:
            return "%s != null" % p
        if kind == 2:
            return "null == %s" % p
        if kind == 3:
            return "null != %s" % p
        if kind == 4 and objects:
            return "%s instanceof String" % self.rng.choice(objects)
        if kind == 5 and objects:
            self.binders += 1
            return "%s instanceof String bind%d" % (self.rng.choice(objects), self.binders)
        if kind == 6:
            return "true"
        if kind == 7:
            return "false"
        if kind == 8:
            return "%s != null" % p
        return self.rng.choice(DECOY_TEMPLATES).format(p=p, q=q)

    def guard(self, params: list[tuple[str, str]], depth: int) -> str:
        if depth == 0 or self.rng.random() < 0.3:
            return self.leaf(params)
        shape = self.rng.randrange(5)
        if shape == 0:
            return "!(%s)" % self.guard(params, depth - 1)
        if shape == 1:
            return "(%s)" % self.guard(params, depth - 1)
        operator = "&&" if shape in (2, 3) else "||"
        return "%s %s %s" % (self.guard(params, depth - 1), operator, self.guard(params, depth - 1))

    def budget_guard(self, params: list[tuple[str, str]]) -> str:
        p = params[0][0]
        copies = self.rng.choice((13, 14, 50, 97, 100))
        return " && ".join(["%s != null" % p] * copies)

    def statement(self, params: list[tuple[str, str]], indent: str) -> str:
        shape = self.rng.randrange(8)
        g = self.guard(params, 3) if shape != 7 else self.budget_guard(params)
        if shape == 0:
            return "%sif (%s) { count++; } else { count--; }\n" % (indent, g)
        if shape == 1:
            return "%sif (%s) { count++; } else if (%s) { count--; }\n" % (indent, g, self.guard(params, 2))
        if shape == 2:
            return "%sif (%s) {\n%s    if (%s) { count++; }\n%s}\n" % (indent, g, indent, self.guard(params, 2), indent)
        if shape == 3:
            return "%sfor (int i = 0; i < 2; i++) { if (%s) { count++; } }\n" % (indent, g)
        if shape == 4:
            return "%sif (%s) { field = null; }\n" % (indent, g)
        return "%sif (%s) { count++; }\n" % (indent, g)

    def body(self, params: list[tuple[str, str]], indent: str) -> str:
        first = params[0][0]
        lines = ["%sObject local = %s;\n" % (indent, first),
                 "%sString text = \"if (%s == null) { count++; }\";\n" % (indent, first)]
        if self.rng.random() < 0.5:
            lines.append("%sString block = \"\"\"\n%s    if (%s != null) { impossible(); }\n%s    \"\"\";\n"
                         % (indent, indent, first, indent))
            lines.append("%sif (block.isEmpty()) { count++; }\n" % indent)
        lines.append("%sif (text.isEmpty() && local == null) { count++; }\n" % indent)
        for _ in range(self.rng.randint(2, 8)):
            lines.append(self.statement(params, indent))
        return "".join(lines)

    def method(self, name: str, modifiers: str, indent: str) -> str:
        params = self.params()
        signature = ", ".join("%s %s" % (kind, pname) for pname, kind in params)
        return "%s%svoid %s(%s) {\n%s%s}\n" % (indent, modifiers, name, signature, self.body(params, indent + "    "), indent)

    def source(self, name: str) -> str:
        parts = ["class %s {\n" % name,
                 "    static Object field;\n    static int count;\n",
                 "    static boolean sideEffect() { count++; return true; }\n",
                 "    static boolean mutate() { field = null; return count > 0; }\n",
                 "    static Object other() { return field; }\n",
                 "    static void impossible() { }\n"]
        constructor_params = self.params()
        signature = ", ".join("%s %s" % (kind, pname) for pname, kind in constructor_params)
        parts.append("    %s(%s) {\n%s    }\n" % (name, signature, self.body(constructor_params, "        ")))
        parts.append("    %s() { }\n" % name)
        for index in range(self.rng.randint(1, 4)):
            parts.append(self.method("m%d" % index, "", "    "))
        if self.rng.random() < 0.5:
            parts.append(self.method("s0", "static ", "    "))
        parts.append("    void noParameters() {\n        Object local = other();\n"
                     "        if (local != null) { count++; }\n        if (field == null) { count--; }\n    }\n")
        if self.rng.random() < 0.4:
            parts.append("    static final class Nested {\n        static int count;\n        static Object field;\n"
                         "        static boolean sideEffect() { return true; }\n"
                         "        static boolean mutate() { return true; }\n"
                         "        static Object other() { return field; }\n        static void impossible() { }\n")
            parts.append(self.method("n0", "", "        "))
            parts.append("    }\n")
        parts.append("}\n")
        return "".join(parts)


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--out", required=True)
    parser.add_argument("--count", type=int, default=200)
    parser.add_argument("--seed", type=int, default=20261106)
    args = parser.parse_args(argv)
    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=True)
    generator = Generator(args.seed)
    for index in range(args.count):
        name = "G%03d" % index
        (out / (name + ".java")).write_bytes(generator.source(name).encode("ascii"))
    print("FIXTURES files=%d seed=%d out=%s" % (args.count, args.seed, out))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
