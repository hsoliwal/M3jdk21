/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import com.m3.text.M3NumberSpace;
import com.m3.text.LocalM3StringPiece;

/** Contract proof for the source-manifest to M3NumberSpace owner mapping. */
public final class M3NumberSpaceContractTest {
    private static int checks;

    private static void check(boolean condition) {
        checks++;
        if (!condition) {
            throw new AssertionError("check " + checks);
        }
    }

    public static void main(String[] args) throws Exception {
        M3NumberSpace space = M3NumberSpace.INSTANCE;
        check(M3NumberSpace.SOURCE_ID.equals("dictlang.numbers.0-10000"));
        check(M3NumberSpace.SOURCE_REVISION.equals("64a2ea61c73b548413fed6686a9daeeb0b9b0564"));
        check(M3NumberSpace.RECORD_ID.equals("number"));
        check(M3NumberSpace.MIN_VALUE == 0);
        check(M3NumberSpace.MAX_VALUE == 10_000);
        check(space.precomputedValueCount() == 10_001);
        check(space.number(0).storageIdentity().equals(space.number(10_000).storageIdentity()));
        check(space.number(0).encoding() == LocalM3StringPiece.Encoding.UTF16_LE);
        check(space.spelling(0).equals("0"));
        check(space.spelling(10_000).equals("10000"));
        check(space.parse(new View("0")) == space.number(0));
        check(space.parse(new View("10000")) == space.number(10_000));
        check(expectNumberFormat(space, "-1").startsWith("non-decimal number spelling:"));
        check(expectNumberFormat(space, "10001").startsWith("number outside 0..10000:"));
        check(expectNumberFormat(space, "01").startsWith("non-canonical number spelling:"));

        String manifest = Files.readString(
                Path.of("lexicon/synexia-source-manifest.tsv"), StandardCharsets.UTF_8);
        String row = Arrays.stream(manifest.split("\\n", -1))
                .filter(line -> line.startsWith(M3NumberSpace.SOURCE_ID + "\t"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("number manifest row missing"));
        String[] columns = row.split("\\t", -1);
        check(columns.length == 9);
        check(columns[0].equals(M3NumberSpace.SOURCE_ID));
        check(columns[3].equals(M3NumberSpace.SOURCE_REVISION));
        check(columns[8].equals(
                "source_id,record_id,min_value,max_value,precomputed_value_count,"
                        + "shared_utf16_storage,canonical_decimal_spelling,source_revision"));
        String targetMap = Files.readString(
                Path.of("lexicon/synexia-number-target-map.tsv"), StandardCharsets.UTF_8);
        String[] targetLines = targetMap.split("\\n", -1);
        String[] targetHeader = targetLines[0].split("\\t", -1);
        check(targetHeader.length == 11 && targetHeader[0].equals("schema")
                && targetHeader[10].equals("source_revision"));
        String targetRow = Arrays.stream(targetLines)
                .filter(line -> line.startsWith("M3JDK_NUMBER_TARGET_MAP_V1\t"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("number target map row missing"));
        String[] targetColumns = targetRow.split("\\t", -1);
        check(targetColumns.length == 11);
        check(targetColumns[1].equals(M3NumberSpace.SOURCE_ID));
        check(targetColumns[2].equals(M3NumberSpace.RECORD_ID));
        check(targetColumns[6].contains("com.m3.text.M3NumberSpace"));
        check(targetColumns[9].equals("ADMITTED_TYPED_RECEIVER"));
        check(targetColumns[10].equals(M3NumberSpace.SOURCE_REVISION));
        System.out.println("M3JDK_NUMBER_CONTRACT_PASS checks=" + checks + " fields=9");
    }

    private static String expectNumberFormat(M3NumberSpace space, CharSequence spelling) {
        try {
            space.parse(spelling);
            throw new AssertionError("invalid number spelling was accepted: " + spelling);
        } catch (NumberFormatException expected) {
            return expected.getMessage();
        }
    }

    private static final class View implements CharSequence {
        private final String value;

        View(String value) {
            this.value = value;
        }

        @Override
        public int length() {
            return value.length();
        }

        @Override
        public char charAt(int index) {
            return value.charAt(index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return value.subSequence(start, end);
        }

        @Override
        public String toString() {
            throw new AssertionError("number parser materialized view");
        }
    }
}
