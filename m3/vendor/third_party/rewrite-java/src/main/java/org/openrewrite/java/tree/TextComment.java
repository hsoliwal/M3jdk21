// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/tree/TextComment.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.tree;

import org.openrewrite.Cursor;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;
import java.util.function.UnaryOperator;

public final class TextComment implements Comment {
    private final boolean multiline;
    private final String text;

    public TextComment withText(String text) {
        if (!text.equals(this.text)) {
            return new TextComment(multiline, text, suffix, markers);
        }
        return this;
    }

    private final String suffix;

    @SuppressWarnings("unchecked")
    public TextComment withSuffix(String suffix) {
        if (!suffix.equals(this.suffix)) {
            return new TextComment(multiline, text, suffix, markers);
        }
        return this;
    }

    private final Markers markers;
    private static final UnaryOperator<String> JAVA_MARKER_WRAPPER = out -> "/*~~" + out + (out.isEmpty() ? "" : "~~") + ">*/";

    @Override
    public <P> void printComment(Cursor cursor, PrintOutputCapture<P> p) {
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().beforeSyntax(marker, new Cursor(cursor, this), JAVA_MARKER_WRAPPER));
        }
        p.append(multiline ? "/*" + text + "*/" : "//" + text);
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().afterSyntax(marker, new Cursor(cursor, this), JAVA_MARKER_WRAPPER));
        }
    }

    public TextComment(final boolean multiline, final String text, final String suffix, final Markers markers) {
        this.multiline = multiline;
        this.text = text;
        this.suffix = suffix;
        this.markers = markers;
    }

    public boolean isMultiline() {
        return this.multiline;
    }

    public String getText() {
        return this.text;
    }

    public String getSuffix() {
        return this.suffix;
    }

    public Markers getMarkers() {
        return this.markers;
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof TextComment)) return false;
        final TextComment other = (TextComment) o;
        if (this.isMultiline() != other.isMultiline()) return false;
        final Object this$text = this.getText();
        final Object other$text = other.getText();
        if (this$text == null ? other$text != null : !this$text.equals(other$text)) return false;
        final Object this$suffix = this.getSuffix();
        final Object other$suffix = other.getSuffix();
        if (this$suffix == null ? other$suffix != null : !this$suffix.equals(other$suffix)) return false;
        final Object this$markers = this.getMarkers();
        final Object other$markers = other.getMarkers();
        if (this$markers == null ? other$markers != null : !this$markers.equals(other$markers)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + (this.isMultiline() ? 79 : 97);
        final Object $text = this.getText();
        result = result * PRIME + ($text == null ? 43 : $text.hashCode());
        final Object $suffix = this.getSuffix();
        result = result * PRIME + ($suffix == null ? 43 : $suffix.hashCode());
        final Object $markers = this.getMarkers();
        result = result * PRIME + ($markers == null ? 43 : $markers.hashCode());
        return result;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "TextComment(multiline=" + this.isMultiline() + ", text=" + this.getText() + ", suffix=" + this.getSuffix() + ", markers=" + this.getMarkers() + ")";
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public TextComment withMultiline(final boolean multiline) {
        return this.multiline == multiline ? this : new TextComment(multiline, this.text, this.suffix, this.markers);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public TextComment withMarkers(final Markers markers) {
        return this.markers == markers ? this : new TextComment(this.multiline, this.text, this.suffix, markers);
    }
}
