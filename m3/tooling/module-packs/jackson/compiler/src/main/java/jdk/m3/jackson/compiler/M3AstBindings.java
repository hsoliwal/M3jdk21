/* Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.jackson.compiler;

import com.sun.source.util.M3AST;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import jdk.m3.jackson.M3Binding;
import jdk.m3.jackson.M3ObjectMapper;

/**
 * Detached Jackson-readable declaration metadata from the existing M3AST session.
 * The compiler owns parsing/attribution and memoizes one projection per analyzed
 * event. This class retains no Unit, tree, element, type mirror or compiler task.
 *
 * <p>Declaration facts describe Java source, not Jackson's configurable property
 * model. A prepared runtime binding is always built by the configured mapper
 * against the actual Class; source metadata never overrides annotations, mixins,
 * custom modules, visibility or type resolution. No global class/source cache is
 * introduced, and the source digest is not a classpath or bytecode-equivalence key.
 */
public final class M3AstBindings implements M3AST.Consumer {
    private static final M3AST.Projection<TypeShape> SHAPE =
        new M3AST.Projection<>("jdk.m3.jackson.declaration/1", M3AstBindings::capture);
    private final Consumer<? super TypeShape> sink;

    /** The sink controls retention and runs on the compiler callback thread. */
    public M3AstBindings(Consumer<? super TypeShape> sink) {
        this.sink = Objects.requireNonNull(sink, "sink");
    }

    /** Reuse this identity to share exact-event work with another consumer. */
    public static M3AST.Projection<TypeShape> projection() { return SHAPE; }

    @Override public void analyzed(M3AST.Unit unit) throws Exception {
        sink.accept(unit.project(SHAPE));
    }

    public record Member(String name, String javaType, List<String> annotations) {
        public Member {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(javaType, "javaType");
            annotations = List.copyOf(annotations);
        }
    }

    /** Source-sealed immutable facts; JSON serialization is ordinary Jackson binding. */
    public record TypeShape(String qualifiedName, String kind, URI sourceUri,
                            String sourceUtf16Sha256, List<Member> declaredFields,
                            List<Member> recordComponents) {
        public TypeShape {
            Objects.requireNonNull(qualifiedName, "qualifiedName");
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(sourceUri, "sourceUri");
            Objects.requireNonNull(sourceUtf16Sha256, "sourceUtf16Sha256");
            declaredFields = List.copyOf(declaredFields);
            recordComponents = List.copyOf(recordComponents);
        }
    }

    /**
     * Bind a caller-selected runtime class after checking declaration identity.
     * Name/record-shape checks catch mismatched inputs; they do not certify source
     * versus bytecode equivalence. Jackson inspects the actual Class and settings.
     */
    public static <T> M3Binding<T> prepare(TypeShape shape, Class<T> type,
                                          M3ObjectMapper mapper) {
        Objects.requireNonNull(shape, "shape");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(mapper, "mapper");
        if (!shape.qualifiedName().equals(type.getCanonicalName())) {
            throw new IllegalArgumentException("source declaration and runtime class names differ");
        }
        if (shape.kind().equals(ElementKind.RECORD.name()) != type.isRecord()) {
            throw new IllegalArgumentException("source and runtime record kinds differ");
        }
        if (type.isRecord()) {
            var components = type.getRecordComponents();
            if (components.length != shape.recordComponents().size()) {
                throw new IllegalArgumentException("source and runtime record component counts differ");
            }
            for (int i = 0; i < components.length; i++) {
                if (!components[i].getName().equals(shape.recordComponents().get(i).name())) {
                    throw new IllegalArgumentException("source and runtime record component order differs");
                }
            }
        }
        return mapper.prepare(type);
    }

    private static TypeShape capture(M3AST.Unit unit) throws IOException {
        if (unit.phase() != M3AST.Phase.ANALYZED) {
            throw new IllegalArgumentException("binding metadata requires attributed M3AST facts");
        }
        TypeElement type = Objects.requireNonNull(unit.analyzedType(), "analyzed type");
        M3AST.Source source = unit.source();
        List<Member> fields = type.getEnclosedElements().stream()
            .filter(e -> e.getKind() == ElementKind.FIELD || e.getKind() == ElementKind.ENUM_CONSTANT)
            .map(M3AstBindings::member).toList();
        List<Member> components = type.getRecordComponents().stream()
            .map(M3AstBindings::member).toList();
        return new TypeShape(type.getQualifiedName().toString(), type.getKind().name(),
            source.uri(), source.utf16Sha256(), fields, components);
    }

    private static Member member(Element element) {
        return new Member(element.getSimpleName().toString(), element.asType().toString(),
            element.getAnnotationMirrors().stream()
                .map(annotation -> annotation.getAnnotationType().toString()).toList());
    }
}
