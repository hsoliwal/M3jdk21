/*
 * Copyright (c) 2003, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package javax.swing.plaf.synth;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.ColorUIResource;
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.util.*;
import java.util.regex.*;
import sun.swing.plaf.synth.*;
import sun.swing.BakedArrayList;
import sun.swing.SwingAccessor;

/**
 * Factory used for obtaining styles. Supports associating a style based on
 * the name of the component as returned by <code>Component.getName()</code>,
 * and the <code>Region</code> associated with the <code>JComponent</code>.
 * Lookup is done using regular expressions.
 *
 * @author Scott Violet
 */
class DefaultSynthStyleFactory extends SynthStyleFactory {
    /**
     * Used to indicate the lookup should be done based on Component name.
     */
    public static final int NAME = 0;
    /**
     * Used to indicate the lookup should be done based on region.
     */
    public static final int REGION = 1;

    /**
     * List containing set of StyleAssociations used in determining matching
     * styles.
     */
    private List<StyleAssociation> _styles;
    /**
     * Used during lookup.
     */
    private BakedArrayList<SynthStyle> _tmpList;

    /**
     * Maps from a List (BakedArrayList to be precise) to the merged style.
     */
    private Map<BakedArrayList<SynthStyle>, SynthStyle> _resolvedStyles;

    /**
     * Used if there are no styles matching a widget.
     */
    private SynthStyle _defaultStyle;

    private final List<CssRule> cssRules = new ArrayList<>();
    private final Map<CssStyleKey, SynthStyle> cssStyles = new HashMap<>();


    DefaultSynthStyleFactory() {
        _tmpList = new BakedArrayList<SynthStyle>(5);
        _styles = new ArrayList<>();
        _resolvedStyles = new HashMap<>();
    }

    public synchronized void addStyle(DefaultSynthStyle style,
                         String path, int type) throws PatternSyntaxException {
        if (path == null) {
            // Make an empty path match all.
            path = ".*";
        }
        if (type == NAME) {
            _styles.add(StyleAssociation.createStyleAssociation(
                            path, style, type));
        }
        else if (type == REGION) {
            _styles.add(StyleAssociation.createStyleAssociation(
                            path.toLowerCase(), style, type));
        }
    }

    /**
     * Returns the style for the specified Component.
     *
     * @param c Component asking for
     * @param id ID of the Component
     */
    public synchronized SynthStyle getStyle(JComponent c, Region id) {
        SynthStyle base = getXmlStyle(c, id);
        if (cssRules.isEmpty()) {
            return base;
        }
        Set<String> classes = new HashSet<>();
        if (c.getClientProperty("styleClass") instanceof String value) {
            StringTokenizer tokens = new StringTokenizer(value);
            while (tokens.hasMoreTokens()) {
                classes.add(tokens.nextToken());
            }
        }
        List<CssRule> matches = new ArrayList<>();
        for (CssRule rule : cssRules) {
            if (rule.selector.matches(c, id, classes)) {
                matches.add(rule);
            }
        }
        if (matches.isEmpty()) {
            return base;
        }
        CssStyleKey key = new CssStyleKey(base, List.copyOf(matches));
        return cssStyles.computeIfAbsent(key,
            k -> new CssStyle((DefaultSynthStyle) k.base, k.rules));
    }

    private SynthStyle getXmlStyle(JComponent c, Region id) {
        BakedArrayList<SynthStyle> matches = _tmpList;

        matches.clear();
        getMatchingStyles(matches, c, id);

        if (matches.size() == 0) {
            return getDefaultStyle();
        }
        // Use a cached Style if possible, otherwise create a new one.
        matches.cacheHashCode();
        SynthStyle style = getCachedStyle(matches);

        if (style == null) {
            style = mergeStyles(matches);

            if (style != null) {
                cacheStyle(matches, style);
            }
        }
        return style;
    }

    synchronized boolean hasCssStyles() {
        return !cssRules.isEmpty();
    }

    void loadCss(Reader reader) throws IOException, ParseException {
        CssRules parsed = new CssRules();
        try {
            SwingAccessor.getCssParserAccessor().parse(reader, parsed);
        } catch (RuntimeException ex) {
            ParseException failure = new ParseException(ex.getMessage(), 0);
            failure.initCause(ex);
            throw failure;
        }
        // Parsing and value validation finish before any live style changes.
        synchronized (this) {
            if (!parsed.rules.isEmpty()) {
                cssRules.addAll(parsed.rules);
                cssStyles.clear();
            }
        }
    }

    private record CssStyleKey(SynthStyle base, List<CssRule> rules) { }

    private enum CssProperty {
        COLOR, BACKGROUND, FAMILY, SIZE, WEIGHT, SLANT,
        TOP, RIGHT, BOTTOM, LEFT, OPAQUE
    }

    private record CssValue(Object value, boolean important) { }

    private record CssRule(CssSelector selector,
                           Map<CssProperty, CssValue> values) { }

    private record CssSelector(String region, String name, List<String> classes,
                               int required, int forbidden, int ids, int qualifiers) {
        boolean matches(JComponent c, Region id, Set<String> componentClasses) {
            return (region == null || region.equalsIgnoreCase(id.getLowerCaseName())) &&
                   (name == null || name.equals(c.getName())) &&
                   componentClasses.containsAll(classes);
        }

        boolean matches(int state) {
            return (state & required) == required && (state & forbidden) == 0;
        }

        int compareSpecificity(CssSelector other) {
            int result = Integer.compare(ids, other.ids);
            if (result == 0) {
                result = Integer.compare(qualifiers, other.qualifiers);
            }
            if (result == 0) {
                result = Boolean.compare(region != null, other.region != null);
            }
            return result;
        }

        static CssSelector parse(String text) {
            String region = null;
            String name = null;
            List<String> classes = new ArrayList<>();
            int required = 0;
            int forbidden = 0;
            int ids = 0;
            int qualifiers = 0;
            int offset = 0;
            if (text.isEmpty()) {
                return null;
            }
            if (text.charAt(0) == '*') {
                offset++;
            } else if (identifier(text.charAt(0))) {
                while (offset < text.length() && identifier(text.charAt(offset))) {
                    offset++;
                }
                region = text.substring(0, offset);
            }
            while (offset < text.length()) {
                char kind = text.charAt(offset++);
                if (kind != '#' && kind != '.' && kind != ':') {
                    return null;
                }
                int start = offset;
                while (offset < text.length() && identifier(text.charAt(offset))) {
                    offset++;
                }
                if (offset == start) {
                    return null;
                }
                String value = text.substring(start, offset);
                if (kind == '#') {
                    if (name != null && !name.equals(value)) {
                        return null;
                    }
                    name = value;
                    ids++;
                } else if (kind == '.') {
                    classes.add(value);
                    qualifiers++;
                } else {
                    int state = switch (value.toLowerCase(Locale.ROOT)) {
                        case "enabled" -> -SynthConstants.DISABLED;
                        case "disabled" -> SynthConstants.DISABLED;
                        case "hover" -> SynthConstants.MOUSE_OVER;
                        case "pressed", "active" -> SynthConstants.PRESSED;
                        case "focus" -> SynthConstants.FOCUSED;
                        case "selected", "checked" -> SynthConstants.SELECTED;
                        case "default" -> SynthConstants.DEFAULT;
                        default -> 0;
                    };
                    if (state == 0) {
                        return null;
                    }
                    if (state < 0) {
                        forbidden |= -state;
                    } else {
                        required |= state;
                    }
                    qualifiers++;
                }
            }
            return new CssSelector(region, name, List.copyOf(classes),
                                   required, forbidden, ids, qualifiers);
        }

        private static boolean identifier(char c) {
            return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' ||
                   c >= '0' && c <= '9' || c == '-' || c == '_';
        }
    }

    /** Receives tokens from the existing HTML parser; it is not a CSS lexer. */
    private static final class CssRules implements SwingAccessor.CssParserCallback {
        private final List<CssRule> rules = new ArrayList<>();
        private final StringBuilder selector = new StringBuilder();
        private final Map<CssProperty, CssValue> values =
            new EnumMap<>(CssProperty.class);
        private String property;
        private boolean unsupportedSelector;

        public void handleSelector(String text) {
            if (selector.length() != 0) {
                selector.append(' ');
            }
            selector.append(text);
        }

        public void handleUnsupportedSelector() {
            unsupportedSelector = true;
        }

        public void startRule() {
            values.clear();
            property = null;
        }

        public void handleProperty(String text) {
            property = text.trim();
        }

        public void handleValue(String text) {
            String value = text.trim();
            boolean important = false;
            int bang = value.lastIndexOf('!');
            if (bang >= 0) {
                if (!value.substring(bang + 1).trim().equalsIgnoreCase("important")) {
                    return;
                }
                important = true;
                value = value.substring(0, bang).trim();
            }
            try {
                switch (property == null ? "" : property) {
                    case "color" -> put(CssProperty.COLOR, color(value), important);
                    case "background-color" ->
                        put(CssProperty.BACKGROUND, color(value), important);
                    case "font-family" ->
                        put(CssProperty.FAMILY, family(value), important);
                    case "font-size" ->
                        put(CssProperty.SIZE, length(value, true), important);
                    case "font-weight" ->
                        put(CssProperty.WEIGHT, weight(value), important);
                    case "font-style" -> put(CssProperty.SLANT,
                        switch (value.toLowerCase(Locale.ROOT)) {
                            case "normal" -> Font.PLAIN;
                            case "italic", "oblique" -> Font.ITALIC;
                            default -> null;
                        }, important);
                    case "padding" -> padding(value, important);
                    case "padding-top" ->
                        put(CssProperty.TOP, length(value, false), important);
                    case "padding-right" ->
                        put(CssProperty.RIGHT, length(value, false), important);
                    case "padding-bottom" ->
                        put(CssProperty.BOTTOM, length(value, false), important);
                    case "padding-left" ->
                        put(CssProperty.LEFT, length(value, false), important);
                    case "-synth-opaque" -> put(CssProperty.OPAQUE,
                        switch (value.toLowerCase(Locale.ROOT)) {
                            case "true" -> Boolean.TRUE;
                            case "false" -> Boolean.FALSE;
                            default -> null;
                        }, important);
                    default -> { }
                }
            } catch (IllegalArgumentException ex) {
                // An invalid declaration cannot discard valid earlier values.
            }
        }

        private void put(CssProperty key, Object value, boolean important) {
            if (value == null) {
                return;
            }
            CssValue previous = values.get(key);
            if (previous == null || important || !previous.important) {
                values.put(key, new CssValue(value, important));
            }
        }

        public void endRule() {
            List<CssSelector> selectors = new ArrayList<>();
            if (!unsupportedSelector && !values.isEmpty()) {
                String source = selector.toString();
                int offset = 0;
                while (offset <= source.length()) {
                    int comma = source.indexOf(',', offset);
                    int end = comma < 0 ? source.length() : comma;
                    CssSelector parsed = CssSelector.parse(source.substring(offset, end).trim());
                    if (parsed == null) {
                        selectors.clear();
                        break;
                    }
                    selectors.add(parsed);
                    if (comma < 0) {
                        break;
                    }
                    offset = comma + 1;
                }
                Map<CssProperty, CssValue> declarations = Map.copyOf(values);
                for (CssSelector parsed : selectors) {
                    rules.add(new CssRule(parsed, declarations));
                }
            }
            selector.setLength(0);
            unsupportedSelector = false;
            values.clear();
            property = null;
        }

        private void padding(String value, boolean important) {
            StringTokenizer tokens = new StringTokenizer(value);
            int count = tokens.countTokens();
            if (count < 1 || count > 4) {
                return;
            }
            Float[] lengths = new Float[count];
            for (int i = 0; i < count; i++) {
                lengths[i] = length(tokens.nextToken(), false);
                if (lengths[i] == null) {
                    return;
                }
            }
            put(CssProperty.TOP, lengths[0], important);
            put(CssProperty.RIGHT, lengths[count > 1 ? 1 : 0], important);
            put(CssProperty.BOTTOM, lengths[count > 2 ? 2 : 0], important);
            put(CssProperty.LEFT, lengths[count > 3 ? 3 : count > 1 ? 1 : 0], important);
        }

        private static Float length(String value, boolean font) {
            value = value.toLowerCase(Locale.ROOT);
            float factor = 1;
            if (value.endsWith("px")) {
                value = value.substring(0, value.length() - 2).trim();
            } else if (font && value.endsWith("pt")) {
                factor = 4f / 3f;
                value = value.substring(0, value.length() - 2).trim();
            } else if (font || !value.equals("0")) {
                return null;
            }
            float result = Float.parseFloat(value) * factor;
            return Float.isFinite(result) && result >= 0 &&
                   (!font || result > 0) && result <= (font ? 4096 : 32767)
                   ? result : null;
        }

        private static String family(String value) {
            int comma = value.indexOf(',');
            if (comma >= 0) {
                value = value.substring(0, comma).trim();
            }
            if (value.isEmpty() || value.length() > 256) {
                return null;
            }
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                if (!Character.isLetterOrDigit(c) && c != ' ' && c != '-' && c != '_') {
                    return null;
                }
            }
            return switch (value.toLowerCase(Locale.ROOT)) {
                case "serif" -> Font.SERIF;
                case "sans-serif" -> Font.SANS_SERIF;
                case "monospace" -> Font.MONOSPACED;
                case "dialog" -> Font.DIALOG;
                case "dialoginput" -> Font.DIALOG_INPUT;
                default -> value;
            };
        }

        private static Integer weight(String value) {
            value = value.toLowerCase(Locale.ROOT);
            if (value.equals("normal")) {
                return Font.PLAIN;
            }
            if (value.equals("bold")) {
                return Font.BOLD;
            }
            int weight = Integer.parseInt(value);
            return weight >= 100 && weight <= 900 && weight % 100 == 0
                   ? (weight >= 600 ? Font.BOLD : Font.PLAIN) : null;
        }

        private static Color color(String value) {
            value = value.toLowerCase(Locale.ROOT);
            if (value.startsWith("#")) {
                String digits = value.substring(1);
                int count = digits.length();
                if (count != 3 && count != 4 && count != 6 && count != 8) {
                    return null;
                }
                for (int i = 0; i < count; i++) {
                    if (Character.digit(digits.charAt(i), 16) < 0) {
                        return null;
                    }
                }
                long number = Long.parseLong(digits, 16);
                if (count <= 4) {
                    int alpha = count == 4 ? (int) (number & 15) * 17 : 255;
                    if (count == 4) {
                        number >>= 4;
                    }
                    return uiColor((int) (number >> 8) * 17,
                                   (int) (number >> 4 & 15) * 17,
                                   (int) (number & 15) * 17, alpha);
                }
                int alpha = count == 8 ? (int) (number & 255) : 255;
                if (count == 8) {
                    number >>= 8;
                }
                return uiColor((int) (number >> 16),
                               (int) (number >> 8 & 255),
                               (int) (number & 255), alpha);
            }
            if ((value.startsWith("rgb(") || value.startsWith("rgba(")) &&
                    value.endsWith(")")) {
                boolean alpha = value.startsWith("rgba(");
                String[] channels = value.substring(alpha ? 5 : 4, value.length() - 1)
                                         .split(",", -1);
                if (channels.length != (alpha ? 4 : 3)) {
                    return null;
                }
                int red = Integer.parseInt(channels[0].trim());
                int green = Integer.parseInt(channels[1].trim());
                int blue = Integer.parseInt(channels[2].trim());
                float opacity = alpha ? Float.parseFloat(channels[3].trim()) : 1;
                if (!Float.isFinite(opacity) || opacity < 0 || opacity > 1) {
                    return null;
                }
                return uiColor(red, green, blue, Math.round(opacity * 255));
            }
            Integer rgb = switch (value) {
                case "black" -> 0x000000;
                case "silver" -> 0xc0c0c0;
                case "gray" -> 0x808080;
                case "white" -> 0xffffff;
                case "maroon" -> 0x800000;
                case "red" -> 0xff0000;
                case "purple" -> 0x800080;
                case "fuchsia" -> 0xff00ff;
                case "green" -> 0x008000;
                case "lime" -> 0x00ff00;
                case "olive" -> 0x808000;
                case "yellow" -> 0xffff00;
                case "navy" -> 0x000080;
                case "blue" -> 0x0000ff;
                case "teal" -> 0x008080;
                case "aqua" -> 0x00ffff;
                case "orange" -> 0xffa500;
                default -> null;
            };
            if (value.equals("transparent")) {
                return uiColor(0, 0, 0, 0);
            }
            return rgb == null ? null : new ColorUIResource(rgb);
        }

        private static Color uiColor(int red, int green, int blue, int alpha) {
            return new ColorUIResource(new Color(red, green, blue, alpha));
        }
    }

    /** Retains ParsedSynthStyle's XML painters, state data and install policy. */
    private static final class CssStyle extends ParsedSynthStyle {
        private static final int STATE_MASK = SynthConstants.ENABLED |
            SynthConstants.DISABLED | SynthConstants.MOUSE_OVER | SynthConstants.PRESSED |
            SynthConstants.FOCUSED | SynthConstants.SELECTED | SynthConstants.DEFAULT;
        private final List<CssRule> rules;
        // At most 128 normalized Synth state combinations; no component/model keys.
        private final Map<Integer, CssValues> states = new HashMap<>();

        CssStyle(DefaultSynthStyle base, List<CssRule> rules) {
            super(base);
            this.rules = rules;
        }

        private synchronized CssValues values(int state) {
            return states.computeIfAbsent(state & STATE_MASK, this::resolve);
        }

        private CssValues resolve(int state) {
            CssValue[] winners = new CssValue[CssProperty.values().length];
            CssSelector[] selectors = new CssSelector[winners.length];
            for (CssRule rule : rules) {
                if (!rule.selector.matches(state)) {
                    continue;
                }
                for (Map.Entry<CssProperty, CssValue> entry : rule.values.entrySet()) {
                    int index = entry.getKey().ordinal();
                    CssValue candidate = entry.getValue();
                    CssValue previous = winners[index];
                    if (previous == null ||
                            candidate.important && !previous.important ||
                            candidate.important == previous.important &&
                            rule.selector.compareSpecificity(selectors[index]) >= 0) {
                        winners[index] = candidate;
                        selectors[index] = rule.selector;
                    }
                }
            }
            return new CssValues(winners);
        }

        @Override
        protected Color getColorForState(JComponent c, Region id, int state,
                                         ColorType type) {
            CssProperty property = type == ColorType.FOREGROUND ||
                    type == ColorType.TEXT_FOREGROUND ? CssProperty.COLOR :
                    type == ColorType.BACKGROUND || type == ColorType.TEXT_BACKGROUND
                    ? CssProperty.BACKGROUND : null;
            Object color = property == null ? null : values(state).get(property);
            return color == null ? super.getColorForState(c, id, state, type) : (Color) color;
        }

        @Override
        protected Font getFontForState(JComponent c, Region id, int state) {
            return values(state).font(super.getFontForState(c, id, state));
        }

        @Override
        public Insets getInsets(SynthContext context, Insets insets) {
            insets = super.getInsets(context, insets);
            CssValues values = values(context.getComponentState());
            insets.top = values.inset(CssProperty.TOP, insets.top);
            insets.right = values.inset(CssProperty.RIGHT, insets.right);
            insets.bottom = values.inset(CssProperty.BOTTOM, insets.bottom);
            insets.left = values.inset(CssProperty.LEFT, insets.left);
            return insets;
        }

        @Override
        public boolean isOpaque(SynthContext context) {
            CssValues values = values(context.getComponentState());
            Boolean opaque = (Boolean) values.get(CssProperty.OPAQUE);
            if (opaque != null) {
                return opaque;
            }
            Color background = (Color) values.get(CssProperty.BACKGROUND);
            return background == null ? super.isOpaque(context) : background.getAlpha() == 255;
        }
    }

    private static final class CssValues {
        private final CssValue[] values;
        private Font sourceFont;
        private Font cssFont;

        CssValues(CssValue[] values) {
            this.values = values;
        }

        Object get(CssProperty property) {
            CssValue value = values[property.ordinal()];
            return value == null ? null : value.value;
        }

        int inset(CssProperty property, int fallback) {
            Float value = (Float) get(property);
            return value == null ? fallback : Math.round(value);
        }

        synchronized Font font(Font base) {
            String family = (String) get(CssProperty.FAMILY);
            Float size = (Float) get(CssProperty.SIZE);
            Integer weight = (Integer) get(CssProperty.WEIGHT);
            Integer slant = (Integer) get(CssProperty.SLANT);
            if (family == null && size == null && weight == null && slant == null) {
                return base;
            }
            if (base == null) {
                base = new FontUIResource(Font.DIALOG, Font.PLAIN, 12);
            }
            if (base.equals(sourceFont)) {
                return cssFont;
            }
            int style = (weight == null ? base.getStyle() & Font.BOLD : weight) |
                        (slant == null ? base.getStyle() & Font.ITALIC : slant);
            Font result = family == null ? base : new Font(family, base.getStyle(), base.getSize());
            sourceFont = base;
            cssFont = new FontUIResource(result.deriveFont(style, size == null ? base.getSize2D() : size));
            return cssFont;
        }
    }

    /**
     * Returns the style to use if there are no matching styles.
     */
    private SynthStyle getDefaultStyle() {
        if (_defaultStyle == null) {
            _defaultStyle = new DefaultSynthStyle();
            ((DefaultSynthStyle)_defaultStyle).setFont(
                new FontUIResource(Font.DIALOG, Font.PLAIN,12));
        }
        return _defaultStyle;
    }

    /**
     * Fetches any styles that match the passed into arguments into
     * <code>matches</code>.
     */
    private void getMatchingStyles(List<SynthStyle> matches, JComponent c,
                                   Region id) {
        String idName = id.getLowerCaseName();
        String cName = c.getName();

        if (cName == null) {
            cName = "";
        }
        for (int counter = _styles.size() - 1; counter >= 0; counter--){
            StyleAssociation sa = _styles.get(counter);
            String path;

            if (sa.getID() == NAME) {
                path = cName;
            }
            else {
                path = idName;
            }

            if (sa.matches(path) && matches.indexOf(sa.getStyle()) == -1) {
                matches.add(sa.getStyle());
            }
        }
    }

    /**
     * Caches the specified style.
     */
    private void cacheStyle(List<SynthStyle> styles, SynthStyle style) {
        BakedArrayList<SynthStyle> cachedStyles = new BakedArrayList<>(styles);

        _resolvedStyles.put(cachedStyles, style);
    }

    /**
     * Returns the cached style from the passed in arguments.
     */
    private SynthStyle getCachedStyle(List<SynthStyle> styles) { // ??
        if (styles.size() == 0) {
            return null;
        }
        return _resolvedStyles.get(styles);
    }

    /**
     * Creates a single Style from the passed in styles. The passed in List
     * is reverse sorted, that is the most recently added style found to
     * match will be first.
     */
    private SynthStyle mergeStyles(List<SynthStyle> styles) {
        int size = styles.size();

        if (size == 0) {
            return null;
        }
        else if (size == 1) {
            return (SynthStyle)((DefaultSynthStyle)styles.get(0)).clone();
        }
        // NOTE: merging is done backwards as DefaultSynthStyleFactory reverses
        // order, that is, the most specific style is first.
        DefaultSynthStyle style = (DefaultSynthStyle)styles.get(size - 1);

        style = (DefaultSynthStyle)style.clone();
        for (int counter = size - 2; counter >= 0; counter--) {
            style = ((DefaultSynthStyle)styles.get(counter)).addTo(style);
        }
        return style;
    }
}
