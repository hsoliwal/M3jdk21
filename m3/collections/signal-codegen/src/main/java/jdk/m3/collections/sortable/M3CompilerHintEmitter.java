// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package jdk.m3.collections.sortable;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;

/**
 * Source-time MOP hint transport over the existing SortableProcessor fact lanes.
 *
 * <p>Hints are diagnostic candidates, not GoF verification, performance measurements,
 * a completed compilation, or authority to rewrite Java/JNI. The processor is the only
 * fact manufacturer. Maven consumes one deterministic per-type receipt after successful
 * javac execution and joins it to its independently verified M3File/M3AST generation.</p>
 */
public final class M3CompilerHintEmitter {
    public static final String OPTION = "m3.machineHints";
    public static final String SCHEMA = "M3_MOP_HINT_RECEIPT_V1";
    public static final String RECEIPT_DIRECTORY = "META-INF/m3/pattern-hints/";

    private M3CompilerHintEmitter() { }

    /**
     * Off by default to preserve existing javac diagnostics and generated companions.
     * Supported -A values: off, notes, receipts, both. Notes never become warnings.
     */
    public static void emit(ProcessingEnvironment environment, TypeElement owner,
                            int[] counts, long validity) {
        Objects.requireNonNull(environment, "environment");
        Objects.requireNonNull(owner, "owner");
        String mode = environment.getOptions().getOrDefault(OPTION, "off");
        if (!mode.equals("off") && !mode.equals("notes")
                && !mode.equals("receipts") && !mode.equals("both")) {
            environment.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "M3 machineHints must be off, notes, receipts or both", owner);
            return;
        }
        if (mode.equals("off")) return;
        // This is the canonical tri-state rule owner; UNKNOWN is never NOT_OBSERVED.
        M3MachineHintRules hints = M3MachineHintRules.fromFacts(counts, new long[] {validity});
        boolean notes = mode.equals("notes") || mode.equals("both");
        boolean receipts = mode.equals("receipts") || mode.equals("both");
        if (notes) {
            for (int id = 0; id < M3MachineHintRules.COUNT; id++) {
                if (hints.state(id) != M3MachineHintRules.State.CANDIDATE) continue;
                environment.getMessager().printMessage(Diagnostic.Kind.NOTE,
                        "[M3-MOP:CANDIDATE] " + M3MachineHintRules.name(id)
                                + " — compiler-fact pattern review; not proof or rewrite permission",
                        owner);
            }
        }
        if (receipts) {
            String qualifiedName = owner.getQualifiedName().toString();
            if (qualifiedName.isEmpty() || qualifiedName.indexOf('/') >= 0
                    || qualifiedName.indexOf('\\') >= 0) {
                environment.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "M3 pattern hint type identity is invalid", owner);
                return;
            }
            String path = RECEIPT_DIRECTORY + qualifiedName.replace('.', '/') + ".tsv";
            String receipt = receipt(qualifiedName, counts, validity, hints);
            try (Writer writer = environment.getFiler()
                    .createResource(StandardLocation.CLASS_OUTPUT, "", path, owner).openWriter()) {
                writer.write(receipt);
            } catch (IOException failure) {
                environment.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "M3 pattern hint receipt could not be published: "
                                + failure.getClass().getSimpleName(), owner);
            }
        }
    }

    /** Stable text protocol for Maven; fact counts are source-time, not runtime statistics. */
    public static String receipt(String qualifiedName, int[] counts, long validity,
                                 M3MachineHintRules hints) {
        Objects.requireNonNull(qualifiedName, "qualifiedName");
        Objects.requireNonNull(counts, "counts");
        Objects.requireNonNull(hints, "hints");
        // Recompute rather than trusting supplied masks or an unverified caller.
        M3MachineHintRules expected = M3MachineHintRules.fromFacts(counts, new long[] {validity});
        if (expected.knownMask() != hints.knownMask()
                || expected.candidateMask() != hints.candidateMask()
                || qualifiedName.isBlank() || qualifiedName.length() > 4096) {
            throw new IllegalArgumentException("unverified machine hint receipt");
        }
        StringBuilder text = new StringBuilder(SCHEMA)
                .append("\tSOURCE_TIME_ONLY\tCANDIDATE_ONLY\n")
                .append("TYPE\t")
                .append(Base64.getUrlEncoder().withoutPadding().encodeToString(
                        qualifiedName.getBytes(StandardCharsets.UTF_8)))
                .append('\t').append(HexFormat.of().toHexDigits(validity))
                .append('\t').append(HexFormat.of().toHexDigits(hints.knownMask()))
                .append('\t').append(HexFormat.of().toHexDigits(hints.candidateMask()))
                .append('\n');
        for (int i = 0; i < counts.length; i++) {
            text.append("F\t").append(i).append('\t')
                    .append((validity & (1L << i)) == 0L ? "UNKNOWN" : Integer.toString(counts[i]))
                    .append('\n');
        }
        for (int i = 0; i < M3MachineHintRules.COUNT; i++) {
            text.append("H\t").append(i).append('\t').append(M3MachineHintRules.name(i))
                    .append('\t').append(hints.state(i)).append('\n');
        }
        String body = text.toString();
        return body + "SHA256\t" + sha256(body) + "\n";
    }

    /** Hash covers the exact UTF-8 receipt body; it is not the source-file digest. */
    private static String sha256(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }
}
