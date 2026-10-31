/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

/*
 * @test
 * @bug 8284315
 * @summary DocTrees.getElement returns null for type-only primitive/array references
 * @modules jdk.compiler
 * @build DocTreesElementTypeConsistencyTest
 * @compile -processor DocTreesElementTypeConsistencyTest -proc:only DocTreesElementTypeConsistencyTest.java
 */

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.LinkTree;
import com.sun.source.doctree.ReferenceTree;
import com.sun.source.doctree.TextTree;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.DocTreePathScanner;
import com.sun.source.util.DocTrees;

import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/**
 * {@link int type:INT}
 * {@link java.lang.String[] type:ARRAY}
 * {@link java.lang.String element:CLASS}
 * {@link java.base/java.util.List<String> element:INTERFACE}
 * {@link java.base/java.util.List<String>#add(Object) element:METHOD}
 */
@SupportedAnnotationTypes("*")
public class DocTreesElementTypeConsistencyTest extends AbstractProcessor {
    private DocTrees trees;

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        trees = DocTrees.instance(processingEnvironment);
    }

    @Override
    public boolean process(
            Set<? extends TypeElement> annotations,
            RoundEnvironment roundEnvironment) {
        for (Element element : roundEnvironment.getRootElements()) {
            DocCommentTree comment = trees.getDocCommentTree(element);
            if (comment == null) {
                continue;
            }
            new Scanner(trees.getPath(element), comment).scan();
        }
        return true;
    }

    private final class Scanner extends DocTreePathScanner<Void, Void> {
        private final com.sun.source.util.TreePath path;
        private final DocCommentTree comment;

        Scanner(com.sun.source.util.TreePath path, DocCommentTree comment) {
            this.path = path;
            this.comment = comment;
        }

        void scan() {
            scan(new DocTreePath(path, comment), null);
        }

        @Override
        public Void visitLink(LinkTree tree, Void unused) {
            check(tree);
            return null;
        }

        private void check(LinkTree link) {
            if (link.getLabel().isEmpty()
                    || !(link.getLabel().get(0) instanceof TextTree text)) {
                throw new AssertionError("missing expectation label: " + link);
            }

            ReferenceTree reference = link.getReference();
            DocTreePath referencePath = new DocTreePath(getCurrentPath(), reference);
            Element element = trees.getElement(referencePath);
            TypeMirror type = trees.getType(referencePath);
            String expected = text.getBody();

            if (expected.startsWith("type:")) {
                TypeKind kind = TypeKind.valueOf(expected.substring("type:".length()));
                if (element != null) {
                    throw new AssertionError(
                            reference.getSignature()
                                    + " unexpectedly resolved element "
                                    + element);
                }
                if (type == null || type.getKind() != kind) {
                    throw new AssertionError(
                            reference.getSignature()
                                    + " type="
                                    + type
                                    + ", expected kind "
                                    + kind);
                }
                return;
            }

            if (expected.startsWith("element:")) {
                String kind = expected.substring("element:".length());
                if (element == null || !element.getKind().name().equals(kind)) {
                    throw new AssertionError(
                            reference.getSignature()
                                    + " element="
                                    + element
                                    + ", expected kind "
                                    + kind);
                }
                if (type == null) {
                    throw new AssertionError(
                            reference.getSignature() + " should also resolve a type");
                }
                return;
            }

            throw new AssertionError("unknown expectation: " + expected);
        }
    }
}
