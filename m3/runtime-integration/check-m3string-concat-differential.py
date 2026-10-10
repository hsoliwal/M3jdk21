#!/usr/bin/env python3
"""Independent differential proof for the M3String persistent concat contract.

This gate does not import or reimplement M3StringPool.  It checks the receiver's
source-level ownership/balance hooks, then exercises an independent UTF-16
reference model over deterministic adversarial compositions.  It proves the
observable contract (order, UTF-16 length, Java hash, coder, charAt, and
bounded tree height), not a particular internal node shape.
"""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
import math
import random
import sys
from typing import Optional


ROOT = Path(__file__).resolve().parents[2]


def fail(message: str) -> None:
    raise SystemExit(f"M3_STRING_CONCAT_DIFFERENTIAL_FAIL|{message}")


def read(relative: str) -> str:
    path = ROOT / relative
    if not path.is_file():
        fail(f"missing={relative}")
    return path.read_text(encoding="utf-8")


def require(source: str, fragment: str, label: str) -> None:
    if fragment not in source:
        fail(f"source_contract={label}|missing={fragment}")


def utf16_units(value: str) -> tuple[int, ...]:
    encoded = value.encode("utf-16-le", "surrogatepass")
    return tuple(encoded[index] | (encoded[index + 1] << 8)
                 for index in range(0, len(encoded), 2))


def java_hash(units: tuple[int, ...]) -> int:
    result = 0
    for unit in units:
        result = (31 * result + unit) & 0xFFFFFFFF
    return result


def pow31(length: int) -> int:
    result = 1
    factor = 31
    remaining = length
    while remaining:
        if remaining & 1:
            result = (result * factor) & 0xFFFFFFFF
        factor = (factor * factor) & 0xFFFFFFFF
        remaining >>= 1
    return result


@dataclass(frozen=True)
class RefNode:
    """Reference node: only leaves carry UTF-16 payload; pairs retain children."""

    leaf_units: Optional[tuple[int, ...]]
    identity: int
    left: Optional["RefNode"]
    right: Optional["RefNode"]
    length: int
    hash_value: int
    coder: int
    height: int

    @staticmethod
    def leaf(units: tuple[int, ...], identity: int) -> "RefNode":
        return RefNode(
            units,
            identity,
            None,
            None,
            len(units),
            java_hash(units),
            0 if all(unit <= 0xFF for unit in units) else 1,
            0,
        )

    @staticmethod
    def pair(left: "RefNode", right: "RefNode") -> "RefNode":
        return RefNode(
            None,
            -1,
            left,
            right,
            left.length + right.length,
            (left.hash_value * pow31(right.length) + right.hash_value)
            & 0xFFFFFFFF,
            left.coder | right.coder,
            1 + max(left.height, right.height),
        )

    def char_at(self, index: int) -> int:
        if index < 0 or index >= self.length:
            raise IndexError(index)
        if self.leaf_units is not None:
            return self.leaf_units[index]
        assert self.left is not None and self.right is not None
        if index < self.left.length:
            return self.left.char_at(index)
        return self.right.char_at(index - self.left.length)

    def leaves(self) -> tuple[tuple[int, tuple[int, ...]], ...]:
        if self.leaf_units is not None:
            return ((self.identity, self.leaf_units),)
        assert self.left is not None and self.right is not None
        return self.left.leaves() + self.right.leaves()


def balance(left: RefNode, right: RefNode) -> RefNode:
    left_height = left.height
    right_height = right.height
    if left_height > right_height + 1:
        assert left.left is not None and left.right is not None
        branch = left
        if branch.left.height >= branch.right.height:
            return RefNode.pair(
                branch.left, RefNode.pair(branch.right, right))
        assert branch.right.left is not None and branch.right.right is not None
        middle = branch.right
        return RefNode.pair(
            RefNode.pair(branch.left, middle.left),
            RefNode.pair(middle.right, right))
    if right_height > left_height + 1:
        assert right.left is not None and right.right is not None
        branch = right
        if branch.right.height >= branch.left.height:
            return RefNode.pair(
                RefNode.pair(left, branch.left), branch.right)
        assert branch.left.left is not None and branch.left.right is not None
        middle = branch.left
        return RefNode.pair(
            RefNode.pair(left, middle.left),
            RefNode.pair(middle.right, branch.right))
    return RefNode.pair(left, right)


def concat(left: RefNode, right: RefNode) -> RefNode:
    if left.length == 0:
        return right
    if right.length == 0:
        return left
    if left.height > right.height + 1:
        assert left.left is not None and left.right is not None
        return balance(left.left, concat(left.right, right))
    if right.height > left.height + 1:
        assert right.left is not None and right.right is not None
        return balance(concat(left, right.left), right.right)
    return RefNode.pair(left, right)


def build_left(parts: list[RefNode]) -> RefNode:
    result = RefNode.leaf((), -1)
    for part in parts:
        result = concat(result, part)
    return result


def build_right(parts: list[RefNode]) -> RefNode:
    if not parts:
        return RefNode.leaf((), -1)
    result = parts[-1]
    for part in reversed(parts[:-1]):
        result = concat(part, result)
    return result


def check_case(parts: list[RefNode], case_number: int) -> None:
    expected = tuple(unit for part in parts for unit in (
        part.leaf_units if part.leaf_units is not None else
        tuple(part.char_at(index) for index in range(part.length))))
    expected_hash = java_hash(expected)
    expected_coder = 0 if all(unit <= 0xFF for unit in expected) else 1

    left_tree = build_left(parts)
    right_tree = build_right(parts)

    for label, tree in (("left", left_tree), ("right", right_tree)):
        if tree.length != len(expected):
            fail(f"case={case_number}|{label}|length")
        if tree.hash_value != expected_hash:
            fail(f"case={case_number}|{label}|java_hash")
        if tree.coder != expected_coder:
            fail(f"case={case_number}|{label}|coder")
        for index, unit in enumerate(expected):
            if tree.char_at(index) != unit:
                fail(f"case={case_number}|{label}|char_at={index}")
        if tuple(identity for identity, _ in tree.leaves()) != tuple(
                identity for identity, _ in left_tree.leaves()):
            fail(f"case={case_number}|{label}|leaf_order")
        leaf_count = max(1, len(tree.leaves()))
        if tree.height > 2 * math.ceil(math.log2(leaf_count + 1)) + 1:
            fail(f"case={case_number}|{label}|height={tree.height}")
        if tree.leaf_units is None and tree.left is None:
            fail(f"case={case_number}|{label}|missing_children")

    if left_tree.leaves() != right_tree.leaves():
        fail(f"case={case_number}|parenthesization")


def main() -> None:
    pool = read("src/java.base/share/classes/java/lang/M3StringPool.java")
    tuple_source = read("src/java.base/share/classes/java/lang/M3StringTuple.java")
    m3 = read("src/java.base/share/classes/java/lang/M3String.java")

    for fragment, label in [
        ("private static M3String concatBalanced(M3String left, M3String right)",
         "balanced-owner"),
        ("return balance(branch.left, concatBalanced(branch.right, right));",
         "left-recursion"),
        ("return balance(concatBalanced(left, branch.left), branch.right);",
         "right-recursion"),
        ("sameLeafSequence(M3String.whole(existing), left, right)",
         "exact-leaf-identity"),
        ("left.hashCodeValue() * M3String.pow31(right.length()) + right.hashCodeValue()",
         "composed-java-hash"),
        ("M3StringFacts.compose(left.facts(), right.facts())",
         "composed-facts"),
        ("M3StringPool.concat(canonicalize(first), canonicalize(second))",
         "canonical-concat-owner"),
    ]:
        require(pool if label not in {"composed-java-hash", "composed-facts"} else tuple_source
                if label == "composed-java-hash"
                else m3, fragment, label)

    rng = random.Random(20261010)
    alphabet = ("", "a", "z", "é", "Ā", "\ud800", "\udfff", "😀", "ab", "é\ud800")
    case_count = 0
    identity = 1

    fixed = [
        ["", "a", "😀", "\ud800", "\u0100"],
        ["left", "", "middle", "\udfff", "right"],
        ["a"] * 97,
        ["\ud800", "\udfff", "😀", "é"] * 31,
    ]
    for values in fixed:
        parts = []
        for value in values:
            parts.append(RefNode.leaf(utf16_units(value), identity))
            identity += 1
        check_case(parts, case_count)
        case_count += 1

    for _ in range(256):
        parts = []
        for _ in range(rng.randint(0, 40)):
            value = rng.choice(alphabet)
            parts.append(RefNode.leaf(utf16_units(value), identity))
            identity += 1
        check_case(parts, case_count)
        case_count += 1

    print(
        "M3_STRING_CONCAT_DIFFERENTIAL_PASS"
        f"|cases={case_count}|java_hash=true|utf16=true|"
        "parenthesization=true|bounded_balance=true|source_contract=true"
    )


if __name__ == "__main__":
    main()
