// SPDX-License-Identifier: Apache-2.0
package jdk.m3.compiler.reference;

import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.TreeMap;

/**
 * CREF_FACT_PROBE v1: deterministic random Java units (locals of reference, String, array and primitive types;
 * aliasing assignments, ternaries, if/else, loops, allocations, calls, array stores, early returns) through the
 * whole reference-flow family: M3CompilerFactsAudit, M3SourceLiveness (plain and extended), M3ReferenceFlow,
 * project(), M3CompilerReferenceCensus and M3CRefPathCertainty. Every record is dumped reflectively (arrays, BitSets,
 * lists, maps, enums); components derived from the analyzer's own class bytes (engineDigest and the proof keys
 * built on it) are masked, everything else must be byte-identical between donor and relocated packages.
 * Usage: CrefFactProbe seed units
 */
public final class CrefFactProbe {
    private static int seq;
    private static final String[] TYPES = {"Object", "String", "int[]", "StringBuilder", "Object[]"};
    private static final java.util.Set<String> MASKED = java.util.Set.of(
            "engineDigest", "proofKey", "referenceProofKey", "flowProofKey");

    public static void main(String[] args) throws Exception {
        SplittableRandom random = new SplittableRandom(Long.decode(args[0]));
        int units = Integer.parseInt(args[1]);
        MessageDigest all = MessageDigest.getInstance("SHA-256");
        long methods = 0, edges = 0, failures = 0;
        for (int u = 0; u < units; u++) {
            Map<String, String> sources = new TreeMap<>();
            int files = 1 + random.nextInt(2);
            for (int f = 0; f < files; f++) sources.put("p/U" + u + "F" + f + ".java", unit(random, u, f));
            StringBuilder dump = new StringBuilder();
            try {
                var facts = M3CompilerFactsAudit.analyze(sources, null);
                dump(dump, "facts", facts);
                var flow = M3SourceLiveness.analyze(sources, null);
                dump(dump, "flow", flow);
                var extended = M3SourceLiveness.analyzeExtended(sources, null);
                dump(dump, "extended", extended);
                var refs = M3ReferenceFlow.analyze(sources, null);
                dump(dump, "refs", refs);
                methods += refs.methods().size();
                dump(dump, "projection", M3ReferenceFlow.project(refs, flow, null));
                var census = M3CompilerReferenceCensus.analyze(refs, flow, null);
                edges += census.edges().size();
                dump(dump, "census", census);
                dump(dump, "certainty", M3CRefPathCertainty.analyze(sources, refs, flow, null));
            } catch (Exception | Error e) {
                if (failures++ < 4) System.out.println("throw unit" + u + ": " + e);
                dump.append("THROW ").append(e.getClass().getSimpleName()).append(' ').append(e.getMessage());
            }
            all.update(dump.toString().getBytes(StandardCharsets.UTF_8));
            if (u < 3) System.out.println("unit" + u + " dumpBytes=" + dump.length());
        }
        System.out.println("CREF_FACT_PROBE units=" + units + " methods=" + methods + " edges=" + edges
                + " throws=" + failures + " digest=" + HexFormat.of().formatHex(all.digest()));
    }

    private static String unit(SplittableRandom r, int u, int f) {
        StringBuilder s = new StringBuilder("package p;\nfinal class U" + u + "F" + f + " {\n");
        s.append("  static Object sink;\n  static Object id(Object o) { return o; }\n");
        int ms = 1 + r.nextInt(3);
        for (int m = 0; m < ms; m++) {
            s.append("  static Object m").append(m).append("(Object a, String b, int n, boolean c) {\n");
            List<String> refs = new ArrayList<>(List.of("a", "b"));
            int locals = 1 + r.nextInt(5);
            for (int i = 0; i < locals; i++) {
                String t = TYPES[r.nextInt(TYPES.length)];
                String init = switch (t) {
                    case "String" -> r.nextBoolean() ? "b" : "\"s" + i + "\"";
                    case "int[]" -> "new int[" + (1 + r.nextInt(4)) + "]";
                    case "StringBuilder" -> "new StringBuilder()";
                    case "Object[]" -> "new Object[2]";
                    default -> refs.get(r.nextInt(refs.size()));
                };
                s.append("    ").append(t).append(" v").append(i).append(" = ").append(init).append(";\n");
                refs.add("v" + i);
            }
            int stmts = 2 + r.nextInt(7);
            for (int k = 0; k < stmts; k++) s.append(statement(r, refs, locals, 2));
            s.append("    return ").append(refs.get(r.nextInt(refs.size()))).append(";\n  }\n");
        }
        return s.append("}\n").toString();
    }

    private static String statement(SplittableRandom r, List<String> refs, int locals, int depth) {
        String pad = "    ".repeat(depth);
        String any = refs.get(r.nextInt(refs.size()));
        String local = "v" + r.nextInt(locals);
        return switch (depth > 3 ? r.nextInt(5) : r.nextInt(9)) {
            case 0 -> pad + "sink = " + any + ";\n";
            case 1 -> pad + "sink = c ? " + any + " : " + refs.get(r.nextInt(refs.size())) + ";\n";
            case 2 -> pad + "id(" + any + ");\n";
            case 3 -> pad + "Object t" + (seq++) + " = " + any + ";\n";
            case 4 -> pad + "if (n == " + r.nextInt(3) + ") return " + any + ";\n";
            case 5 -> pad + "if (c) {\n" + statement(r, refs, locals, depth + 1) + pad + "} else {\n"
                    + statement(r, refs, locals, depth + 1) + pad + "}\n";
            case 6 -> {
                String i = "i" + (seq++);
                yield pad + "for (int " + i + " = 0; " + i + " < n; " + i + "++) {\n"
                        + statement(r, refs, locals, depth + 1) + pad + "}\n";
            }
            case 7 -> pad + "while (n-- > 0) {\n" + statement(r, refs, locals, depth + 1) + pad + "}\n";
            default -> pad + "sink = new Object[] { " + any + ", " + local + " };\n";
        };
    }

    private static void dump(StringBuilder out, String name, Object value) throws Exception {
        out.append(name).append('=');
        value(out, value);
        out.append('\n');
    }

    private static void value(StringBuilder out, Object v) throws Exception {
        if (v == null) { out.append("null"); return; }
        Class<?> c = v.getClass();
        if (c.isRecord()) {
            out.append(c.getSimpleName()).append('(');
            for (RecordComponent rc : c.getRecordComponents()) {
                out.append(rc.getName()).append(':');
                if (MASKED.contains(rc.getName())) out.append("<masked>");
                else { rc.getAccessor().setAccessible(true); value(out, rc.getAccessor().invoke(v)); }
                out.append(';');
            }
            out.append(')');
        } else if (c.isArray()) {
            out.append('[');
            int n = java.lang.reflect.Array.getLength(v);
            for (int i = 0; i < n; i++) { if (i > 0) out.append(','); value(out, java.lang.reflect.Array.get(v, i)); }
            out.append(']');
        } else if (v instanceof Collection<?> col) {
            out.append('<');
            for (Object o : col) { value(out, o); out.append(','); }
            out.append('>');
        } else if (v instanceof Map<?, ?> map) {
            out.append('{');
            for (var e : map.entrySet()) { value(out, e.getKey()); out.append("->"); value(out, e.getValue()); out.append(','); }
            out.append('}');
        } else if (v instanceof Enum<?> e) {
            out.append(e.name());
        } else if (v instanceof BitSet || v instanceof CharSequence || v instanceof Number || v instanceof Boolean
                || v instanceof Character) {
            out.append(v);
        } else {
            out.append("?").append(c.getSimpleName());
        }
    }
}
