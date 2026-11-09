// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.compiler;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.TreeMap;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;
import jdk.m3.collections.ParentOwned;

/**
 * Opt-in javac annotation processor. Emits deterministic, conservative sidecar
 * metadata. Neither static reference counts nor escape proofs are fabricated.
 */
@SupportedAnnotationTypes("jdk.m3.collections.ParentOwned")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/eaacff5931ef16a1a52425043bb71b821eda3515/synexia-primitives/src/main/java/com/synexia/primitives/M3ParentOwnedProcessor.java blob 9bb160d48a44; recipe m3/compiler/parent-owned/recipe/parent-owned-processor-20261107/relocate.py v1
public final class ParentOwnedProcessor extends AbstractProcessor {
    /** Instantiated by javac (opt-in through -processor or the Processors API); explicit for the exported module package. */
    public ParentOwnedProcessor() {
    }

    private final Map<String, String> rows = new TreeMap<>();
    private boolean emitted;

    @Override
    public boolean process(java.util.Set<? extends TypeElement> annotations,
                           RoundEnvironment round) {
        for (Element element : round.getElementsAnnotatedWith(ParentOwned.class)) {
            ParentOwned hint = element.getAnnotation(ParentOwned.class);
            if (hint == null) continue;
            String id = identifier(element);
            String owner = hint.owner();
            if (owner.indexOf('\t') >= 0 || owner.indexOf('\n') >= 0
                    || owner.indexOf('\r') >= 0) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "M3 owner may not contain control delimiters", element);
                continue;
            }
            String row = id + "\t" + element.getKind() + "\t" + hint.flags()
                    + "\t" + owner + "\tUNKNOWN\tUNVERIFIED";
            String previous = rows.putIfAbsent(id, row);
            if (previous != null && !previous.equals(row)) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "conflicting M3 lifecycle metadata: " + id, element);
            }
        }
        if (round.processingOver() && !emitted && !rows.isEmpty()) {
            emitted = true;
            try (Writer out = processingEnv.getFiler()
                    .createResource(StandardLocation.CLASS_OUTPUT, "",
                            "META-INF/m3/parent-owned-v1.tsv").openWriter()) {
                out.write("symbol\tkind\tflags\towner\tstaticReferences\tescapeProof\n");
                for (String row : rows.values()) {
                    out.write(row);
                    out.write('\n');
                }
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "cannot publish M3 lifecycle sidecar: " + e.getMessage());
            }
        }
        return true;
    }

    private static String identifier(Element element) {
        if (element instanceof TypeElement type) return type.getQualifiedName().toString();
        Element owner = element.getEnclosingElement();
        while (!(owner instanceof TypeElement) && owner != null) {
            owner = owner.getEnclosingElement();
        }
        if (!(owner instanceof TypeElement type)) {
            throw new IllegalArgumentException("no enclosing type for " + element);
        }
        StringBuilder id = new StringBuilder(type.getQualifiedName())
                .append('#').append(element.getSimpleName());
        if (element instanceof ExecutableElement executable) {
            id.append('(');
            for (int i = 0; i < executable.getParameters().size(); i++) {
                if (i != 0) id.append(',');
                id.append(executable.getParameters().get(i).asType());
            }
            id.append(')');
        }
        return id.toString();
    }
}
