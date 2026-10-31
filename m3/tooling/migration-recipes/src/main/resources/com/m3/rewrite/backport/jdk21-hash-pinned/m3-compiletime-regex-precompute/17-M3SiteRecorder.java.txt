// SPDX-License-Identifier: Apache-2.0
package com.m3.compiletime;

import com.m3.precompute.M3RegexShape;
import java.text.MessageFormat;
import java.util.Optional;
import java.util.regex.Pattern;

/** Records one catalogued call site and classifies its constant operand once per content key. */
final class M3SiteRecorder {
    private final M3InventoryState state;
    private final boolean regexOnly;

    M3SiteRecorder(M3InventoryState state, boolean regexOnly) {
        this.state = state;
        this.regexOnly = regexOnly;
    }

    void record(M3CallSiteCatalog.Site site, Optional<String> constant, boolean unanalyzed, int intArg) {
        if (regexOnly && !"b".equals(site.category())) {
            return;
        }
        String c = site.category();
        state.inc(c, site.kind() + "_sites");
        if (unanalyzed) {
            state.inc(c, site.kind() + "_unanalyzed");
            return;
        }
        if (constant.isEmpty()) {
            return;
        }
        String text = constant.get();
        state.inc(c, site.kind() + "_const");
        state.add(c, site.kind() + "_const_units", text.length());
        switch (c) {
            case "b" -> regex(site.kind(), text, intArg);
            case "c" -> format(site.kind(), text);
            case "g" -> other(site.kind(), text);
            default -> { }
        }
    }

    private void regex(String kind, String pattern, int intArg) {
        if (kind.startsWith("string_split") && splitFastpath(pattern)) {
            state.inc("b", "regex_class_SPLIT_FASTPATH");
            return;
        }
        int flags = kind.equals("pattern_compile_flags") ? intArg : 0;
        if (flags < 0) {
            state.inc("b", "regex_flags_not_constant");
            return;
        }
        M3InventoryState.RegexKey key = state.regex.computeIfAbsent(pattern + '\u0000' + flags,
                k -> classify(pattern, flags));
        key.sites[state.corpus]++;
        key.apis.add(kind);
        key.units.add(M3InventoryState.CORPORA[state.corpus] + ':' + state.unitName);
        state.inc("b", "regex_class_" + key.analysis.kind());
        if (key.shapeLiteral) {
            state.inc("b", "regex_shape_literal_sites");
        }
    }

    private static M3InventoryState.RegexKey classify(String pattern, int flags) {
        M3InventoryState.RegexKey key = new M3InventoryState.RegexKey(pattern, flags);
        key.analysis = M3RegexRequirement.analyze(pattern, flags);
        key.shapeLiteral = (flags & Pattern.LITERAL) != 0
                || M3RegexShape.analyze(pattern).kind() == M3RegexShape.Kind.LITERAL;
        return key;
    }

    /** {@code String.split} fastpath: never compiles a {@code Pattern}. */
    static boolean splitFastpath(String regex) {
        char ch = 0;
        boolean fast = (regex.length() == 1 && ".$|()[{^?*+\\".indexOf(ch = regex.charAt(0)) == -1)
                || (regex.length() == 2 && regex.charAt(0) == '\\'
                        && ((ch = regex.charAt(1)) - '0' | '9' - ch) < 0
                        && (ch - 'a' | 'z' - ch) < 0
                        && (ch - 'A' | 'Z' - ch) < 0);
        return fast && (ch < Character.MIN_HIGH_SURROGATE || ch > Character.MAX_LOW_SURROGATE);
    }

    private void format(String kind, String text) {
        if (kind.startsWith("message_format")) {
            messageFormat(text);
            return;
        }
        M3FormatPlan.Kind plan = state.formatKinds.computeIfAbsent(text, t -> M3FormatPlan.parse(t).kind());
        state.inc("c", "formatter_plan_" + plan);
        if (state.seen(state.formats, text)) {
            state.inc("c", "format_class_string_pairs");
        }
    }

    private void messageFormat(String text) {
        try {
            int elements = new MessageFormat(text).getFormats().length;
            state.inc("c", elements == 0 ? "message_format_plan_NO_ARGUMENT" : "message_format_plan_ARGUMENTS");
            state.add("c", "message_format_elements", elements);
        } catch (IllegalArgumentException invalid) {
            state.inc("c", "message_format_plan_INVALID");
        }
        if (state.seen(state.formats, text)) {
            state.inc("c", "format_class_string_pairs");
        }
    }

    private void other(String kind, String text) {
        if (kind.startsWith("log_template")) {
            int at = text.indexOf("{}");
            int at0 = text.indexOf("{0");
            state.inc("g", kind + (at >= 0 || at0 >= 0 ? "_with_placeholders" : "_no_placeholders"));
        }
    }
}
