/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Mechanical receipt gate for the Synexia PR #10124 code-text handoff.
 *
 * <p>This proves source/target custody and field coverage only. It does not claim a full
 * M3JDK build, JNI execution, or promotion of candidate signals into semantic authority.</p>
 */
public final class M3CodeTextPrecomputeCoverageTest {
    private static final String SOURCE_REPO = "hsoliwal/com.synexia";
    private static final String SOURCE_PR = "10124";
    private static final String SOURCE_HEAD =
            "700208dda3c0ba970fcac6c1e17c1830a986b42b";
    private static final String TARGET_REF =
            "codex/m3jdk-precompute-coverage-invariant-20261010";
    private static final Set<String> REQUIRED_FIELDS = Set.of(
            "utf16Length", "codePointCount", "textFlags", "contentHash64", "presence64",
            "simHash64", "lexicalPacked", "javaKeywordHits", "codeScore", "regexScore", "minHash");

    public static void main(String[] args) throws Exception {
        Path root = Path.of(".");
        if (!Files.exists(root.resolve("m3/lexicon/synexia-code-text-precompute-field-map.tsv"))) {
            root = Path.of("..");
        }
        Path map = root.resolve("m3/lexicon/synexia-code-text-precompute-field-map.tsv");
        Path receipt = root.resolve("m3/lexicon/synexia-code-text-precompute-receipt-10124.tsv");
        Path nativeReceipt = root.resolve("m3/lexicon/synexia-code-text-native-jni-receipt-10124.tsv");
        Path facts = root.resolve("src/java.base/share/classes/java/lang/M3CodeTextFacts.java");
        Path batch = root.resolve("src/java.base/share/classes/java/lang/M3CodeTextSignalBatch.java");
        Path nativeBridge = root.resolve(
                "src/java.base/share/classes/java/lang/M3CodeTextNativeBridge.java");
        Path nativeContract = root.resolve(
                "src/java.base/share/classes/java/lang/M3CodeTextNativeContract.java");
        check(Files.exists(map) && Files.exists(receipt) && Files.exists(nativeReceipt),
                "code-text receipt files are missing");
        check(Files.exists(facts) && Files.exists(batch) && Files.exists(nativeBridge)
                        && Files.exists(nativeContract),
                "code-text receiver files are missing");

        List<String> mapLines = Files.readAllLines(map, StandardCharsets.UTF_8);
        check(mapLines.size() == REQUIRED_FIELDS.size() + 1, "field map row count changed");
        Set<String> fields = new HashSet<>();
        for (String line : mapLines.subList(1, mapLines.size())) {
            String[] columns = line.split("\\t", -1);
            check(columns.length == 15, "field map row is malformed: " + line);
            check(columns[1].equals(SOURCE_REPO)
                            && columns[2].equals(SOURCE_PR)
                            && columns[3].equals(SOURCE_HEAD),
                    "field map source pin drift: " + line);
            check(columns[8].equals("hsoliwal/M3jdk21")
                            && columns[9].equals(TARGET_REF)
                            && columns[10].equals(
                                    "src/java.base/share/classes/java/lang/M3CodeTextFacts.java")
                            && columns[11].equals("java.lang.M3CodeTextFacts")
                            && columns[13].equals("MAPPED"),
                    "field map target drift: " + line);
            fields.add(columns[7]);
        }
        check(fields.equals(REQUIRED_FIELDS), "code-text field coverage changed: " + fields);

        List<String> receiptLines = Files.readAllLines(receipt, StandardCharsets.UTF_8);
        check(receiptLines.size() == 6, "receipt row count changed");
        Set<String> statuses = new HashSet<>();
        boolean corpusBound = false;
        for (String line : receiptLines.subList(1, receiptLines.size())) {
            String[] columns = line.split("\\t", -1);
            check(columns.length == 14, "receipt row is malformed: " + line);
            check(columns[1].equals(SOURCE_REPO)
                            && columns[2].equals(SOURCE_PR)
                            && columns[3].equals(SOURCE_HEAD)
                            && columns[7].equals("hsoliwal/M3jdk21")
                            && columns[8].equals(TARGET_REF),
                    "receipt pin drift: " + line);
            statuses.add(columns[12]);
            if (columns[6].equals("v6")) {
                corpusBound = columns[13].contains("9702 pairs")
                        && columns[13].contains(
                                "11b5b883594988f3d85212ae6da0c016411ba149f49d46838d913be025e16359")
                        && columns[13].contains("not claimed");
            }
        }
        check(statuses.contains("ADMITTED_TYPED_RECEIVER")
                        && statuses.contains("HOST_JAVA_RECEIVER_PROVEN")
                        && statuses.contains("DIFFERENTIAL_TARGET_BOUND")
                        && statuses.contains("RECIPE_BOUND_NOT_RUNTIME_PROVEN")
                        && statuses.contains("TEST_CORPUS_RECEIPT_BOUND"),
                "receipt status coverage changed: " + statuses);
        check(corpusBound, "V6 corpus receipt is not bound");
        checkNativeReceipt(nativeReceipt, nativeContract, nativeBridge);

        System.out.println("M3JDK_CODE_TEXT_PRECOMPUTE_COVERAGE_PASS fields="
                + fields.size() + " receipt_rows=" + (receiptLines.size() - 1)
                + " native_jni=1 native_bridge=1 source_pr=" + SOURCE_PR
                + " target_ref=" + TARGET_REF);
    }

    private static void checkNativeReceipt(
            Path receipt, Path nativeContract, Path nativeBridge) throws Exception {
        final String nativeHead = "f446bc7f9978aca24103cfab0220500d265e41ab";
        final String nativeBlob = "34f247f4f2b74862997d6e82f2938b4310bbc5a2";
        final String targetBlob = "b2e00337be3be4fbe83452b88ec6fe336646475f";
        String contract = Files.readString(nativeContract, StandardCharsets.UTF_8);
        String bridge = Files.readString(nativeBridge, StandardCharsets.UTF_8);
        check(contract.contains("nativeAnalyzeRange")
                        && contract.contains("([C[I[III)[J")
                        && contract.contains("27b6f34ab44b00e82ec814660f249f0b6bfd7cbd")
                        && contract.contains("6b1e4a5716aacfa71c33aedf5ed249f4af4a325d")
                        && contract.contains(
                                "Java_com_synexia_indexstring_JniMIndexCodeTextSignalBatch_nativeAnalyzeRange")
                        && contract.contains("java.lang.M3CodeTextNativeBridge")
                        && contract.contains(
                                "Java_java_lang_M3CodeTextNativeBridge_nativeAnalyzeRange"),
                "native JNI ABI contract is incomplete");
        check(bridge.contains("m3.code.text.native.path")
                        && bridge.contains("M3CodeTextSignalBatch.validate")
                        && bridge.contains("M3CodeTextSignalBatch.analyze")
                        && bridge.contains("nativeAnalyzeRange")
                        && bridge.contains("host Java"),
                "optional native bridge fallback contract is incomplete");
        List<String> lines = Files.readAllLines(receipt, StandardCharsets.UTF_8);
        check(lines.size() == 2, "native JNI receipt row count changed");
        String[] columns = lines.get(1).split("\\t", -1);
        check(columns.length == 14, "native JNI receipt row is malformed");
        check(columns[1].equals(SOURCE_REPO)
                        && columns[2].equals(SOURCE_PR)
                        && columns[3].equals(nativeHead)
                        && columns[4].equals(
                                "synexia-indexstring/native/src/mindex_code_text_signal_jni.c")
                        && columns[5].equals(nativeBlob)
                        && columns[7].equals("hsoliwal/M3jdk21")
                        && columns[8].equals(TARGET_REF)
                        && columns[10].equals(targetBlob)
                        && columns[11].equals("java.lang.M3CodeTextNativeBridge")
                        && columns[12].equals("ADMITTED_NATIVE_JNI_PROOF")
                        && columns[13].contains("SYNEXIA_CODE_TEXT_NATIVE_JNI_PASS")
                        && columns[13].contains(
                                "Java_java_lang_M3CodeTextNativeBridge_nativeAnalyzeRange")
                        && columns[13].contains("M3_CODE_TEXT_NATIVE_BRIDGE_PASS")
                        && columns[13].contains("host Java fallback authoritative"),
                "native JNI receipt drift");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
