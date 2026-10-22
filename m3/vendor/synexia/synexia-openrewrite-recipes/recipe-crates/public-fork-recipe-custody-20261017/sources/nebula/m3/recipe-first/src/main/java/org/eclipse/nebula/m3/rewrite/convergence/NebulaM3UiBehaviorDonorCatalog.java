// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.convergence;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * Read-only behavior-donor catalogue for the final SWT/Nebula distillation proof.
 *
 * <p>The catalogue records observable UI behaviors to test. It grants no permission to copy donor
 * implementation source and carries no mutation or promotion authority.</p>
 */
public final class NebulaM3UiBehaviorDonorCatalog {
    public record Source(String id, String url, List<String> obligations) {
        public Source {
            id = text(id, "id");
            url = text(url, "url");
            obligations = List.copyOf(Objects.requireNonNull(obligations, "obligations"));
            if (obligations.isEmpty() || obligations.stream().anyMatch(String::isBlank)) {
                throw new IllegalArgumentException("obligations");
            }
        }
    }

    private static final List<Source> SOURCES =
            List.of(
                    new Source(
                            "JAVA2S_SWT",
                            "https://www.java2s.com/Tutorial/Java/0280__SWT/Catalog0280__SWT.html",
                            List.of(
                                    "CANVAS_CLIPPED_PAINT",
                                    "TABLE_MODEL_SELECTION_EDITOR_RENDERER_SORT",
                                    "TREE_LAZY_POPULATION_SELECTION_EDITOR_EVENTS",
                                    "TREE_TABLE_COLUMNS",
                                    "SCROLLED_COMPOSITE_LOGICAL_ORIGIN",
                                    "SCROLLBAR_SELECTION_EVENTS",
                                    "KEY_MOUSE_EVENT_ORDERING",
                                    "DND_HIT_TESTING",
                                    "SWT_AWT_SWING_BRIDGE",
                                    "WIN32_PLATFORM_QUIRKS")),
                    new Source(
                            "JAVA2S_SWT_2D",
                            "https://www.java2s.com/Tutorial/Java/0300__SWT-2D-Graphics/Catalog0300__SWT-2D-Graphics.html",
                            List.of(
                                    "GC_STATE_SCOPE",
                                    "PAINT_CLIP",
                                    "FOCUS_DRAWING",
                                    "PATH_AND_GEOMETRY",
                                    "AFFINE_TRANSFORM",
                                    "ANIMATION_INVALIDATION",
                                    "IMAGE_DRAWING")),
                    new Source(
                            "JAVA2S_SWING",
                            "https://www.java2s.com/Tutorial/Java/0240__Swing/Catalog0240__Swing.html",
                            List.of(
                                    "LIST_MODEL_SELECTION",
                                    "JSCROLLPANE_VIEWPORT",
                                    "JSCROLLBAR_RANGE_MODEL",
                                    "JLAYEREDPANE_Z_ORDER",
                                    "JTABLE_MODEL_RENDERER_EDITOR_HEADER_SORT_FILTER_SELECTION",
                                    "JTREE_MODEL_RENDERER_EDITOR_SELECTION",
                                    "DRAG_DROP",
                                    "ACCESSIBILITY",
                                    "SWING_WORKER",
                                    "COORDINATE_CONVERSION")),
                    new Source(
                            "JAVA2S_SWING_EVENT",
                            "https://www.java2s.com/Tutorial/Java/0260__Swing-Event/Catalog0260__Swing-Event.html",
                            List.of(
                                    "EVENT_DISPATCH_THREAD",
                                    "FOCUS_EVENTS",
                                    "LIST_SELECTION_EVENTS",
                                    "MOUSE_MOTION_WHEEL_EVENTS",
                                    "TABLE_MODEL_EVENTS",
                                    "TREE_EXPAND_COLLAPSE_MODEL_SELECTION_EVENTS",
                                    "WINDOW_EVENTS")));

    private static final String ROOT = sha256(tsv());

    private NebulaM3UiBehaviorDonorCatalog() {}

    public static List<Source> sources() {
        return SOURCES;
    }

    public static boolean sourceCopyAuthority() {
        return false;
    }

    public static boolean mutationAuthority() {
        return false;
    }

    public static boolean promotionAuthority() {
        return false;
    }

    public static String root() {
        return ROOT;
    }

    public static String tsv() {
        StringBuilder out = new StringBuilder("id\turl\tobligations\n");
        for (Source source : SOURCES) {
            out.append(source.id())
                    .append('\t')
                    .append(source.url())
                    .append('\t')
                    .append(String.join(",", source.obligations()))
                    .append('\n');
        }
        return out.toString();
    }

    private static String text(String value, String field) {
        String checked = Objects.requireNonNull(value, field).strip();
        if (checked.isEmpty() || checked.indexOf('\0') >= 0) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }
}
