# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""test_gen_atom_tests.py : unit tests of ATOM-TEST GENERATOR v2 and SCENARIO SHAPES v1 (python -m unittest, CPU).

In memory only (no file is written): spec text through parse_spec_text, the test library as source strings. Each
gate has a discriminator that a plausible wrong gate passes: an unverified XxxOracle call, a check*-named helper that
asserts nothing, a lookup helper whose only AssertionError is in a catch block, a literal 3 in a body or a helper."""
import os
import sys
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_atom_tests as gen  # noqa: E402
import scenario_shapes as shapes  # noqa: E402

KIT = """
final class Kit {
    static java.lang.invoke.MethodHandle findStatic(Class<?> owner, String name) {
        try {
            return null;
        } catch (RuntimeException e) {
            throw new AssertionError("static atom missing: " + name, e);
        }
    }

    static <T extends Throwable> T expect(Class<T> type, Runnable body) {
        try {
            body.run();
        } catch (Throwable t) {
            throw new AssertionError("wrong exception " + t, t);
        }
        fail("expected " + type);
        return null;
    }
}
"""
ORACLE = """
final class ShapeOracle {
    static int firstMember(Object ci, int kind) {
        return kind;
    }

    static void assertSame(Object expected, Object actual) {
        assertEquals(expected, actual);
    }
}
"""
PERF = """
final class Perf {
    static long checkedBatch(long expected) {
        check(expected, "first run");
        return expected;
    }

    private static void check(long sum, String phase) {
        if (sum != 0) {
            throw new AssertionError(phase + ": checksum " + sum);
        }
    }
}
"""
TEMPLATES = {
    "CLASS": ("${generated}\npublic class ${cls} {\n${constants}    @TestFactory\n${entries}\n${methods}\n${helpers}\n}\n",
              set(gen.OPTIONAL_ARGS)),
    "SCENARIO": ("${setup}\n${check}\n", set(gen.OPTIONAL_ARGS)),
    "GETTER_DIFF": ("${setup}\nassertEquals(${expected}, ${receiver}.${atom}()${label});\n${extra}\n",
                    set(gen.OPTIONAL_ARGS) | {"label"}),
}
HEADER = "#class DemoAtomTest\n#summary demo\n"


def render(spec_text, sources=None):
    header, rows = gen.parse_spec_text(spec_text, "Demo.atoms")
    catalog = gen.AssertionCatalog(sources if sources is not None else {"Kit": KIT, "ShapeOracle": ORACLE, "Perf": PERF})
    errors, audit = [], []
    source = gen.render_class(header, rows, TEMPLATES, "Demo.atoms", catalog, errors, audit)
    return source, errors, audit


def scenario(check, helper=""):
    block = "#helper <<<\n%s\n>>>\n" % helper if helper else ""
    return HEADER + block + "@atom Demo.run 1 SCENARIO model\ncheck <<<\n%s\n>>>\n" % check


class AssertionGateTest(unittest.TestCase):
    def assert_refused(self, check, helper=""):
        _, errors, audit = render(scenario(check, helper))
        self.assertTrue(any(e.startswith("ASSERTION") for e in errors), errors)
        return audit

    def assert_accepted(self, check, helper=""):
        _, errors, _ = render(scenario(check, helper))
        self.assertFalse(any(e.startswith("ASSERTION") for e in errors), errors)

    def test_unverified_oracle_call_is_refused_although_v1_accepted_it(self):
        audit = self.assert_refused("int row = ShapeOracle.firstMember(ci, 1);")
        self.assertEqual([True], [a[2] for a in audit], "the v1 regex accepted any XxxOracle.method call")

    def test_check_named_helper_without_assertion_is_refused(self):
        self.assert_refused("checkRows(ci);", "    private static void checkRows(Object ci) {\n        ci.hashCode();\n    }")

    def test_check_named_helper_with_assertion_is_accepted(self):
        self.assert_accepted("checkRows(ci);",
                             "    private static void checkRows(Object ci) {\n        assertNotNull(ci);\n    }")

    def test_lookup_helper_whose_assertion_error_is_in_a_catch_block_is_refused(self):
        self.assert_refused("Kit.findStatic(Object.class, \"run\");")

    def test_expect_helper_with_fail_outside_catch_is_accepted(self):
        self.assert_accepted("Kit.expect(IllegalStateException.class, () -> run());")

    def test_verified_oracle_comparison_is_accepted(self):
        self.assert_accepted("ShapeOracle.assertSame(ci, ci);")

    def test_perf_batch_check_through_private_helper_is_accepted(self):
        self.assert_accepted("Perf.checkedBatch(0L);")

    def test_direct_junit_assertion_is_accepted(self):
        self.assert_accepted("assertThrows(RuntimeException.class, () -> run());")

    def test_assertion_word_in_a_string_does_not_count(self):
        self.assert_refused("String s = \"assertEquals(1, 1)\";")


class LiteralGateTest(unittest.TestCase):
    def literal_errors(self, spec_text):
        return [e for e in render(spec_text)[1] if e.startswith("LITERAL")]

    def test_magic_literal_in_body_is_refused(self):
        self.assertEqual(1, len(self.literal_errors(scenario("int width = 3;\nassertEquals(width, width);"))))

    def test_unit_values_are_exempt(self):
        self.assertEqual([], self.literal_errors(scenario("assertEquals(-1, 0 + 1L - 1.0);")))

    def test_static_final_helper_field_is_a_named_constant(self):
        helper = "    private static final int WIDTH = 3;\n    private static final int[] TABLE = {4, 5};"
        self.assertEqual([], self.literal_errors(scenario("assertEquals(WIDTH, TABLE[0]);", helper)))

    def test_literal_in_helper_method_body_is_refused(self):
        helper = "    private static int width() {\n        return 3;\n    }"
        self.assertEqual(1, len(self.literal_errors(scenario("assertEquals(width(), width());", helper))))

    def test_literal_in_string_or_char_is_not_counted(self):
        self.assertEqual([], self.literal_errors(scenario("assertEquals(\"a3\", String.valueOf('3'));")))

    def test_library_literals_are_reported(self):
        errors = gen.library_literal_errors({"Lib": "final class Lib {\n    static int f() {\n        return 7;\n    }\n}\n"})
        self.assertEqual(1, len(errors))
        self.assertIn("Lib.java:3", errors[0])


class ConstTest(unittest.TestCase):
    def test_const_is_emitted_documented_and_folds(self):
        source, errors, _ = render(HEADER + "#const int WIDTH = 3 // width of the demo lane\n"
                                   + "@atom Demo.run 1 SCENARIO model\ncheck = assertEquals(WIDTH, WIDTH);\n")
        self.assertEqual([], errors)
        self.assertIn("    /** width of the demo lane */\n    private static final int WIDTH = 3;\n", source)

    def test_unused_lowercase_and_duplicate_constants_are_refused(self):
        _, errors, _ = render(HEADER + "#const int width = 3 // lower case\n#const int UNUSED = 4 // never read\n"
                              + "#const int UNUSED = 5 // twice\n"
                              + "@atom Demo.run 1 SCENARIO model\ncheck = assertEquals(width, width);\n")
        kinds = sorted(e.split(": ", 1)[1] for e in errors if e.startswith("CONST"))
        self.assertEqual(["declared twice", "name is not UPPER_CASE", "never referenced", "never referenced"], kinds)

    def test_const_without_role_is_refused(self):
        with self.assertRaises(SystemExit):
            gen.parse_spec_text(HEADER + "#const int WIDTH = 3\n", "Demo.atoms")

    def test_no_constants_keeps_the_class_skeleton(self):
        source, _, _ = render(scenario("assertTrue(true);"))
        self.assertIn("public class DemoAtomTest {\n    @TestFactory", source)


class ShapeExtractionTest(unittest.TestCase):
    def test_scenario_row_becomes_the_pattern_and_renders_identically(self):
        spec = HEADER + ("@atom Demo.size 1 SCENARIO model\ncheck <<<\nDemo a = new Demo();\n"
                         + "assertEquals(3, a.size(), \"three\");\nassertTrue(a.ok());\n>>>\n")
        header, rows = gen.parse_spec_text(spec, "Demo.atoms")
        row = rows[0]
        body = gen.render_body(row, TEMPLATES, "Demo.atoms").strip("\n")
        template, optional = TEMPLATES["GETTER_DIFF"]
        converted = shapes.match_row(row, body, template, optional, TEMPLATES, "Demo.atoms", "GETTER_DIFF")
        self.assertIsNotNone(converted)
        self.assertEqual({"setup": "Demo a = new Demo();", "expected": "3", "receiver": "a",
                          "label": ", \"three\"", "extra": "assertTrue(a.ok());"}, converted["args"])
        self.assertEqual(body, gen.render_body(converted, TEMPLATES, "Demo.atoms").strip("\n"))

    def converted(self, check):
        header, rows = gen.parse_spec_text(HEADER + "@atom Demo.size 1 SCENARIO model\ncheck = %s\n" % check, "Demo.atoms")
        body = gen.render_body(rows[0], TEMPLATES, "Demo.atoms").strip("\n")
        template, optional = TEMPLATES["GETTER_DIFF"]
        return shapes.match_row(rows[0], body, template, optional, TEMPLATES, "Demo.atoms", "GETTER_DIFF")

    def test_argument_holes_take_commas_inside_brackets_only(self):
        self.assertEqual("f(a, b)", self.converted("assertEquals(f(a, b), x.size());")["args"]["expected"])
        self.assertIsNone(self.converted("assertEquals(a, b, x.size());"), "a hole never crosses a top-level comma")

    def test_label_hole_is_a_whole_trailing_argument(self):
        self.assertIsNone(self.converted("assertEquals(1, x.size().y(), z);"), "'.y(), z' is not a trailing argument")


if __name__ == "__main__":
    unittest.main()
