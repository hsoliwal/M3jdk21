// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/tree/Javadoc.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavadocPrinter;
import org.openrewrite.java.JavadocVisitor;
import org.openrewrite.marker.Markers;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public interface Javadoc extends Tree {
    @SuppressWarnings("unchecked")
    @Override
    default <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
        return (R) acceptJavadoc((JavadocVisitor<P>) v, p);
    }

    @Override
    default <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return v instanceof JavadocVisitor;
    }

    @Nullable
    default <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
        return v.defaultValue(this, p);
    }

    @Override
    default <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
        return new JavadocPrinter<>();
    }


    final class Attribute implements Javadoc {
        private final UUID id;
        private final Markers markers;
        private final String name;
        @Nullable
        private final List<Javadoc> spaceBeforeEqual;
        @Nullable
        private final List<Javadoc> value;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitAttribute(this, p);
        }

        public Attribute(final UUID id, final Markers markers, final String name, @Nullable final List<Javadoc> spaceBeforeEqual, @Nullable final List<Javadoc> value) {
            this.id = id;
            this.markers = markers;
            this.name = name;
            this.spaceBeforeEqual = spaceBeforeEqual;
            this.value = value;
        }

        public UUID getId() {
            return this.id;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public String getName() {
            return this.name;
        }

        @Nullable
        public List<Javadoc> getSpaceBeforeEqual() {
            return this.spaceBeforeEqual;
        }

        @Nullable
        public List<Javadoc> getValue() {
            return this.value;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "Javadoc.Attribute(id=" + this.getId() + ", markers=" + this.getMarkers() + ", name=" + this.getName() + ", spaceBeforeEqual=" + this.getSpaceBeforeEqual() + ", value=" + this.getValue() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof Javadoc.Attribute)) return false;
            final Javadoc.Attribute other = (Javadoc.Attribute) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Attribute withId(final UUID id) {
            return this.id == id ? this : new Javadoc.Attribute(id, this.markers, this.name, this.spaceBeforeEqual, this.value);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Attribute withMarkers(final Markers markers) {
            return this.markers == markers ? this : new Javadoc.Attribute(this.id, markers, this.name, this.spaceBeforeEqual, this.value);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Attribute withName(final String name) {
            return this.name == name ? this : new Javadoc.Attribute(this.id, this.markers, name, this.spaceBeforeEqual, this.value);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Attribute withSpaceBeforeEqual(@Nullable final List<Javadoc> spaceBeforeEqual) {
            return this.spaceBeforeEqual == spaceBeforeEqual ? this : new Javadoc.Attribute(this.id, this.markers, this.name, spaceBeforeEqual, this.value);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Attribute withValue(@Nullable final List<Javadoc> value) {
            return this.value == value ? this : new Javadoc.Attribute(this.id, this.markers, this.name, this.spaceBeforeEqual, value);
        }
    }


    final class Author implements Javadoc {
        private final UUID id;
        private final Markers markers;
        private final List<Javadoc> name;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitAuthor(this, p);
        }

        public Author(final UUID id, final Markers markers, final List<Javadoc> name) {
            this.id = id;
            this.markers = markers;
            this.name = name;
        }

        public UUID getId() {
            return this.id;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public List<Javadoc> getName() {
            return this.name;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "Javadoc.Author(id=" + this.getId() + ", markers=" + this.getMarkers() + ", name=" + this.getName() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof Javadoc.Author)) return false;
            final Javadoc.Author other = (Javadoc.Author) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Author withId(final UUID id) {
            return this.id == id ? this : new Javadoc.Author(id, this.markers, this.name);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Author withMarkers(final Markers markers) {
            return this.markers == markers ? this : new Javadoc.Author(this.id, markers, this.name);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Author withName(final List<Javadoc> name) {
            return this.name == name ? this : new Javadoc.Author(this.id, this.markers, name);
        }
    }


    final class Deprecated implements Javadoc {
        private final UUID id;
        private final Markers markers;
        private final List<Javadoc> description;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitDeprecated(this, p);
        }

        public Deprecated(final UUID id, final Markers markers, final List<Javadoc> description) {
            this.id = id;
            this.markers = markers;
            this.description = description;
        }

        public UUID getId() {
            return this.id;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public List<Javadoc> getDescription() {
            return this.description;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "Javadoc.Deprecated(id=" + this.getId() + ", markers=" + this.getMarkers() + ", description=" + this.getDescription() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof Javadoc.Deprecated)) return false;
            final Javadoc.Deprecated other = (Javadoc.Deprecated) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Deprecated withId(final UUID id) {
            return this.id == id ? this : new Javadoc.Deprecated(id, this.markers, this.description);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Deprecated withMarkers(final Markers markers) {
            return this.markers == markers ? this : new Javadoc.Deprecated(this.id, markers, this.description);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Deprecated withDescription(final List<Javadoc> description) {
            return this.description == description ? this : new Javadoc.Deprecated(this.id, this.markers, description);
        }
    }


    final class EndElement implements Javadoc {
        private final UUID id;
        private final Markers markers;
        private final String name;
        private final List<Javadoc> spaceBeforeEndBracket;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitEndElement(this, p);
        }

        public EndElement(final UUID id, final Markers markers, final String name, final List<Javadoc> spaceBeforeEndBracket) {
            this.id = id;
            this.markers = markers;
            this.name = name;
            this.spaceBeforeEndBracket = spaceBeforeEndBracket;
        }

        public UUID getId() {
            return this.id;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public String getName() {
            return this.name;
        }

        public List<Javadoc> getSpaceBeforeEndBracket() {
            return this.spaceBeforeEndBracket;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "Javadoc.EndElement(id=" + this.getId() + ", markers=" + this.getMarkers() + ", name=" + this.getName() + ", spaceBeforeEndBracket=" + this.getSpaceBeforeEndBracket() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof Javadoc.EndElement)) return false;
            final Javadoc.EndElement other = (Javadoc.EndElement) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.EndElement withId(final UUID id) {
            return this.id == id ? this : new Javadoc.EndElement(id, this.markers, this.name, this.spaceBeforeEndBracket);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.EndElement withMarkers(final Markers markers) {
            return this.markers == markers ? this : new Javadoc.EndElement(this.id, markers, this.name, this.spaceBeforeEndBracket);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.EndElement withName(final String name) {
            return this.name == name ? this : new Javadoc.EndElement(this.id, this.markers, name, this.spaceBeforeEndBracket);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.EndElement withSpaceBeforeEndBracket(final List<Javadoc> spaceBeforeEndBracket) {
            return this.spaceBeforeEndBracket == spaceBeforeEndBracket ? this : new Javadoc.EndElement(this.id, this.markers, this.name, spaceBeforeEndBracket);
        }
    }


    final class DocComment implements Javadoc, Comment {
        private final UUID id;
        private final Markers markers;
        private final List<Javadoc> body;
        private final String suffix;

        @SuppressWarnings("unchecked")
        public DocComment withSuffix(String suffix) {
            if (!suffix.equals(this.suffix)) {
                return new DocComment(id, markers, body, suffix);
            }
            return this;
        }

        @Override
        public boolean isMultiline() {
            return true;
        }

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitDocComment(this, p);
        }

        @Override
        public <P> void printComment(Cursor cursor, PrintOutputCapture<P> print) {
            new JavadocPrinter<P>().visit(this, print, cursor);
        }

        public DocComment(final UUID id, final Markers markers, final List<Javadoc> body, final String suffix) {
            this.id = id;
            this.markers = markers;
            this.body = body;
            this.suffix = suffix;
        }

        public UUID getId() {
            return this.id;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public List<Javadoc> getBody() {
            return this.body;
        }

        public String getSuffix() {
            return this.suffix;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "Javadoc.DocComment(id=" + this.getId() + ", markers=" + this.getMarkers() + ", body=" + this.getBody() + ", suffix=" + this.getSuffix() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof Javadoc.DocComment)) return false;
            final Javadoc.DocComment other = (Javadoc.DocComment) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.DocComment withId(final UUID id) {
            return this.id == id ? this : new Javadoc.DocComment(id, this.markers, this.body, this.suffix);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.DocComment withMarkers(final Markers markers) {
            return this.markers == markers ? this : new Javadoc.DocComment(this.id, markers, this.body, this.suffix);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.DocComment withBody(final List<Javadoc> body) {
            return this.body == body ? this : new Javadoc.DocComment(this.id, this.markers, body, this.suffix);
        }
    }


    final class DocRoot implements Javadoc {
        private final UUID id;
        private final Markers markers;
        private final List<Javadoc> endBrace;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitDocRoot(this, p);
        }

        public DocRoot(final UUID id, final Markers markers, final List<Javadoc> endBrace) {
            this.id = id;
            this.markers = markers;
            this.endBrace = endBrace;
        }

        public UUID getId() {
            return this.id;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public List<Javadoc> getEndBrace() {
            return this.endBrace;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "Javadoc.DocRoot(id=" + this.getId() + ", markers=" + this.getMarkers() + ", endBrace=" + this.getEndBrace() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof Javadoc.DocRoot)) return false;
            final Javadoc.DocRoot other = (Javadoc.DocRoot) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.DocRoot withId(final UUID id) {
            return this.id == id ? this : new Javadoc.DocRoot(id, this.markers, this.endBrace);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.DocRoot withMarkers(final Markers markers) {
            return this.markers == markers ? this : new Javadoc.DocRoot(this.id, markers, this.endBrace);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.DocRoot withEndBrace(final List<Javadoc> endBrace) {
            return this.endBrace == endBrace ? this : new Javadoc.DocRoot(this.id, this.markers, endBrace);
        }
    }


    final class DocType implements Javadoc {
        private final UUID id;
        private final Markers markers;
        private final List<Javadoc> text;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitDocType(this, p);
        }

        public DocType(final UUID id, final Markers markers, final List<Javadoc> text) {
            this.id = id;
            this.markers = markers;
            this.text = text;
        }

        public UUID getId() {
            return this.id;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public List<Javadoc> getText() {
            return this.text;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "Javadoc.DocType(id=" + this.getId() + ", markers=" + this.getMarkers() + ", text=" + this.getText() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof Javadoc.DocType)) return false;
            final Javadoc.DocType other = (Javadoc.DocType) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.DocType withId(final UUID id) {
            return this.id == id ? this : new Javadoc.DocType(id, this.markers, this.text);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.DocType withMarkers(final Markers markers) {
            return this.markers == markers ? this : new Javadoc.DocType(this.id, markers, this.text);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.DocType withText(final List<Javadoc> text) {
            return this.text == text ? this : new Javadoc.DocType(this.id, this.markers, text);
        }
    }


    final class Erroneous implements Javadoc {
        private final UUID id;
        private final Markers markers;
        private final List<Javadoc> text;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitErroneous(this, p);
        }

        public Erroneous(final UUID id, final Markers markers, final List<Javadoc> text) {
            this.id = id;
            this.markers = markers;
            this.text = text;
        }

        public UUID getId() {
            return this.id;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public List<Javadoc> getText() {
            return this.text;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "Javadoc.Erroneous(id=" + this.getId() + ", markers=" + this.getMarkers() + ", text=" + this.getText() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof Javadoc.Erroneous)) return false;
            final Javadoc.Erroneous other = (Javadoc.Erroneous) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Erroneous withId(final UUID id) {
            return this.id == id ? this : new Javadoc.Erroneous(id, this.markers, this.text);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Erroneous withMarkers(final Markers markers) {
            return this.markers == markers ? this : new Javadoc.Erroneous(this.id, markers, this.text);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Erroneous withText(final List<Javadoc> text) {
            return this.text == text ? this : new Javadoc.Erroneous(this.id, this.markers, text);
        }
    }


    final class InheritDoc implements Javadoc {
        private final UUID id;
        private final Markers markers;
        private final List<Javadoc> endBrace;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitInheritDoc(this, p);
        }

        public InheritDoc(final UUID id, final Markers markers, final List<Javadoc> endBrace) {
            this.id = id;
            this.markers = markers;
            this.endBrace = endBrace;
        }

        public UUID getId() {
            return this.id;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public List<Javadoc> getEndBrace() {
            return this.endBrace;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "Javadoc.InheritDoc(id=" + this.getId() + ", markers=" + this.getMarkers() + ", endBrace=" + this.getEndBrace() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof Javadoc.InheritDoc)) return false;
            final Javadoc.InheritDoc other = (Javadoc.InheritDoc) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.InheritDoc withId(final UUID id) {
            return this.id == id ? this : new Javadoc.InheritDoc(id, this.markers, this.endBrace);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.InheritDoc withMarkers(final Markers markers) {
            return this.markers == markers ? this : new Javadoc.InheritDoc(this.id, markers, this.endBrace);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.InheritDoc withEndBrace(final List<Javadoc> endBrace) {
            return this.endBrace == endBrace ? this : new Javadoc.InheritDoc(this.id, this.markers, endBrace);
        }
    }


    final class Hidden implements Javadoc {
        private final UUID id;
        private final Markers markers;
        private final List<Javadoc> body;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitHidden(this, p);
        }

        public Hidden(final UUID id, final Markers markers, final List<Javadoc> body) {
            this.id = id;
            this.markers = markers;
            this.body = body;
        }

        public UUID getId() {
            return this.id;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public List<Javadoc> getBody() {
            return this.body;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "Javadoc.Hidden(id=" + this.getId() + ", markers=" + this.getMarkers() + ", body=" + this.getBody() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof Javadoc.Hidden)) return false;
            final Javadoc.Hidden other = (Javadoc.Hidden) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Hidden withId(final UUID id) {
            return this.id == id ? this : new Javadoc.Hidden(id, this.markers, this.body);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Hidden withMarkers(final Markers markers) {
            return this.markers == markers ? this : new Javadoc.Hidden(this.id, markers, this.body);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Hidden withBody(final List<Javadoc> body) {
            return this.body == body ? this : new Javadoc.Hidden(this.id, this.markers, body);
        }
    }


    final class Index implements Javadoc {
        private final UUID id;
        private final Markers markers;
        private final List<Javadoc> searchTerm;
        private final List<Javadoc> description;
        private final List<Javadoc> endBrace;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitIndex(this, p);
        }

        public Index(final UUID id, final Markers markers, final List<Javadoc> searchTerm, final List<Javadoc> description, final List<Javadoc> endBrace) {
            this.id = id;
            this.markers = markers;
            this.searchTerm = searchTerm;
            this.description = description;
            this.endBrace = endBrace;
        }

        public UUID getId() {
            return this.id;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public List<Javadoc> getSearchTerm() {
            return this.searchTerm;
        }

        public List<Javadoc> getDescription() {
            return this.description;
        }

        public List<Javadoc> getEndBrace() {
            return this.endBrace;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "Javadoc.Index(id=" + this.getId() + ", markers=" + this.getMarkers() + ", searchTerm=" + this.getSearchTerm() + ", description=" + this.getDescription() + ", endBrace=" + this.getEndBrace() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof Javadoc.Index)) return false;
            final Javadoc.Index other = (Javadoc.Index) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Index withId(final UUID id) {
            return this.id == id ? this : new Javadoc.Index(id, this.markers, this.searchTerm, this.description, this.endBrace);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Index withMarkers(final Markers markers) {
            return this.markers == markers ? this : new Javadoc.Index(this.id, markers, this.searchTerm, this.description, this.endBrace);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Index withSearchTerm(final List<Javadoc> searchTerm) {
            return this.searchTerm == searchTerm ? this : new Javadoc.Index(this.id, this.markers, searchTerm, this.description, this.endBrace);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Index withDescription(final List<Javadoc> description) {
            return this.description == description ? this : new Javadoc.Index(this.id, this.markers, this.searchTerm, description, this.endBrace);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Index withEndBrace(final List<Javadoc> endBrace) {
            return this.endBrace == endBrace ? this : new Javadoc.Index(this.id, this.markers, this.searchTerm, this.description, endBrace);
        }
    }


    final class InlinedValue implements Javadoc {
        private final UUID id;
        private final Markers markers;
        private final List<Javadoc> spaceBeforeTree;
        @Nullable
        private final J tree;
        private final List<Javadoc> endBrace;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitInlinedValue(this, p);
        }

        public InlinedValue(final UUID id, final Markers markers, final List<Javadoc> spaceBeforeTree, @Nullable final J tree, final List<Javadoc> endBrace) {
            this.id = id;
            this.markers = markers;
            this.spaceBeforeTree = spaceBeforeTree;
            this.tree = tree;
            this.endBrace = endBrace;
        }

        public UUID getId() {
            return this.id;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public List<Javadoc> getSpaceBeforeTree() {
            return this.spaceBeforeTree;
        }

        @Nullable
        public J getTree() {
            return this.tree;
        }

        public List<Javadoc> getEndBrace() {
            return this.endBrace;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "Javadoc.InlinedValue(id=" + this.getId() + ", markers=" + this.getMarkers() + ", spaceBeforeTree=" + this.getSpaceBeforeTree() + ", tree=" + this.getTree() + ", endBrace=" + this.getEndBrace() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof Javadoc.InlinedValue)) return false;
            final Javadoc.InlinedValue other = (Javadoc.InlinedValue) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.InlinedValue withId(final UUID id) {
            return this.id == id ? this : new Javadoc.InlinedValue(id, this.markers, this.spaceBeforeTree, this.tree, this.endBrace);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.InlinedValue withMarkers(final Markers markers) {
            return this.markers == markers ? this : new Javadoc.InlinedValue(this.id, markers, this.spaceBeforeTree, this.tree, this.endBrace);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.InlinedValue withSpaceBeforeTree(final List<Javadoc> spaceBeforeTree) {
            return this.spaceBeforeTree == spaceBeforeTree ? this : new Javadoc.InlinedValue(this.id, this.markers, spaceBeforeTree, this.tree, this.endBrace);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.InlinedValue withTree(@Nullable final J tree) {
            return this.tree == tree ? this : new Javadoc.InlinedValue(this.id, this.markers, this.spaceBeforeTree, tree, this.endBrace);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.InlinedValue withEndBrace(final List<Javadoc> endBrace) {
            return this.endBrace == endBrace ? this : new Javadoc.InlinedValue(this.id, this.markers, this.spaceBeforeTree, this.tree, endBrace);
        }
    }


    /**
     * Holds line break plus the margin that starts the next line. The margin in many cases
     * ends in a '*'.
     */
    final class LineBreak implements Javadoc {
        private final UUID id;
        private final String margin;
        private final Markers markers;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitLineBreak(this, p);
        }

        public LineBreak withMargin(String margin) {
            if (margin.equals(this.margin)) {
                return this;
            }
            return new LineBreak(this.id, margin, this.markers);
        }

        @SuppressWarnings("unchecked")
        public LineBreak withMarkers(Markers markers) {
            if (markers == this.markers) {
                return this;
            }
            return new LineBreak(id, margin, markers);
        }

        public LineBreak(final UUID id, final String margin, final Markers markers) {
            this.id = id;
            this.margin = margin;
            this.markers = markers;
        }

        public UUID getId() {
            return this.id;
        }

        public String getMargin() {
            return this.margin;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "Javadoc.LineBreak(id=" + this.getId() + ", margin=" + this.getMargin() + ", markers=" + this.getMarkers() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof Javadoc.LineBreak)) return false;
            final Javadoc.LineBreak other = (Javadoc.LineBreak) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.LineBreak withId(final UUID id) {
            return this.id == id ? this : new Javadoc.LineBreak(id, this.margin, this.markers);
        }
    }


    final class Link implements Javadoc {
        private final UUID id;
        private final Markers markers;
        private final boolean plain;
        private final List<Javadoc> spaceBeforeTree;
        @Nullable
        private final J tree;

        @Nullable
        public Reference getTreeReference() {
            if (tree != null && treeReference == null) {
                treeReference = new Reference(Tree.randomId(), Markers.EMPTY, tree, null);
            }
            return treeReference;
        }

        // This is non-final to maintain backwards compatibility.
        @Nullable
        private Reference treeReference;
        private final List<Javadoc> label;
        private final List<Javadoc> endBrace;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitLink(this, p);
        }

        public Link(final UUID id, final Markers markers, final boolean plain, final List<Javadoc> spaceBeforeTree, @Nullable final J tree, @Nullable final Reference treeReference, final List<Javadoc> label, final List<Javadoc> endBrace) {
            this.id = id;
            this.markers = markers;
            this.plain = plain;
            this.spaceBeforeTree = spaceBeforeTree;
            this.tree = tree;
            this.treeReference = treeReference;
            this.label = label;
            this.endBrace = endBrace;
        }

        public UUID getId() {
            return this.id;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public boolean isPlain() {
            return this.plain;
        }

        public List<Javadoc> getSpaceBeforeTree() {
            return this.spaceBeforeTree;
        }

        @Nullable
        public J getTree() {
            return this.tree;
        }

        public List<Javadoc> getLabel() {
            return this.label;
        }

        public List<Javadoc> getEndBrace() {
            return this.endBrace;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "Javadoc.Link(id=" + this.getId() + ", markers=" + this.getMarkers() + ", plain=" + this.isPlain() + ", spaceBeforeTree=" + this.getSpaceBeforeTree() + ", tree=" + this.getTree() + ", treeReference=" + this.getTreeReference() + ", label=" + this.getLabel() + ", endBrace=" + this.getEndBrace() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof Javadoc.Link)) return false;
            final Javadoc.Link other = (Javadoc.Link) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Link withId(final UUID id) {
            return this.id == id ? this : new Javadoc.Link(id, this.markers, this.plain, this.spaceBeforeTree, this.tree, this.treeReference, this.label, this.endBrace);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Link withMarkers(final Markers markers) {
            return this.markers == markers ? this : new Javadoc.Link(this.id, markers, this.plain, this.spaceBeforeTree, this.tree, this.treeReference, this.label, this.endBrace);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Link withPlain(final boolean plain) {
            return this.plain == plain ? this : new Javadoc.Link(this.id, this.markers, plain, this.spaceBeforeTree, this.tree, this.treeReference, this.label, this.endBrace);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Link withSpaceBeforeTree(final List<Javadoc> spaceBeforeTree) {
            return this.spaceBeforeTree == spaceBeforeTree ? this : new Javadoc.Link(this.id, this.markers, this.plain, spaceBeforeTree, this.tree, this.treeReference, this.label, this.endBrace);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Link withTree(@Nullable final J tree) {
            return this.tree == tree ? this : new Javadoc.Link(this.id, this.markers, this.plain, this.spaceBeforeTree, tree, this.treeReference, this.label, this.endBrace);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Link withTreeReference(@Nullable final Reference treeReference) {
            return this.treeReference == treeReference ? this : new Javadoc.Link(this.id, this.markers, this.plain, this.spaceBeforeTree, this.tree, treeReference, this.label, this.endBrace);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Link withLabel(final List<Javadoc> label) {
            return this.label == label ? this : new Javadoc.Link(this.id, this.markers, this.plain, this.spaceBeforeTree, this.tree, this.treeReference, label, this.endBrace);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Link withEndBrace(final List<Javadoc> endBrace) {
            return this.endBrace == endBrace ? this : new Javadoc.Link(this.id, this.markers, this.plain, this.spaceBeforeTree, this.tree, this.treeReference, this.label, endBrace);
        }
    }


    final class Literal implements Javadoc {
        private final UUID id;
        private final Markers markers;
        private final boolean code;
        private final List<Javadoc> description;
        private final List<Javadoc> endBrace;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitLiteral(this, p);
        }

        public Literal(final UUID id, final Markers markers, final boolean code, final List<Javadoc> description, final List<Javadoc> endBrace) {
            this.id = id;
            this.markers = markers;
            this.code = code;
            this.description = description;
            this.endBrace = endBrace;
        }

        public UUID getId() {
            return this.id;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public boolean isCode() {
            return this.code;
        }

        public List<Javadoc> getDescription() {
            return this.description;
        }

        public List<Javadoc> getEndBrace() {
            return this.endBrace;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "Javadoc.Literal(id=" + this.getId() + ", markers=" + this.getMarkers() + ", code=" + this.isCode() + ", description=" + this.getDescription() + ", endBrace=" + this.getEndBrace() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof Javadoc.Literal)) return false;
            final Javadoc.Literal other = (Javadoc.Literal) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Literal withId(final UUID id) {
            return this.id == id ? this : new Javadoc.Literal(id, this.markers, this.code, this.description, this.endBrace);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Literal withMarkers(final Markers markers) {
            return this.markers == markers ? this : new Javadoc.Literal(this.id, markers, this.code, this.description, this.endBrace);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Literal withCode(final boolean code) {
            return this.code == code ? this : new Javadoc.Literal(this.id, this.markers, code, this.description, this.endBrace);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Literal withDescription(final List<Javadoc> description) {
            return this.description == description ? this : new Javadoc.Literal(this.id, this.markers, this.code, description, this.endBrace);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Literal withEndBrace(final List<Javadoc> endBrace) {
            return this.endBrace == endBrace ? this : new Javadoc.Literal(this.id, this.markers, this.code, this.description, endBrace);
        }
    }


    final class Parameter implements Javadoc {
        private final UUID id;
        private final Markers markers;
        private final List<Javadoc> spaceBeforeName;
        @Nullable
        private final J name;

        @Nullable
        public Reference getNameReference() {
            if (name != null && nameReference == null) {
                nameReference = new Reference(Tree.randomId(), Markers.EMPTY, name, null);
            }
            return nameReference;
        }

        // This is non-final to maintain backwards compatibility.
        @Nullable
        private Reference nameReference;
        private final List<Javadoc> description;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitParameter(this, p);
        }

        public Parameter(final UUID id, final Markers markers, final List<Javadoc> spaceBeforeName, @Nullable final J name, @Nullable final Reference nameReference, final List<Javadoc> description) {
            this.id = id;
            this.markers = markers;
            this.spaceBeforeName = spaceBeforeName;
            this.name = name;
            this.nameReference = nameReference;
            this.description = description;
        }

        public UUID getId() {
            return this.id;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public List<Javadoc> getSpaceBeforeName() {
            return this.spaceBeforeName;
        }

        @Nullable
        public J getName() {
            return this.name;
        }

        public List<Javadoc> getDescription() {
            return this.description;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "Javadoc.Parameter(id=" + this.getId() + ", markers=" + this.getMarkers() + ", spaceBeforeName=" + this.getSpaceBeforeName() + ", name=" + this.getName() + ", nameReference=" + this.getNameReference() + ", description=" + this.getDescription() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof Javadoc.Parameter)) return false;
            final Javadoc.Parameter other = (Javadoc.Parameter) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Parameter withId(final UUID id) {
            return this.id == id ? this : new Javadoc.Parameter(id, this.markers, this.spaceBeforeName, this.name, this.nameReference, this.description);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Parameter withMarkers(final Markers markers) {
            return this.markers == markers ? this : new Javadoc.Parameter(this.id, markers, this.spaceBeforeName, this.name, this.nameReference, this.description);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Parameter withSpaceBeforeName(final List<Javadoc> spaceBeforeName) {
            return this.spaceBeforeName == spaceBeforeName ? this : new Javadoc.Parameter(this.id, this.markers, spaceBeforeName, this.name, this.nameReference, this.description);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Parameter withName(@Nullable final J name) {
            return this.name == name ? this : new Javadoc.Parameter(this.id, this.markers, this.spaceBeforeName, name, this.nameReference, this.description);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Parameter withNameReference(@Nullable final Reference nameReference) {
            return this.nameReference == nameReference ? this : new Javadoc.Parameter(this.id, this.markers, this.spaceBeforeName, this.name, nameReference, this.description);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Parameter withDescription(final List<Javadoc> description) {
            return this.description == description ? this : new Javadoc.Parameter(this.id, this.markers, this.spaceBeforeName, this.name, this.nameReference, description);
        }
    }


    final class Provides implements Javadoc {
        private final UUID id;
        private final Markers markers;
        private final List<Javadoc> spaceBeforeServiceType;
        private final J serviceType;
        private final List<Javadoc> description;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitProvides(this, p);
        }

        public Provides(final UUID id, final Markers markers, final List<Javadoc> spaceBeforeServiceType, final J serviceType, final List<Javadoc> description) {
            this.id = id;
            this.markers = markers;
            this.spaceBeforeServiceType = spaceBeforeServiceType;
            this.serviceType = serviceType;
            this.description = description;
        }

        public UUID getId() {
            return this.id;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public List<Javadoc> getSpaceBeforeServiceType() {
            return this.spaceBeforeServiceType;
        }

        public J getServiceType() {
            return this.serviceType;
        }

        public List<Javadoc> getDescription() {
            return this.description;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "Javadoc.Provides(id=" + this.getId() + ", markers=" + this.getMarkers() + ", spaceBeforeServiceType=" + this.getSpaceBeforeServiceType() + ", serviceType=" + this.getServiceType() + ", description=" + this.getDescription() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof Javadoc.Provides)) return false;
            final Javadoc.Provides other = (Javadoc.Provides) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Provides withId(final UUID id) {
            return this.id == id ? this : new Javadoc.Provides(id, this.markers, this.spaceBeforeServiceType, this.serviceType, this.description);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Provides withMarkers(final Markers markers) {
            return this.markers == markers ? this : new Javadoc.Provides(this.id, markers, this.spaceBeforeServiceType, this.serviceType, this.description);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Provides withSpaceBeforeServiceType(final List<Javadoc> spaceBeforeServiceType) {
            return this.spaceBeforeServiceType == spaceBeforeServiceType ? this : new Javadoc.Provides(this.id, this.markers, spaceBeforeServiceType, this.serviceType, this.description);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Provides withServiceType(final J serviceType) {
            return this.serviceType == serviceType ? this : new Javadoc.Provides(this.id, this.markers, this.spaceBeforeServiceType, serviceType, this.description);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Provides withDescription(final List<Javadoc> description) {
            return this.description == description ? this : new Javadoc.Provides(this.id, this.markers, this.spaceBeforeServiceType, this.serviceType, description);
        }
    }


    final class Return implements Javadoc {
        private final UUID id;
        private final Markers markers;
        private final List<Javadoc> description;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitReturn(this, p);
        }

        public Return(final UUID id, final Markers markers, final List<Javadoc> description) {
            this.id = id;
            this.markers = markers;
            this.description = description;
        }

        public UUID getId() {
            return this.id;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public List<Javadoc> getDescription() {
            return this.description;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "Javadoc.Return(id=" + this.getId() + ", markers=" + this.getMarkers() + ", description=" + this.getDescription() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof Javadoc.Return)) return false;
            final Javadoc.Return other = (Javadoc.Return) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Return withId(final UUID id) {
            return this.id == id ? this : new Javadoc.Return(id, this.markers, this.description);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Return withMarkers(final Markers markers) {
            return this.markers == markers ? this : new Javadoc.Return(this.id, markers, this.description);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Return withDescription(final List<Javadoc> description) {
            return this.description == description ? this : new Javadoc.Return(this.id, this.markers, description);
        }
    }


    final class See implements Javadoc {
        private final UUID id;
        private final Markers markers;
        private final List<Javadoc> spaceBeforeTree;
        @Nullable
        private final J tree;

        @Nullable
        public Reference getTreeReference() {
            if (tree != null && treeReference == null) {
                treeReference = new Reference(Tree.randomId(), Markers.EMPTY, tree, null);
            }
            return treeReference;
        }

        // This is non-final to maintain backwards compatibility.
        @Nullable
        private Reference treeReference;
        private final List<Javadoc> reference;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitSee(this, p);
        }

        public See(final UUID id, final Markers markers, final List<Javadoc> spaceBeforeTree, @Nullable final J tree, @Nullable final Reference treeReference, final List<Javadoc> reference) {
            this.id = id;
            this.markers = markers;
            this.spaceBeforeTree = spaceBeforeTree;
            this.tree = tree;
            this.treeReference = treeReference;
            this.reference = reference;
        }

        public UUID getId() {
            return this.id;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public List<Javadoc> getSpaceBeforeTree() {
            return this.spaceBeforeTree;
        }

        @Nullable
        public J getTree() {
            return this.tree;
        }

        public List<Javadoc> getReference() {
            return this.reference;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "Javadoc.See(id=" + this.getId() + ", markers=" + this.getMarkers() + ", spaceBeforeTree=" + this.getSpaceBeforeTree() + ", tree=" + this.getTree() + ", treeReference=" + this.getTreeReference() + ", reference=" + this.getReference() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof Javadoc.See)) return false;
            final Javadoc.See other = (Javadoc.See) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.See withId(final UUID id) {
            return this.id == id ? this : new Javadoc.See(id, this.markers, this.spaceBeforeTree, this.tree, this.treeReference, this.reference);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.See withMarkers(final Markers markers) {
            return this.markers == markers ? this : new Javadoc.See(this.id, markers, this.spaceBeforeTree, this.tree, this.treeReference, this.reference);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.See withSpaceBeforeTree(final List<Javadoc> spaceBeforeTree) {
            return this.spaceBeforeTree == spaceBeforeTree ? this : new Javadoc.See(this.id, this.markers, spaceBeforeTree, this.tree, this.treeReference, this.reference);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.See withTree(@Nullable final J tree) {
            return this.tree == tree ? this : new Javadoc.See(this.id, this.markers, this.spaceBeforeTree, tree, this.treeReference, this.reference);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.See withTreeReference(@Nullable final Reference treeReference) {
            return this.treeReference == treeReference ? this : new Javadoc.See(this.id, this.markers, this.spaceBeforeTree, this.tree, treeReference, this.reference);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.See withReference(final List<Javadoc> reference) {
            return this.reference == reference ? this : new Javadoc.See(this.id, this.markers, this.spaceBeforeTree, this.tree, this.treeReference, reference);
        }
    }


    final class Serial implements Javadoc {
        private final UUID id;
        private final Markers markers;
        private final List<Javadoc> description;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitSerial(this, p);
        }

        public Serial(final UUID id, final Markers markers, final List<Javadoc> description) {
            this.id = id;
            this.markers = markers;
            this.description = description;
        }

        public UUID getId() {
            return this.id;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public List<Javadoc> getDescription() {
            return this.description;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "Javadoc.Serial(id=" + this.getId() + ", markers=" + this.getMarkers() + ", description=" + this.getDescription() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof Javadoc.Serial)) return false;
            final Javadoc.Serial other = (Javadoc.Serial) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Serial withId(final UUID id) {
            return this.id == id ? this : new Javadoc.Serial(id, this.markers, this.description);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Serial withMarkers(final Markers markers) {
            return this.markers == markers ? this : new Javadoc.Serial(this.id, markers, this.description);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Serial withDescription(final List<Javadoc> description) {
            return this.description == description ? this : new Javadoc.Serial(this.id, this.markers, description);
        }
    }


    final class SerialData implements Javadoc {
        private final UUID id;
        private final Markers markers;
        private final List<Javadoc> description;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitSerialData(this, p);
        }

        public SerialData(final UUID id, final Markers markers, final List<Javadoc> description) {
            this.id = id;
            this.markers = markers;
            this.description = description;
        }

        public UUID getId() {
            return this.id;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public List<Javadoc> getDescription() {
            return this.description;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "Javadoc.SerialData(id=" + this.getId() + ", markers=" + this.getMarkers() + ", description=" + this.getDescription() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof Javadoc.SerialData)) return false;
            final Javadoc.SerialData other = (Javadoc.SerialData) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.SerialData withId(final UUID id) {
            return this.id == id ? this : new Javadoc.SerialData(id, this.markers, this.description);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.SerialData withMarkers(final Markers markers) {
            return this.markers == markers ? this : new Javadoc.SerialData(this.id, markers, this.description);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.SerialData withDescription(final List<Javadoc> description) {
            return this.description == description ? this : new Javadoc.SerialData(this.id, this.markers, description);
        }
    }


    final class SerialField implements Javadoc {
        private final UUID id;
        private final Markers markers;
        private final J.Identifier name;
        private final J type;
        private final List<Javadoc> description;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitSerialField(this, p);
        }

        public SerialField(final UUID id, final Markers markers, final J.Identifier name, final J type, final List<Javadoc> description) {
            this.id = id;
            this.markers = markers;
            this.name = name;
            this.type = type;
            this.description = description;
        }

        public UUID getId() {
            return this.id;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public J.Identifier getName() {
            return this.name;
        }

        public J getType() {
            return this.type;
        }

        public List<Javadoc> getDescription() {
            return this.description;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "Javadoc.SerialField(id=" + this.getId() + ", markers=" + this.getMarkers() + ", name=" + this.getName() + ", type=" + this.getType() + ", description=" + this.getDescription() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof Javadoc.SerialField)) return false;
            final Javadoc.SerialField other = (Javadoc.SerialField) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.SerialField withId(final UUID id) {
            return this.id == id ? this : new Javadoc.SerialField(id, this.markers, this.name, this.type, this.description);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.SerialField withMarkers(final Markers markers) {
            return this.markers == markers ? this : new Javadoc.SerialField(this.id, markers, this.name, this.type, this.description);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.SerialField withName(final J.Identifier name) {
            return this.name == name ? this : new Javadoc.SerialField(this.id, this.markers, name, this.type, this.description);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.SerialField withType(final J type) {
            return this.type == type ? this : new Javadoc.SerialField(this.id, this.markers, this.name, type, this.description);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.SerialField withDescription(final List<Javadoc> description) {
            return this.description == description ? this : new Javadoc.SerialField(this.id, this.markers, this.name, this.type, description);
        }
    }


    final class Since implements Javadoc {
        private final UUID id;
        private final Markers markers;
        private final List<Javadoc> description;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitSince(this, p);
        }

        public Since(final UUID id, final Markers markers, final List<Javadoc> description) {
            this.id = id;
            this.markers = markers;
            this.description = description;
        }

        public UUID getId() {
            return this.id;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public List<Javadoc> getDescription() {
            return this.description;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "Javadoc.Since(id=" + this.getId() + ", markers=" + this.getMarkers() + ", description=" + this.getDescription() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof Javadoc.Since)) return false;
            final Javadoc.Since other = (Javadoc.Since) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Since withId(final UUID id) {
            return this.id == id ? this : new Javadoc.Since(id, this.markers, this.description);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Since withMarkers(final Markers markers) {
            return this.markers == markers ? this : new Javadoc.Since(this.id, markers, this.description);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Since withDescription(final List<Javadoc> description) {
            return this.description == description ? this : new Javadoc.Since(this.id, this.markers, description);
        }
    }


    final class StartElement implements Javadoc {
        private final UUID id;
        private final Markers markers;
        private final String name;
        private final List<Javadoc> attributes;
        private final boolean selfClosing;
        private final List<Javadoc> spaceBeforeEndBracket;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitStartElement(this, p);
        }

        public StartElement(final UUID id, final Markers markers, final String name, final List<Javadoc> attributes, final boolean selfClosing, final List<Javadoc> spaceBeforeEndBracket) {
            this.id = id;
            this.markers = markers;
            this.name = name;
            this.attributes = attributes;
            this.selfClosing = selfClosing;
            this.spaceBeforeEndBracket = spaceBeforeEndBracket;
        }

        public UUID getId() {
            return this.id;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public String getName() {
            return this.name;
        }

        public List<Javadoc> getAttributes() {
            return this.attributes;
        }

        public boolean isSelfClosing() {
            return this.selfClosing;
        }

        public List<Javadoc> getSpaceBeforeEndBracket() {
            return this.spaceBeforeEndBracket;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "Javadoc.StartElement(id=" + this.getId() + ", markers=" + this.getMarkers() + ", name=" + this.getName() + ", attributes=" + this.getAttributes() + ", selfClosing=" + this.isSelfClosing() + ", spaceBeforeEndBracket=" + this.getSpaceBeforeEndBracket() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof Javadoc.StartElement)) return false;
            final Javadoc.StartElement other = (Javadoc.StartElement) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.StartElement withId(final UUID id) {
            return this.id == id ? this : new Javadoc.StartElement(id, this.markers, this.name, this.attributes, this.selfClosing, this.spaceBeforeEndBracket);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.StartElement withMarkers(final Markers markers) {
            return this.markers == markers ? this : new Javadoc.StartElement(this.id, markers, this.name, this.attributes, this.selfClosing, this.spaceBeforeEndBracket);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.StartElement withName(final String name) {
            return this.name == name ? this : new Javadoc.StartElement(this.id, this.markers, name, this.attributes, this.selfClosing, this.spaceBeforeEndBracket);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.StartElement withAttributes(final List<Javadoc> attributes) {
            return this.attributes == attributes ? this : new Javadoc.StartElement(this.id, this.markers, this.name, attributes, this.selfClosing, this.spaceBeforeEndBracket);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.StartElement withSelfClosing(final boolean selfClosing) {
            return this.selfClosing == selfClosing ? this : new Javadoc.StartElement(this.id, this.markers, this.name, this.attributes, selfClosing, this.spaceBeforeEndBracket);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.StartElement withSpaceBeforeEndBracket(final List<Javadoc> spaceBeforeEndBracket) {
            return this.spaceBeforeEndBracket == spaceBeforeEndBracket ? this : new Javadoc.StartElement(this.id, this.markers, this.name, this.attributes, this.selfClosing, spaceBeforeEndBracket);
        }
    }


    final class Summary implements Javadoc {
        private final UUID id;
        private final Markers markers;
        private final List<Javadoc> summary;
        private final List<Javadoc> beforeBrace;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitSummary(this, p);
        }

        public Summary(final UUID id, final Markers markers, final List<Javadoc> summary, final List<Javadoc> beforeBrace) {
            this.id = id;
            this.markers = markers;
            this.summary = summary;
            this.beforeBrace = beforeBrace;
        }

        public UUID getId() {
            return this.id;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public List<Javadoc> getSummary() {
            return this.summary;
        }

        public List<Javadoc> getBeforeBrace() {
            return this.beforeBrace;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "Javadoc.Summary(id=" + this.getId() + ", markers=" + this.getMarkers() + ", summary=" + this.getSummary() + ", beforeBrace=" + this.getBeforeBrace() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof Javadoc.Summary)) return false;
            final Javadoc.Summary other = (Javadoc.Summary) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Summary withId(final UUID id) {
            return this.id == id ? this : new Javadoc.Summary(id, this.markers, this.summary, this.beforeBrace);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Summary withMarkers(final Markers markers) {
            return this.markers == markers ? this : new Javadoc.Summary(this.id, markers, this.summary, this.beforeBrace);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Summary withSummary(final List<Javadoc> summary) {
            return this.summary == summary ? this : new Javadoc.Summary(this.id, this.markers, summary, this.beforeBrace);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Summary withBeforeBrace(final List<Javadoc> beforeBrace) {
            return this.beforeBrace == beforeBrace ? this : new Javadoc.Summary(this.id, this.markers, this.summary, beforeBrace);
        }
    }


    final class Text implements Javadoc {
        private final UUID id;
        private final Markers markers;
        private final String text;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitText(this, p);
        }

        public Text(final UUID id, final Markers markers, final String text) {
            this.id = id;
            this.markers = markers;
            this.text = text;
        }

        public UUID getId() {
            return this.id;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public String getText() {
            return this.text;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "Javadoc.Text(id=" + this.getId() + ", markers=" + this.getMarkers() + ", text=" + this.getText() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof Javadoc.Text)) return false;
            final Javadoc.Text other = (Javadoc.Text) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Text withId(final UUID id) {
            return this.id == id ? this : new Javadoc.Text(id, this.markers, this.text);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Text withMarkers(final Markers markers) {
            return this.markers == markers ? this : new Javadoc.Text(this.id, markers, this.text);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Text withText(final String text) {
            return this.text == text ? this : new Javadoc.Text(this.id, this.markers, text);
        }
    }


    final class Throws implements Javadoc {
        private final UUID id;
        private final Markers markers;
        /**
         * {@code @throws} and {@code @exception} are synonyms.
         */
        private final boolean throwsKeyword;
        private final List<Javadoc> spaceBeforeExceptionName;
        private final J exceptionName;
        private final List<Javadoc> description;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitThrows(this, p);
        }

        public Throws(final UUID id, final Markers markers, final boolean throwsKeyword, final List<Javadoc> spaceBeforeExceptionName, final J exceptionName, final List<Javadoc> description) {
            this.id = id;
            this.markers = markers;
            this.throwsKeyword = throwsKeyword;
            this.spaceBeforeExceptionName = spaceBeforeExceptionName;
            this.exceptionName = exceptionName;
            this.description = description;
        }

        public UUID getId() {
            return this.id;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        /**
         * {@code @throws} and {@code @exception} are synonyms.
         */
        public boolean isThrowsKeyword() {
            return this.throwsKeyword;
        }

        public List<Javadoc> getSpaceBeforeExceptionName() {
            return this.spaceBeforeExceptionName;
        }

        public J getExceptionName() {
            return this.exceptionName;
        }

        public List<Javadoc> getDescription() {
            return this.description;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "Javadoc.Throws(id=" + this.getId() + ", markers=" + this.getMarkers() + ", throwsKeyword=" + this.isThrowsKeyword() + ", spaceBeforeExceptionName=" + this.getSpaceBeforeExceptionName() + ", exceptionName=" + this.getExceptionName() + ", description=" + this.getDescription() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof Javadoc.Throws)) return false;
            final Javadoc.Throws other = (Javadoc.Throws) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Throws withId(final UUID id) {
            return this.id == id ? this : new Javadoc.Throws(id, this.markers, this.throwsKeyword, this.spaceBeforeExceptionName, this.exceptionName, this.description);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Throws withMarkers(final Markers markers) {
            return this.markers == markers ? this : new Javadoc.Throws(this.id, markers, this.throwsKeyword, this.spaceBeforeExceptionName, this.exceptionName, this.description);
        }

        /**
         * {@code @throws} and {@code @exception} are synonyms.
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Throws withThrowsKeyword(final boolean throwsKeyword) {
            return this.throwsKeyword == throwsKeyword ? this : new Javadoc.Throws(this.id, this.markers, throwsKeyword, this.spaceBeforeExceptionName, this.exceptionName, this.description);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Throws withSpaceBeforeExceptionName(final List<Javadoc> spaceBeforeExceptionName) {
            return this.spaceBeforeExceptionName == spaceBeforeExceptionName ? this : new Javadoc.Throws(this.id, this.markers, this.throwsKeyword, spaceBeforeExceptionName, this.exceptionName, this.description);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Throws withExceptionName(final J exceptionName) {
            return this.exceptionName == exceptionName ? this : new Javadoc.Throws(this.id, this.markers, this.throwsKeyword, this.spaceBeforeExceptionName, exceptionName, this.description);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Throws withDescription(final List<Javadoc> description) {
            return this.description == description ? this : new Javadoc.Throws(this.id, this.markers, this.throwsKeyword, this.spaceBeforeExceptionName, this.exceptionName, description);
        }
    }


    final class UnknownBlock implements Javadoc {
        private final UUID id;
        private final Markers markers;
        private final String name;
        private final List<Javadoc> content;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitUnknownBlock(this, p);
        }

        public UnknownBlock(final UUID id, final Markers markers, final String name, final List<Javadoc> content) {
            this.id = id;
            this.markers = markers;
            this.name = name;
            this.content = content;
        }

        public UUID getId() {
            return this.id;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public String getName() {
            return this.name;
        }

        public List<Javadoc> getContent() {
            return this.content;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "Javadoc.UnknownBlock(id=" + this.getId() + ", markers=" + this.getMarkers() + ", name=" + this.getName() + ", content=" + this.getContent() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof Javadoc.UnknownBlock)) return false;
            final Javadoc.UnknownBlock other = (Javadoc.UnknownBlock) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.UnknownBlock withId(final UUID id) {
            return this.id == id ? this : new Javadoc.UnknownBlock(id, this.markers, this.name, this.content);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.UnknownBlock withMarkers(final Markers markers) {
            return this.markers == markers ? this : new Javadoc.UnknownBlock(this.id, markers, this.name, this.content);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.UnknownBlock withName(final String name) {
            return this.name == name ? this : new Javadoc.UnknownBlock(this.id, this.markers, name, this.content);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.UnknownBlock withContent(final List<Javadoc> content) {
            return this.content == content ? this : new Javadoc.UnknownBlock(this.id, this.markers, this.name, content);
        }
    }


    final class UnknownInline implements Javadoc {
        private final UUID id;
        private final Markers markers;
        private final String name;
        private final List<Javadoc> content;
        private final List<Javadoc> endBrace;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitUnknownInline(this, p);
        }

        public UnknownInline(final UUID id, final Markers markers, final String name, final List<Javadoc> content, final List<Javadoc> endBrace) {
            this.id = id;
            this.markers = markers;
            this.name = name;
            this.content = content;
            this.endBrace = endBrace;
        }

        public UUID getId() {
            return this.id;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public String getName() {
            return this.name;
        }

        public List<Javadoc> getContent() {
            return this.content;
        }

        public List<Javadoc> getEndBrace() {
            return this.endBrace;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "Javadoc.UnknownInline(id=" + this.getId() + ", markers=" + this.getMarkers() + ", name=" + this.getName() + ", content=" + this.getContent() + ", endBrace=" + this.getEndBrace() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof Javadoc.UnknownInline)) return false;
            final Javadoc.UnknownInline other = (Javadoc.UnknownInline) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.UnknownInline withId(final UUID id) {
            return this.id == id ? this : new Javadoc.UnknownInline(id, this.markers, this.name, this.content, this.endBrace);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.UnknownInline withMarkers(final Markers markers) {
            return this.markers == markers ? this : new Javadoc.UnknownInline(this.id, markers, this.name, this.content, this.endBrace);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.UnknownInline withName(final String name) {
            return this.name == name ? this : new Javadoc.UnknownInline(this.id, this.markers, name, this.content, this.endBrace);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.UnknownInline withContent(final List<Javadoc> content) {
            return this.content == content ? this : new Javadoc.UnknownInline(this.id, this.markers, this.name, content, this.endBrace);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.UnknownInline withEndBrace(final List<Javadoc> endBrace) {
            return this.endBrace == endBrace ? this : new Javadoc.UnknownInline(this.id, this.markers, this.name, this.content, endBrace);
        }
    }


    final class Uses implements Javadoc {
        private final UUID id;
        private final Markers markers;
        private final List<Javadoc> beforeServiceType;
        private final J serviceType;
        private final List<Javadoc> description;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitUses(this, p);
        }

        public Uses(final UUID id, final Markers markers, final List<Javadoc> beforeServiceType, final J serviceType, final List<Javadoc> description) {
            this.id = id;
            this.markers = markers;
            this.beforeServiceType = beforeServiceType;
            this.serviceType = serviceType;
            this.description = description;
        }

        public UUID getId() {
            return this.id;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public List<Javadoc> getBeforeServiceType() {
            return this.beforeServiceType;
        }

        public J getServiceType() {
            return this.serviceType;
        }

        public List<Javadoc> getDescription() {
            return this.description;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "Javadoc.Uses(id=" + this.getId() + ", markers=" + this.getMarkers() + ", beforeServiceType=" + this.getBeforeServiceType() + ", serviceType=" + this.getServiceType() + ", description=" + this.getDescription() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof Javadoc.Uses)) return false;
            final Javadoc.Uses other = (Javadoc.Uses) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Uses withId(final UUID id) {
            return this.id == id ? this : new Javadoc.Uses(id, this.markers, this.beforeServiceType, this.serviceType, this.description);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Uses withMarkers(final Markers markers) {
            return this.markers == markers ? this : new Javadoc.Uses(this.id, markers, this.beforeServiceType, this.serviceType, this.description);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Uses withBeforeServiceType(final List<Javadoc> beforeServiceType) {
            return this.beforeServiceType == beforeServiceType ? this : new Javadoc.Uses(this.id, this.markers, beforeServiceType, this.serviceType, this.description);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Uses withServiceType(final J serviceType) {
            return this.serviceType == serviceType ? this : new Javadoc.Uses(this.id, this.markers, this.beforeServiceType, serviceType, this.description);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Uses withDescription(final List<Javadoc> description) {
            return this.description == description ? this : new Javadoc.Uses(this.id, this.markers, this.beforeServiceType, this.serviceType, description);
        }
    }


    final class Version implements Javadoc {
        private final UUID id;
        private final Markers markers;
        private final List<Javadoc> body;

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitVersion(this, p);
        }

        public Version(final UUID id, final Markers markers, final List<Javadoc> body) {
            this.id = id;
            this.markers = markers;
            this.body = body;
        }

        public UUID getId() {
            return this.id;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public List<Javadoc> getBody() {
            return this.body;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "Javadoc.Version(id=" + this.getId() + ", markers=" + this.getMarkers() + ", body=" + this.getBody() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof Javadoc.Version)) return false;
            final Javadoc.Version other = (Javadoc.Version) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Version withId(final UUID id) {
            return this.id == id ? this : new Javadoc.Version(id, this.markers, this.body);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Version withMarkers(final Markers markers) {
            return this.markers == markers ? this : new Javadoc.Version(this.id, markers, this.body);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Version withBody(final List<Javadoc> body) {
            return this.body == body ? this : new Javadoc.Version(this.id, this.markers, body);
        }
    }


    final class Reference implements Javadoc {
        private final UUID id;
        /**
         * Not truly nullable, but done for deserialization backwards compatibility since this
         * field was accidentally omitted prior to 7.31.0.
         */
        @Nullable
        private final Markers markers;

        public Markers getMarkers() {
            return markers == null ? Markers.EMPTY : markers;
        }

        @Nullable
        private final J tree;
        @Nullable
        private final List<Javadoc> lineBreaks;

        public List<Javadoc> getLineBreaks() {
            return lineBreaks == null ? Collections.emptyList() : lineBreaks;
        }

        @Override
        public <P> Javadoc acceptJavadoc(JavadocVisitor<P> v, P p) {
            return v.visitReference(this, p);
        }

        public Reference(final UUID id, @Nullable final Markers markers, @Nullable final J tree, @Nullable final List<Javadoc> lineBreaks) {
            this.id = id;
            this.markers = markers;
            this.tree = tree;
            this.lineBreaks = lineBreaks;
        }

        public UUID getId() {
            return this.id;
        }

        @Nullable
        public J getTree() {
            return this.tree;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "Javadoc.Reference(id=" + this.getId() + ", markers=" + this.getMarkers() + ", tree=" + this.getTree() + ", lineBreaks=" + this.getLineBreaks() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof Javadoc.Reference)) return false;
            final Javadoc.Reference other = (Javadoc.Reference) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Reference withId(final UUID id) {
            return this.id == id ? this : new Javadoc.Reference(id, this.markers, this.tree, this.lineBreaks);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Reference withMarkers(@Nullable final Markers markers) {
            return this.markers == markers ? this : new Javadoc.Reference(this.id, markers, this.tree, this.lineBreaks);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Reference withTree(@Nullable final J tree) {
            return this.tree == tree ? this : new Javadoc.Reference(this.id, this.markers, tree, this.lineBreaks);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Javadoc.Reference withLineBreaks(@Nullable final List<Javadoc> lineBreaks) {
            return this.lineBreaks == lineBreaks ? this : new Javadoc.Reference(this.id, this.markers, this.tree, lineBreaks);
        }
    }
}
