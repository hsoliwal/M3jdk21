// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/xml/tree/Xml.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.xml.tree;

import org.apache.commons.text.StringEscapeUtils;
import org.intellij.lang.annotations.Language;
import org.openrewrite.*;
import org.openrewrite.internal.WhitespaceValidationService;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;
import org.openrewrite.xml.XmlParser;
import org.openrewrite.xml.XmlVisitor;
import org.openrewrite.xml.internal.WithPrefix;
import org.openrewrite.xml.internal.XmlPrinter;
import org.openrewrite.xml.internal.XmlWhitespaceValidationService;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.Tree.randomId;

/**
 * The XML <a href="https://www.w3.org/TR/xml11/#syntax">spec</a>.
 */
public interface Xml extends Tree {
    @SuppressWarnings("unchecked")
    @Override
    default <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
        return (R) acceptXml(v.adapt(XmlVisitor.class), p);
    }

    @Nullable
    default <P> Xml acceptXml(XmlVisitor<P> v, P p) {
        return v.defaultValue(this, p);
    }

    @Override
    default <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return v.isAdaptableTo(XmlVisitor.class);
    }

    String getPrefix();

    Xml withPrefix(String prefix);

    /**
     * @param prefix The new prefix
     * @return An XML AST with the new prefix set, even if the old and new prefix pass a
     * string equality check. The receiver is unchanged if the old and new prefix pass a
     * referential equality check.
     */
    Xml withPrefixUnsafe(String prefix);


    class Document implements Xml, SourceFile {
        private final UUID id;
        private final Path sourcePath;
        private final String prefixUnsafe;

        public Document withPrefix(String prefix) {
            return WithPrefix.onlyIfNotEqual(this, prefix);
        }

        public String getPrefix() {
            return prefixUnsafe;
        }

        private final Markers markers;
        // for backwards compatibility
        @Nullable
        private final String charsetName;
        private final boolean charsetBomMarked;
        @Nullable
        private final Checksum checksum;
        @Nullable
        private final FileAttributes fileAttributes;

        @Override
        public Charset getCharset() {
            return charsetName == null ? StandardCharsets.UTF_8 : Charset.forName(charsetName);
        }

        @Override
        public SourceFile withCharset(Charset charset) {
            return withCharsetName(charset.name());
        }

        private final Prolog prolog;
        private final Tag root;
        private final String eof;

        public Document withEof(String eof) {
            if (this.eof.equals(eof)) {
                return this;
            }
            return new Document(id, sourcePath, prefixUnsafe, markers, charsetName, charsetBomMarked, checksum, fileAttributes, prolog, root, eof);
        }

        @Override
        public <P> Xml acceptXml(XmlVisitor<P> v, P p) {
            return v.visitDocument(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new XmlPrinter<>();
        }

        @SuppressWarnings("unchecked")
        @Override
        public <S, T extends S> T service(Class<S> service) {
            if (WhitespaceValidationService.class.getName().equals(service.getName())) {
                return (T) new XmlWhitespaceValidationService();
            }
            return SourceFile.super.service(service);
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof Xml.Document)) return false;
            final Xml.Document other = (Xml.Document) o;
            if (!other.canEqual((Object) this)) return false;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
            return other instanceof Xml.Document;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public Document(final UUID id, final Path sourcePath, final String prefixUnsafe, final Markers markers, @Nullable final String charsetName, final boolean charsetBomMarked, @Nullable final Checksum checksum, @Nullable final FileAttributes fileAttributes, final Prolog prolog, final Tag root, final String eof) {
            this.id = id;
            this.sourcePath = sourcePath;
            this.prefixUnsafe = prefixUnsafe;
            this.markers = markers;
            this.charsetName = charsetName;
            this.charsetBomMarked = charsetBomMarked;
            this.checksum = checksum;
            this.fileAttributes = fileAttributes;
            this.prolog = prolog;
            this.root = root;
            this.eof = eof;
        }

        public UUID getId() {
            return this.id;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Xml.Document withId(final UUID id) {
            return this.id == id ? this : new Xml.Document(id, this.sourcePath, this.prefixUnsafe, this.markers, this.charsetName, this.charsetBomMarked, this.checksum, this.fileAttributes, this.prolog, this.root, this.eof);
        }

        public Path getSourcePath() {
            return this.sourcePath;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Xml.Document withSourcePath(final Path sourcePath) {
            return this.sourcePath == sourcePath ? this : new Xml.Document(this.id, sourcePath, this.prefixUnsafe, this.markers, this.charsetName, this.charsetBomMarked, this.checksum, this.fileAttributes, this.prolog, this.root, this.eof);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Xml.Document withPrefixUnsafe(final String prefixUnsafe) {
            return this.prefixUnsafe == prefixUnsafe ? this : new Xml.Document(this.id, this.sourcePath, prefixUnsafe, this.markers, this.charsetName, this.charsetBomMarked, this.checksum, this.fileAttributes, this.prolog, this.root, this.eof);
        }

        public Markers getMarkers() {
            return this.markers;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Xml.Document withMarkers(final Markers markers) {
            return this.markers == markers ? this : new Xml.Document(this.id, this.sourcePath, this.prefixUnsafe, markers, this.charsetName, this.charsetBomMarked, this.checksum, this.fileAttributes, this.prolog, this.root, this.eof);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        private Xml.Document withCharsetName(@Nullable final String charsetName) {
            return this.charsetName == charsetName ? this : new Xml.Document(this.id, this.sourcePath, this.prefixUnsafe, this.markers, charsetName, this.charsetBomMarked, this.checksum, this.fileAttributes, this.prolog, this.root, this.eof);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Xml.Document withCharsetBomMarked(final boolean charsetBomMarked) {
            return this.charsetBomMarked == charsetBomMarked ? this : new Xml.Document(this.id, this.sourcePath, this.prefixUnsafe, this.markers, this.charsetName, charsetBomMarked, this.checksum, this.fileAttributes, this.prolog, this.root, this.eof);
        }

        public boolean isCharsetBomMarked() {
            return this.charsetBomMarked;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Xml.Document withChecksum(@Nullable final Checksum checksum) {
            return this.checksum == checksum ? this : new Xml.Document(this.id, this.sourcePath, this.prefixUnsafe, this.markers, this.charsetName, this.charsetBomMarked, checksum, this.fileAttributes, this.prolog, this.root, this.eof);
        }

        @Nullable
        public Checksum getChecksum() {
            return this.checksum;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Xml.Document withFileAttributes(@Nullable final FileAttributes fileAttributes) {
            return this.fileAttributes == fileAttributes ? this : new Xml.Document(this.id, this.sourcePath, this.prefixUnsafe, this.markers, this.charsetName, this.charsetBomMarked, this.checksum, fileAttributes, this.prolog, this.root, this.eof);
        }

        @Nullable
        public FileAttributes getFileAttributes() {
            return this.fileAttributes;
        }

        public Prolog getProlog() {
            return this.prolog;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Xml.Document withProlog(final Prolog prolog) {
            return this.prolog == prolog ? this : new Xml.Document(this.id, this.sourcePath, this.prefixUnsafe, this.markers, this.charsetName, this.charsetBomMarked, this.checksum, this.fileAttributes, prolog, this.root, this.eof);
        }

        public Tag getRoot() {
            return this.root;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Xml.Document withRoot(final Tag root) {
            return this.root == root ? this : new Xml.Document(this.id, this.sourcePath, this.prefixUnsafe, this.markers, this.charsetName, this.charsetBomMarked, this.checksum, this.fileAttributes, this.prolog, root, this.eof);
        }

        public String getEof() {
            return this.eof;
        }
    }


    final class Prolog implements Xml {
        private final UUID id;
        private final String prefixUnsafe;

        public Prolog withPrefix(String prefix) {
            return WithPrefix.onlyIfNotEqual(this, prefix);
        }

        public String getPrefix() {
            return prefixUnsafe;
        }

        private final Markers markers;
        @Nullable
        private final XmlDecl xmlDecl;
        private final List<Misc> misc;

        @Override
        public <P> Xml acceptXml(XmlVisitor<P> v, P p) {
            return v.visitProlog(this, p);
        }

        public Prolog(final UUID id, final String prefixUnsafe, final Markers markers, @Nullable final XmlDecl xmlDecl, final List<Misc> misc) {
            this.id = id;
            this.prefixUnsafe = prefixUnsafe;
            this.markers = markers;
            this.xmlDecl = xmlDecl;
            this.misc = misc;
        }

        public UUID getId() {
            return this.id;
        }

        public String getPrefixUnsafe() {
            return this.prefixUnsafe;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        @Nullable
        public XmlDecl getXmlDecl() {
            return this.xmlDecl;
        }

        public List<Misc> getMisc() {
            return this.misc;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "Xml.Prolog(id=" + this.getId() + ", prefixUnsafe=" + this.getPrefixUnsafe() + ", markers=" + this.getMarkers() + ", xmlDecl=" + this.getXmlDecl() + ", misc=" + this.getMisc() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof Xml.Prolog)) return false;
            final Xml.Prolog other = (Xml.Prolog) o;
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
        public Xml.Prolog withId(final UUID id) {
            return this.id == id ? this : new Xml.Prolog(id, this.prefixUnsafe, this.markers, this.xmlDecl, this.misc);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Xml.Prolog withPrefixUnsafe(final String prefixUnsafe) {
            return this.prefixUnsafe == prefixUnsafe ? this : new Xml.Prolog(this.id, prefixUnsafe, this.markers, this.xmlDecl, this.misc);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Xml.Prolog withMarkers(final Markers markers) {
            return this.markers == markers ? this : new Xml.Prolog(this.id, this.prefixUnsafe, markers, this.xmlDecl, this.misc);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Xml.Prolog withXmlDecl(@Nullable final XmlDecl xmlDecl) {
            return this.xmlDecl == xmlDecl ? this : new Xml.Prolog(this.id, this.prefixUnsafe, this.markers, xmlDecl, this.misc);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Xml.Prolog withMisc(final List<Misc> misc) {
            return this.misc == misc ? this : new Xml.Prolog(this.id, this.prefixUnsafe, this.markers, this.xmlDecl, misc);
        }
    }


    final class XmlDecl implements Xml, Misc {
        private final UUID id;
        private final String prefixUnsafe;

        public XmlDecl withPrefix(String prefix) {
            return WithPrefix.onlyIfNotEqual(this, prefix);
        }

        public String getPrefix() {
            return prefixUnsafe;
        }

        private final Markers markers;
        private final String name;
        private final List<Attribute> attributes;
        /**
         * Space before '&gt;'
         */
        private final String beforeTagDelimiterPrefix;

        @Override
        public <P> Xml acceptXml(XmlVisitor<P> v, P p) {
            return v.visitXmlDecl(this, p);
        }

        public XmlDecl(final UUID id, final String prefixUnsafe, final Markers markers, final String name, final List<Attribute> attributes, final String beforeTagDelimiterPrefix) {
            this.id = id;
            this.prefixUnsafe = prefixUnsafe;
            this.markers = markers;
            this.name = name;
            this.attributes = attributes;
            this.beforeTagDelimiterPrefix = beforeTagDelimiterPrefix;
        }

        public UUID getId() {
            return this.id;
        }

        public String getPrefixUnsafe() {
            return this.prefixUnsafe;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public String getName() {
            return this.name;
        }

        public List<Attribute> getAttributes() {
            return this.attributes;
        }

        /**
         * Space before '&gt;'
         */
        public String getBeforeTagDelimiterPrefix() {
            return this.beforeTagDelimiterPrefix;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "Xml.XmlDecl(id=" + this.getId() + ", prefixUnsafe=" + this.getPrefixUnsafe() + ", markers=" + this.getMarkers() + ", name=" + this.getName() + ", attributes=" + this.getAttributes() + ", beforeTagDelimiterPrefix=" + this.getBeforeTagDelimiterPrefix() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof Xml.XmlDecl)) return false;
            final Xml.XmlDecl other = (Xml.XmlDecl) o;
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
        public Xml.XmlDecl withId(final UUID id) {
            return this.id == id ? this : new Xml.XmlDecl(id, this.prefixUnsafe, this.markers, this.name, this.attributes, this.beforeTagDelimiterPrefix);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Xml.XmlDecl withPrefixUnsafe(final String prefixUnsafe) {
            return this.prefixUnsafe == prefixUnsafe ? this : new Xml.XmlDecl(this.id, prefixUnsafe, this.markers, this.name, this.attributes, this.beforeTagDelimiterPrefix);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Xml.XmlDecl withMarkers(final Markers markers) {
            return this.markers == markers ? this : new Xml.XmlDecl(this.id, this.prefixUnsafe, markers, this.name, this.attributes, this.beforeTagDelimiterPrefix);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Xml.XmlDecl withName(final String name) {
            return this.name == name ? this : new Xml.XmlDecl(this.id, this.prefixUnsafe, this.markers, name, this.attributes, this.beforeTagDelimiterPrefix);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Xml.XmlDecl withAttributes(final List<Attribute> attributes) {
            return this.attributes == attributes ? this : new Xml.XmlDecl(this.id, this.prefixUnsafe, this.markers, this.name, attributes, this.beforeTagDelimiterPrefix);
        }

        /**
         * Space before '&gt;'
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Xml.XmlDecl withBeforeTagDelimiterPrefix(final String beforeTagDelimiterPrefix) {
            return this.beforeTagDelimiterPrefix == beforeTagDelimiterPrefix ? this : new Xml.XmlDecl(this.id, this.prefixUnsafe, this.markers, this.name, this.attributes, beforeTagDelimiterPrefix);
        }
    }


    final class ProcessingInstruction implements Xml, Content, Misc {
        private final UUID id;
        private final String prefixUnsafe;

        public ProcessingInstruction withPrefix(String prefix) {
            return WithPrefix.onlyIfNotEqual(this, prefix);
        }

        public String getPrefix() {
            return prefixUnsafe;
        }

        private final Markers markers;
        private final String name;
        private final CharData processingInstructions;
        /**
         * Space before '&gt;'
         */
        private final String beforeTagDelimiterPrefix;

        @Override
        public <P> Xml acceptXml(XmlVisitor<P> v, P p) {
            return v.visitProcessingInstruction(this, p);
        }

        public ProcessingInstruction(final UUID id, final String prefixUnsafe, final Markers markers, final String name, final CharData processingInstructions, final String beforeTagDelimiterPrefix) {
            this.id = id;
            this.prefixUnsafe = prefixUnsafe;
            this.markers = markers;
            this.name = name;
            this.processingInstructions = processingInstructions;
            this.beforeTagDelimiterPrefix = beforeTagDelimiterPrefix;
        }

        public UUID getId() {
            return this.id;
        }

        public String getPrefixUnsafe() {
            return this.prefixUnsafe;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public String getName() {
            return this.name;
        }

        public CharData getProcessingInstructions() {
            return this.processingInstructions;
        }

        /**
         * Space before '&gt;'
         */
        public String getBeforeTagDelimiterPrefix() {
            return this.beforeTagDelimiterPrefix;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "Xml.ProcessingInstruction(id=" + this.getId() + ", prefixUnsafe=" + this.getPrefixUnsafe() + ", markers=" + this.getMarkers() + ", name=" + this.getName() + ", processingInstructions=" + this.getProcessingInstructions() + ", beforeTagDelimiterPrefix=" + this.getBeforeTagDelimiterPrefix() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof Xml.ProcessingInstruction)) return false;
            final Xml.ProcessingInstruction other = (Xml.ProcessingInstruction) o;
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
        public Xml.ProcessingInstruction withId(final UUID id) {
            return this.id == id ? this : new Xml.ProcessingInstruction(id, this.prefixUnsafe, this.markers, this.name, this.processingInstructions, this.beforeTagDelimiterPrefix);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Xml.ProcessingInstruction withPrefixUnsafe(final String prefixUnsafe) {
            return this.prefixUnsafe == prefixUnsafe ? this : new Xml.ProcessingInstruction(this.id, prefixUnsafe, this.markers, this.name, this.processingInstructions, this.beforeTagDelimiterPrefix);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Xml.ProcessingInstruction withMarkers(final Markers markers) {
            return this.markers == markers ? this : new Xml.ProcessingInstruction(this.id, this.prefixUnsafe, markers, this.name, this.processingInstructions, this.beforeTagDelimiterPrefix);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Xml.ProcessingInstruction withName(final String name) {
            return this.name == name ? this : new Xml.ProcessingInstruction(this.id, this.prefixUnsafe, this.markers, name, this.processingInstructions, this.beforeTagDelimiterPrefix);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Xml.ProcessingInstruction withProcessingInstructions(final CharData processingInstructions) {
            return this.processingInstructions == processingInstructions ? this : new Xml.ProcessingInstruction(this.id, this.prefixUnsafe, this.markers, this.name, processingInstructions, this.beforeTagDelimiterPrefix);
        }

        /**
         * Space before '&gt;'
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Xml.ProcessingInstruction withBeforeTagDelimiterPrefix(final String beforeTagDelimiterPrefix) {
            return this.beforeTagDelimiterPrefix == beforeTagDelimiterPrefix ? this : new Xml.ProcessingInstruction(this.id, this.prefixUnsafe, this.markers, this.name, this.processingInstructions, beforeTagDelimiterPrefix);
        }
    }


    final class Tag implements Xml, Content {
        private final UUID id;
        private final String prefixUnsafe;

        public Tag withPrefix(String prefix) {
            return WithPrefix.onlyIfNotEqual(this, prefix);
        }

        public String getPrefix() {
            return prefixUnsafe;
        }

        private final Markers markers;
        /**
         * XML does not allow space between the '&lt;' and tag name.
         */
        private final String name;

        public static Xml.Tag build(@Language("xml") String tagSource) {
            return new XmlParser().parse(tagSource).findFirst().map(Xml.Document.class::cast).orElseThrow(() -> new IllegalArgumentException("Could not parse as XML")).getRoot();
        }

        public Tag withName(String name) {
            return new Tag(id, prefixUnsafe, markers, name, attributes, content, closing == null ? null : closing.withName(name), beforeTagDelimiterPrefix);
        }

        public Tag withValue(String value) {
            CharData charData;
            if (content != null && content.get(0) instanceof CharData) {
                charData = ((CharData) content.get(0)).withText(value);
            } else {
                charData = new CharData(randomId(), "", Markers.EMPTY, false, value, "");
            }
            return withContent(Collections.singletonList(charData));
        }

        private final List<Attribute> attributes;
        @Nullable
        private final List<? extends Content> content;

        public Optional<Tag> getChild(String name) {
            return content == null ? Optional.empty() : content.stream().filter(t -> t instanceof Xml.Tag).map(Tag.class::cast).filter(t -> t.getName().equals(name)).findAny();
        }

        public List<Tag> getChildren(String name) {
            return content == null ? emptyList() : content.stream().filter(t -> t instanceof Xml.Tag).map(Tag.class::cast).filter(t -> t.getName().equals(name)).collect(toList());
        }

        public List<Tag> getChildren() {
            return content == null ? emptyList() : content.stream().filter(t -> t instanceof Xml.Tag).map(Tag.class::cast).collect(toList());
        }

        /**
         * Locate a child tag with the given name and set its text value.
         *
         * @param childName The child tag to locate. This assumes there is one and only one.
         * @param text      The text value to set.
         * @return This tag.
         */
        public Xml.Tag withChildValue(String childName, String text) {
            return getChild(childName).map(tag -> this.withContent(content == null ? null : content.stream().map(content -> content == tag ? ((Tag) content).withValue(text) : content).collect(toList()))).orElse(this);
        }

        /**
         * @return If this tag's content is only character data, consider it the value.
         */
        public Optional<String> getValue() {
            if (content == null) {
                return Optional.empty();
            }
            if (content.size() == 1 && content.get(0) instanceof Xml.CharData) {
                return Optional.ofNullable(((CharData) content.get(0)).getText());
            }
            if (content.stream().allMatch(c -> c instanceof Xml.CharData)) {
                return Optional.of(content.stream().map(c -> ((CharData) c).getText()).map(StringEscapeUtils::unescapeXml).collect(Collectors.joining()));
            }
            return Optional.empty();
        }

        /**
         * A shortcut for {@link #getChild(String)} and {@link #getValue()}.
         *
         * @param name The name of the child element to look for.
         * @return The character data of the first child element matching the provided name, if any.
         */
        public Optional<String> getChildValue(String name) {
            return getChild(name).flatMap(Tag::getValue);
        }

        public Optional<Tag> getSibling(String name, Cursor cursor) {
            if (cursor.getParent() == null) {
                return Optional.empty();
            }
            Xml.Tag parent = cursor.getParent().getValue();
            return parent.getChild(name);
        }

        public Tag withContent(@Nullable List<? extends Content> content) {
            if (this.content == content) {
                return this;
            }
            Tag tag = new Tag(id, prefixUnsafe, markers, name, attributes, content, closing, beforeTagDelimiterPrefix);
            if (closing == null) {
                if (content != null && !content.isEmpty()) {
                    // TODO test this
                    String indentedClosingTagPrefix = prefixUnsafe.substring(Math.max(0, prefixUnsafe.lastIndexOf('\n')));
                    if (content.get(0) instanceof CharData) {
                        return tag.withClosing(new Closing(randomId(), content.get(0).getPrefix().contains("\n") ? indentedClosingTagPrefix : "", Markers.EMPTY, name, ""));
                    } else {
                        return tag.withClosing(new Closing(randomId(), indentedClosingTagPrefix, Markers.EMPTY, name, ""));
                    }
                }
            }
            return tag;
        }

        @Nullable
        private final Closing closing;
        /**
         * Space before '&gt;' or '/&gt;'
         */
        private final String beforeTagDelimiterPrefix;

        @Override
        public <P> Xml acceptXml(XmlVisitor<P> v, P p) {
            return v.visitTag(this, p);
        }

        @Override
        public String toString() {
            return "<" + name + attributes.stream().map(a -> " " + a.getKey().getName() + "=\"" + a.getValueAsString() + "\"").collect(Collectors.joining("")) + ">";
        }


        public static final class Closing implements Xml {
            private final UUID id;
            private final String prefixUnsafe;

            public Closing withPrefix(String prefix) {
                return WithPrefix.onlyIfNotEqual(this, prefix);
            }

            public String getPrefix() {
                return prefixUnsafe;
            }

            private final Markers markers;
            private final String name;
            private final String beforeTagDelimiterPrefix;

            @Override
            public String toString() {
                return "</" + name + ">";
            }

            public Closing(final UUID id, final String prefixUnsafe, final Markers markers, final String name, final String beforeTagDelimiterPrefix) {
                this.id = id;
                this.prefixUnsafe = prefixUnsafe;
                this.markers = markers;
                this.name = name;
                this.beforeTagDelimiterPrefix = beforeTagDelimiterPrefix;
            }

            public UUID getId() {
                return this.id;
            }

            public String getPrefixUnsafe() {
                return this.prefixUnsafe;
            }

            public Markers getMarkers() {
                return this.markers;
            }

            public String getName() {
                return this.name;
            }

            public String getBeforeTagDelimiterPrefix() {
                return this.beforeTagDelimiterPrefix;
            }

            @Override
            public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
                if (o == this) return true;
                if (!(o instanceof Xml.Tag.Closing)) return false;
                final Xml.Tag.Closing other = (Xml.Tag.Closing) o;
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
            public Xml.Tag.Closing withId(final UUID id) {
                return this.id == id ? this : new Xml.Tag.Closing(id, this.prefixUnsafe, this.markers, this.name, this.beforeTagDelimiterPrefix);
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public Xml.Tag.Closing withPrefixUnsafe(final String prefixUnsafe) {
                return this.prefixUnsafe == prefixUnsafe ? this : new Xml.Tag.Closing(this.id, prefixUnsafe, this.markers, this.name, this.beforeTagDelimiterPrefix);
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public Xml.Tag.Closing withMarkers(final Markers markers) {
                return this.markers == markers ? this : new Xml.Tag.Closing(this.id, this.prefixUnsafe, markers, this.name, this.beforeTagDelimiterPrefix);
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public Xml.Tag.Closing withName(final String name) {
                return this.name == name ? this : new Xml.Tag.Closing(this.id, this.prefixUnsafe, this.markers, name, this.beforeTagDelimiterPrefix);
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public Xml.Tag.Closing withBeforeTagDelimiterPrefix(final String beforeTagDelimiterPrefix) {
                return this.beforeTagDelimiterPrefix == beforeTagDelimiterPrefix ? this : new Xml.Tag.Closing(this.id, this.prefixUnsafe, this.markers, this.name, beforeTagDelimiterPrefix);
            }
        }

        public Tag(final UUID id, final String prefixUnsafe, final Markers markers, final String name, final List<Attribute> attributes, @Nullable final List<? extends Content> content, @Nullable final Closing closing, final String beforeTagDelimiterPrefix) {
            this.id = id;
            this.prefixUnsafe = prefixUnsafe;
            this.markers = markers;
            this.name = name;
            this.attributes = attributes;
            this.content = content;
            this.closing = closing;
            this.beforeTagDelimiterPrefix = beforeTagDelimiterPrefix;
        }

        public UUID getId() {
            return this.id;
        }

        public String getPrefixUnsafe() {
            return this.prefixUnsafe;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        /**
         * XML does not allow space between the '&lt;' and tag name.
         */
        public String getName() {
            return this.name;
        }

        public List<Attribute> getAttributes() {
            return this.attributes;
        }

        @Nullable
        public List<? extends Content> getContent() {
            return this.content;
        }

        @Nullable
        public Closing getClosing() {
            return this.closing;
        }

        /**
         * Space before '&gt;' or '/&gt;'
         */
        public String getBeforeTagDelimiterPrefix() {
            return this.beforeTagDelimiterPrefix;
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof Xml.Tag)) return false;
            final Xml.Tag other = (Xml.Tag) o;
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
        public Xml.Tag withId(final UUID id) {
            return this.id == id ? this : new Xml.Tag(id, this.prefixUnsafe, this.markers, this.name, this.attributes, this.content, this.closing, this.beforeTagDelimiterPrefix);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Xml.Tag withPrefixUnsafe(final String prefixUnsafe) {
            return this.prefixUnsafe == prefixUnsafe ? this : new Xml.Tag(this.id, prefixUnsafe, this.markers, this.name, this.attributes, this.content, this.closing, this.beforeTagDelimiterPrefix);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Xml.Tag withMarkers(final Markers markers) {
            return this.markers == markers ? this : new Xml.Tag(this.id, this.prefixUnsafe, markers, this.name, this.attributes, this.content, this.closing, this.beforeTagDelimiterPrefix);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Xml.Tag withAttributes(final List<Attribute> attributes) {
            return this.attributes == attributes ? this : new Xml.Tag(this.id, this.prefixUnsafe, this.markers, this.name, attributes, this.content, this.closing, this.beforeTagDelimiterPrefix);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Xml.Tag withClosing(@Nullable final Closing closing) {
            return this.closing == closing ? this : new Xml.Tag(this.id, this.prefixUnsafe, this.markers, this.name, this.attributes, this.content, closing, this.beforeTagDelimiterPrefix);
        }

        /**
         * Space before '&gt;' or '/&gt;'
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Xml.Tag withBeforeTagDelimiterPrefix(final String beforeTagDelimiterPrefix) {
            return this.beforeTagDelimiterPrefix == beforeTagDelimiterPrefix ? this : new Xml.Tag(this.id, this.prefixUnsafe, this.markers, this.name, this.attributes, this.content, this.closing, beforeTagDelimiterPrefix);
        }
    }


    /**
     * Space before '&gt;'
     */
    final class Attribute implements Xml {
        private final UUID id;
        private final String prefixUnsafe;

        public Attribute withPrefix(String prefix) {
            return WithPrefix.onlyIfNotEqual(this, prefix);
        }

        public String getPrefix() {
            return prefixUnsafe;
        }

        private final Markers markers;
        private final Ident key;
        private final String beforeEquals;
        private final Value value;

        @Override
        public <P> Xml acceptXml(XmlVisitor<P> v, P p) {
            return v.visitAttribute(this, p);
        }


        public static final class Value implements Xml {

            public enum Quote {
                Double, Single;
            }

            private final UUID id;
            private final String prefixUnsafe;

            public Value withPrefix(String prefix) {
                return WithPrefix.onlyIfNotEqual(this, prefix);
            }

            public String getPrefix() {
                return prefixUnsafe;
            }

            private final Markers markers;
            private final Quote quote;
            private final String value;

            public Value(final UUID id, final String prefixUnsafe, final Markers markers, final Quote quote, final String value) {
                this.id = id;
                this.prefixUnsafe = prefixUnsafe;
                this.markers = markers;
                this.quote = quote;
                this.value = value;
            }

            public UUID getId() {
                return this.id;
            }

            public String getPrefixUnsafe() {
                return this.prefixUnsafe;
            }

            public Markers getMarkers() {
                return this.markers;
            }

            public Quote getQuote() {
                return this.quote;
            }

            public String getValue() {
                return this.value;
            }

            @org.openrewrite.internal.lang.NonNull
            @Override
            public String toString() {
                return "Xml.Attribute.Value(id=" + this.getId() + ", prefixUnsafe=" + this.getPrefixUnsafe() + ", markers=" + this.getMarkers() + ", quote=" + this.getQuote() + ", value=" + this.getValue() + ")";
            }

            @Override
            public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
                if (o == this) return true;
                if (!(o instanceof Xml.Attribute.Value)) return false;
                final Xml.Attribute.Value other = (Xml.Attribute.Value) o;
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
            public Xml.Attribute.Value withId(final UUID id) {
                return this.id == id ? this : new Xml.Attribute.Value(id, this.prefixUnsafe, this.markers, this.quote, this.value);
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public Xml.Attribute.Value withPrefixUnsafe(final String prefixUnsafe) {
                return this.prefixUnsafe == prefixUnsafe ? this : new Xml.Attribute.Value(this.id, prefixUnsafe, this.markers, this.quote, this.value);
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public Xml.Attribute.Value withMarkers(final Markers markers) {
                return this.markers == markers ? this : new Xml.Attribute.Value(this.id, this.prefixUnsafe, markers, this.quote, this.value);
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public Xml.Attribute.Value withQuote(final Quote quote) {
                return this.quote == quote ? this : new Xml.Attribute.Value(this.id, this.prefixUnsafe, this.markers, quote, this.value);
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public Xml.Attribute.Value withValue(final String value) {
                return this.value == value ? this : new Xml.Attribute.Value(this.id, this.prefixUnsafe, this.markers, this.quote, value);
            }
        }

        public String getKeyAsString() {
            return key.getName();
        }

        public String getValueAsString() {
            return value.getValue();
        }

        @Override
        public String toString() {
            return getKeyAsString() + "=" + getValueAsString();
        }

        public Attribute(final UUID id, final String prefixUnsafe, final Markers markers, final Ident key, final String beforeEquals, final Value value) {
            this.id = id;
            this.prefixUnsafe = prefixUnsafe;
            this.markers = markers;
            this.key = key;
            this.beforeEquals = beforeEquals;
            this.value = value;
        }

        public UUID getId() {
            return this.id;
        }

        public String getPrefixUnsafe() {
            return this.prefixUnsafe;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public Ident getKey() {
            return this.key;
        }

        public String getBeforeEquals() {
            return this.beforeEquals;
        }

        public Value getValue() {
            return this.value;
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof Xml.Attribute)) return false;
            final Xml.Attribute other = (Xml.Attribute) o;
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
        public Xml.Attribute withId(final UUID id) {
            return this.id == id ? this : new Xml.Attribute(id, this.prefixUnsafe, this.markers, this.key, this.beforeEquals, this.value);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Xml.Attribute withPrefixUnsafe(final String prefixUnsafe) {
            return this.prefixUnsafe == prefixUnsafe ? this : new Xml.Attribute(this.id, prefixUnsafe, this.markers, this.key, this.beforeEquals, this.value);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Xml.Attribute withMarkers(final Markers markers) {
            return this.markers == markers ? this : new Xml.Attribute(this.id, this.prefixUnsafe, markers, this.key, this.beforeEquals, this.value);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Xml.Attribute withKey(final Ident key) {
            return this.key == key ? this : new Xml.Attribute(this.id, this.prefixUnsafe, this.markers, key, this.beforeEquals, this.value);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Xml.Attribute withBeforeEquals(final String beforeEquals) {
            return this.beforeEquals == beforeEquals ? this : new Xml.Attribute(this.id, this.prefixUnsafe, this.markers, this.key, beforeEquals, this.value);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Xml.Attribute withValue(final Value value) {
            return this.value == value ? this : new Xml.Attribute(this.id, this.prefixUnsafe, this.markers, this.key, this.beforeEquals, value);
        }
    }


    final class CharData implements Xml, Content {
        private final UUID id;
        private final String prefixUnsafe;

        public CharData withPrefix(String prefix) {
            return WithPrefix.onlyIfNotEqual(this, prefix);
        }

        public String getPrefix() {
            return prefixUnsafe;
        }

        private final Markers markers;
        private final boolean cdata;
        private final String text;
        private final String afterText;

        @Override
        public <P> Xml acceptXml(XmlVisitor<P> v, P p) {
            return v.visitCharData(this, p);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("text = \"").append(text).append("\"");
            if (afterText != null && !afterText.isEmpty()) {
                sb.append(" afterText = \"").append(afterText).append("\"");
            }
            return sb.toString();
        }

        public CharData(final UUID id, final String prefixUnsafe, final Markers markers, final boolean cdata, final String text, final String afterText) {
            this.id = id;
            this.prefixUnsafe = prefixUnsafe;
            this.markers = markers;
            this.cdata = cdata;
            this.text = text;
            this.afterText = afterText;
        }

        public UUID getId() {
            return this.id;
        }

        public String getPrefixUnsafe() {
            return this.prefixUnsafe;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public boolean isCdata() {
            return this.cdata;
        }

        public String getText() {
            return this.text;
        }

        public String getAfterText() {
            return this.afterText;
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof Xml.CharData)) return false;
            final Xml.CharData other = (Xml.CharData) o;
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
        public Xml.CharData withId(final UUID id) {
            return this.id == id ? this : new Xml.CharData(id, this.prefixUnsafe, this.markers, this.cdata, this.text, this.afterText);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Xml.CharData withPrefixUnsafe(final String prefixUnsafe) {
            return this.prefixUnsafe == prefixUnsafe ? this : new Xml.CharData(this.id, prefixUnsafe, this.markers, this.cdata, this.text, this.afterText);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Xml.CharData withMarkers(final Markers markers) {
            return this.markers == markers ? this : new Xml.CharData(this.id, this.prefixUnsafe, markers, this.cdata, this.text, this.afterText);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Xml.CharData withCdata(final boolean cdata) {
            return this.cdata == cdata ? this : new Xml.CharData(this.id, this.prefixUnsafe, this.markers, cdata, this.text, this.afterText);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Xml.CharData withText(final String text) {
            return this.text == text ? this : new Xml.CharData(this.id, this.prefixUnsafe, this.markers, this.cdata, text, this.afterText);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Xml.CharData withAfterText(final String afterText) {
            return this.afterText == afterText ? this : new Xml.CharData(this.id, this.prefixUnsafe, this.markers, this.cdata, this.text, afterText);
        }
    }


    final class Comment implements Xml, Content, Misc {
        private final UUID id;
        private final String prefixUnsafe;

        public Comment withPrefix(String prefix) {
            return WithPrefix.onlyIfNotEqual(this, prefix);
        }

        public String getPrefix() {
            return prefixUnsafe;
        }

        private final Markers markers;
        private final String text;

        @Override
        public <P> Xml acceptXml(XmlVisitor<P> v, P p) {
            return v.visitComment(this, p);
        }

        @Override
        public String toString() {
            return "<!--" + text + "-->";
        }

        public Comment(final UUID id, final String prefixUnsafe, final Markers markers, final String text) {
            this.id = id;
            this.prefixUnsafe = prefixUnsafe;
            this.markers = markers;
            this.text = text;
        }

        public UUID getId() {
            return this.id;
        }

        public String getPrefixUnsafe() {
            return this.prefixUnsafe;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public String getText() {
            return this.text;
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof Xml.Comment)) return false;
            final Xml.Comment other = (Xml.Comment) o;
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
        public Xml.Comment withId(final UUID id) {
            return this.id == id ? this : new Xml.Comment(id, this.prefixUnsafe, this.markers, this.text);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Xml.Comment withPrefixUnsafe(final String prefixUnsafe) {
            return this.prefixUnsafe == prefixUnsafe ? this : new Xml.Comment(this.id, prefixUnsafe, this.markers, this.text);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Xml.Comment withMarkers(final Markers markers) {
            return this.markers == markers ? this : new Xml.Comment(this.id, this.prefixUnsafe, markers, this.text);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Xml.Comment withText(final String text) {
            return this.text == text ? this : new Xml.Comment(this.id, this.prefixUnsafe, this.markers, text);
        }
    }


    final class DocTypeDecl implements Xml, Misc {
        private final UUID id;
        private final String prefixUnsafe;

        public DocTypeDecl withPrefix(String prefix) {
            return WithPrefix.onlyIfNotEqual(this, prefix);
        }

        public String getPrefix() {
            return prefixUnsafe;
        }

        private final Markers markers;
        private final Ident name;
        @Nullable
        private final Ident externalId;
        private final List<Ident> internalSubset;
        @Nullable
        private final ExternalSubsets externalSubsets;
        /**
         * Space before '&gt;'.
         */
        private final String beforeTagDelimiterPrefix;


        public static final class ExternalSubsets implements Xml {
            private final UUID id;
            private final String prefixUnsafe;

            public ExternalSubsets withPrefix(String prefix) {
                return WithPrefix.onlyIfNotEqual(this, prefix);
            }

            public String getPrefix() {
                return prefixUnsafe;
            }

            private final Markers markers;
            private final List<Element> elements;

            public ExternalSubsets(final UUID id, final String prefixUnsafe, final Markers markers, final List<Element> elements) {
                this.id = id;
                this.prefixUnsafe = prefixUnsafe;
                this.markers = markers;
                this.elements = elements;
            }

            public UUID getId() {
                return this.id;
            }

            public String getPrefixUnsafe() {
                return this.prefixUnsafe;
            }

            public Markers getMarkers() {
                return this.markers;
            }

            public List<Element> getElements() {
                return this.elements;
            }

            @org.openrewrite.internal.lang.NonNull
            @Override
            public String toString() {
                return "Xml.DocTypeDecl.ExternalSubsets(id=" + this.getId() + ", prefixUnsafe=" + this.getPrefixUnsafe() + ", markers=" + this.getMarkers() + ", elements=" + this.getElements() + ")";
            }

            @Override
            public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
                if (o == this) return true;
                if (!(o instanceof Xml.DocTypeDecl.ExternalSubsets)) return false;
                final Xml.DocTypeDecl.ExternalSubsets other = (Xml.DocTypeDecl.ExternalSubsets) o;
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
            public Xml.DocTypeDecl.ExternalSubsets withId(final UUID id) {
                return this.id == id ? this : new Xml.DocTypeDecl.ExternalSubsets(id, this.prefixUnsafe, this.markers, this.elements);
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public Xml.DocTypeDecl.ExternalSubsets withPrefixUnsafe(final String prefixUnsafe) {
                return this.prefixUnsafe == prefixUnsafe ? this : new Xml.DocTypeDecl.ExternalSubsets(this.id, prefixUnsafe, this.markers, this.elements);
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public Xml.DocTypeDecl.ExternalSubsets withMarkers(final Markers markers) {
                return this.markers == markers ? this : new Xml.DocTypeDecl.ExternalSubsets(this.id, this.prefixUnsafe, markers, this.elements);
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public Xml.DocTypeDecl.ExternalSubsets withElements(final List<Element> elements) {
                return this.elements == elements ? this : new Xml.DocTypeDecl.ExternalSubsets(this.id, this.prefixUnsafe, this.markers, elements);
            }
        }

        @Override
        public <P> Xml acceptXml(XmlVisitor<P> v, P p) {
            return v.visitDocTypeDecl(this, p);
        }

        public DocTypeDecl(final UUID id, final String prefixUnsafe, final Markers markers, final Ident name, @Nullable final Ident externalId, final List<Ident> internalSubset, @Nullable final ExternalSubsets externalSubsets, final String beforeTagDelimiterPrefix) {
            this.id = id;
            this.prefixUnsafe = prefixUnsafe;
            this.markers = markers;
            this.name = name;
            this.externalId = externalId;
            this.internalSubset = internalSubset;
            this.externalSubsets = externalSubsets;
            this.beforeTagDelimiterPrefix = beforeTagDelimiterPrefix;
        }

        public UUID getId() {
            return this.id;
        }

        public String getPrefixUnsafe() {
            return this.prefixUnsafe;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public Ident getName() {
            return this.name;
        }

        @Nullable
        public Ident getExternalId() {
            return this.externalId;
        }

        public List<Ident> getInternalSubset() {
            return this.internalSubset;
        }

        @Nullable
        public ExternalSubsets getExternalSubsets() {
            return this.externalSubsets;
        }

        /**
         * Space before '&gt;'.
         */
        public String getBeforeTagDelimiterPrefix() {
            return this.beforeTagDelimiterPrefix;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "Xml.DocTypeDecl(id=" + this.getId() + ", prefixUnsafe=" + this.getPrefixUnsafe() + ", markers=" + this.getMarkers() + ", name=" + this.getName() + ", externalId=" + this.getExternalId() + ", internalSubset=" + this.getInternalSubset() + ", externalSubsets=" + this.getExternalSubsets() + ", beforeTagDelimiterPrefix=" + this.getBeforeTagDelimiterPrefix() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof Xml.DocTypeDecl)) return false;
            final Xml.DocTypeDecl other = (Xml.DocTypeDecl) o;
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
        public Xml.DocTypeDecl withId(final UUID id) {
            return this.id == id ? this : new Xml.DocTypeDecl(id, this.prefixUnsafe, this.markers, this.name, this.externalId, this.internalSubset, this.externalSubsets, this.beforeTagDelimiterPrefix);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Xml.DocTypeDecl withPrefixUnsafe(final String prefixUnsafe) {
            return this.prefixUnsafe == prefixUnsafe ? this : new Xml.DocTypeDecl(this.id, prefixUnsafe, this.markers, this.name, this.externalId, this.internalSubset, this.externalSubsets, this.beforeTagDelimiterPrefix);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Xml.DocTypeDecl withMarkers(final Markers markers) {
            return this.markers == markers ? this : new Xml.DocTypeDecl(this.id, this.prefixUnsafe, markers, this.name, this.externalId, this.internalSubset, this.externalSubsets, this.beforeTagDelimiterPrefix);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Xml.DocTypeDecl withName(final Ident name) {
            return this.name == name ? this : new Xml.DocTypeDecl(this.id, this.prefixUnsafe, this.markers, name, this.externalId, this.internalSubset, this.externalSubsets, this.beforeTagDelimiterPrefix);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Xml.DocTypeDecl withExternalId(@Nullable final Ident externalId) {
            return this.externalId == externalId ? this : new Xml.DocTypeDecl(this.id, this.prefixUnsafe, this.markers, this.name, externalId, this.internalSubset, this.externalSubsets, this.beforeTagDelimiterPrefix);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Xml.DocTypeDecl withInternalSubset(final List<Ident> internalSubset) {
            return this.internalSubset == internalSubset ? this : new Xml.DocTypeDecl(this.id, this.prefixUnsafe, this.markers, this.name, this.externalId, internalSubset, this.externalSubsets, this.beforeTagDelimiterPrefix);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Xml.DocTypeDecl withExternalSubsets(@Nullable final ExternalSubsets externalSubsets) {
            return this.externalSubsets == externalSubsets ? this : new Xml.DocTypeDecl(this.id, this.prefixUnsafe, this.markers, this.name, this.externalId, this.internalSubset, externalSubsets, this.beforeTagDelimiterPrefix);
        }

        /**
         * Space before '&gt;'.
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Xml.DocTypeDecl withBeforeTagDelimiterPrefix(final String beforeTagDelimiterPrefix) {
            return this.beforeTagDelimiterPrefix == beforeTagDelimiterPrefix ? this : new Xml.DocTypeDecl(this.id, this.prefixUnsafe, this.markers, this.name, this.externalId, this.internalSubset, this.externalSubsets, beforeTagDelimiterPrefix);
        }
    }


    final class Element implements Xml {
        private final UUID id;
        private final String prefixUnsafe;

        public Element withPrefix(String prefix) {
            return WithPrefix.onlyIfNotEqual(this, prefix);
        }

        public String getPrefix() {
            return prefixUnsafe;
        }

        private final Markers markers;
        private final List<Ident> subset;
        /**
         * Space before '&gt;'
         */
        private final String beforeTagDelimiterPrefix;

        @Override
        public <P> Xml acceptXml(XmlVisitor<P> v, P p) {
            return v.visitElement(this, p);
        }

        public Element(final UUID id, final String prefixUnsafe, final Markers markers, final List<Ident> subset, final String beforeTagDelimiterPrefix) {
            this.id = id;
            this.prefixUnsafe = prefixUnsafe;
            this.markers = markers;
            this.subset = subset;
            this.beforeTagDelimiterPrefix = beforeTagDelimiterPrefix;
        }

        public UUID getId() {
            return this.id;
        }

        public String getPrefixUnsafe() {
            return this.prefixUnsafe;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public List<Ident> getSubset() {
            return this.subset;
        }

        /**
         * Space before '&gt;'
         */
        public String getBeforeTagDelimiterPrefix() {
            return this.beforeTagDelimiterPrefix;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "Xml.Element(id=" + this.getId() + ", prefixUnsafe=" + this.getPrefixUnsafe() + ", markers=" + this.getMarkers() + ", subset=" + this.getSubset() + ", beforeTagDelimiterPrefix=" + this.getBeforeTagDelimiterPrefix() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof Xml.Element)) return false;
            final Xml.Element other = (Xml.Element) o;
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
        public Xml.Element withId(final UUID id) {
            return this.id == id ? this : new Xml.Element(id, this.prefixUnsafe, this.markers, this.subset, this.beforeTagDelimiterPrefix);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Xml.Element withPrefixUnsafe(final String prefixUnsafe) {
            return this.prefixUnsafe == prefixUnsafe ? this : new Xml.Element(this.id, prefixUnsafe, this.markers, this.subset, this.beforeTagDelimiterPrefix);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Xml.Element withMarkers(final Markers markers) {
            return this.markers == markers ? this : new Xml.Element(this.id, this.prefixUnsafe, markers, this.subset, this.beforeTagDelimiterPrefix);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Xml.Element withSubset(final List<Ident> subset) {
            return this.subset == subset ? this : new Xml.Element(this.id, this.prefixUnsafe, this.markers, subset, this.beforeTagDelimiterPrefix);
        }

        /**
         * Space before '&gt;'
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Xml.Element withBeforeTagDelimiterPrefix(final String beforeTagDelimiterPrefix) {
            return this.beforeTagDelimiterPrefix == beforeTagDelimiterPrefix ? this : new Xml.Element(this.id, this.prefixUnsafe, this.markers, this.subset, beforeTagDelimiterPrefix);
        }
    }


    final class Ident implements Xml {
        private final UUID id;
        private final String prefixUnsafe;

        public Ident withPrefix(String prefix) {
            return WithPrefix.onlyIfNotEqual(this, prefix);
        }

        public String getPrefix() {
            return prefixUnsafe;
        }

        private final Markers markers;
        private final String name;

        @Override
        public <P> Xml acceptXml(XmlVisitor<P> v, P p) {
            return v.visitIdent(this, p);
        }

        @Override
        public String toString() {
            return "Ident{" + name + "}";
        }

        public Ident(final UUID id, final String prefixUnsafe, final Markers markers, final String name) {
            this.id = id;
            this.prefixUnsafe = prefixUnsafe;
            this.markers = markers;
            this.name = name;
        }

        public UUID getId() {
            return this.id;
        }

        public String getPrefixUnsafe() {
            return this.prefixUnsafe;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public String getName() {
            return this.name;
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof Xml.Ident)) return false;
            final Xml.Ident other = (Xml.Ident) o;
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
        public Xml.Ident withId(final UUID id) {
            return this.id == id ? this : new Xml.Ident(id, this.prefixUnsafe, this.markers, this.name);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Xml.Ident withPrefixUnsafe(final String prefixUnsafe) {
            return this.prefixUnsafe == prefixUnsafe ? this : new Xml.Ident(this.id, prefixUnsafe, this.markers, this.name);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Xml.Ident withMarkers(final Markers markers) {
            return this.markers == markers ? this : new Xml.Ident(this.id, this.prefixUnsafe, markers, this.name);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Xml.Ident withName(final String name) {
            return this.name == name ? this : new Xml.Ident(this.id, this.prefixUnsafe, this.markers, name);
        }
    }
}
