/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import com.m3.text.M3NumberSpace;

/**
 * Emits the M3NumberSpace owner values used by the offline lexicon-export proof.
 *
 * <p>The fixture deliberately reads identities, spellings, bounds, and storage
 * identity from the compiled Java owner. It does not reimplement the number
 * range in Python.</p>
 */
public final class M3NumberSpaceExportFixture {
    private static final String HEADER =
            "source_id\tsource_revision\tmin_value\tmax_value\tprecomputed_value_count"
                    + "\trecord_id\tcanonical_decimal_spelling\tshared_utf16_storage\n";

    private M3NumberSpaceExportFixture() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("usage: M3NumberSpaceExportFixture <output.tsv>");
        }
        M3NumberSpace space = M3NumberSpace.INSTANCE;
        Object sharedStorage = space.number(M3NumberSpace.MIN_VALUE).storageIdentity();
        Path output = Path.of(args[0]);
        try (BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            writer.write(HEADER);
            for (int value = M3NumberSpace.MIN_VALUE; value <= M3NumberSpace.MAX_VALUE; value++) {
                String recordId = space.sourceRecordId(value);
                String spelling = space.spelling(value);
                boolean sharesStorage = sharedStorage.equals(space.number(value).storageIdentity());
                if (!recordId.equals(Integer.toString(value))
                        || !spelling.equals(recordId)
                        || !sharesStorage) {
                    throw new AssertionError("M3NumberSpace projection drift at " + value);
                }
                writer.write(M3NumberSpace.SOURCE_ID);
                writer.write('\t');
                writer.write(M3NumberSpace.SOURCE_REVISION);
                writer.write('\t');
                writer.write(Integer.toString(M3NumberSpace.MIN_VALUE));
                writer.write('\t');
                writer.write(Integer.toString(M3NumberSpace.MAX_VALUE));
                writer.write('\t');
                writer.write(Integer.toString(space.precomputedValueCount()));
                writer.write('\t');
                writer.write(recordId);
                writer.write('\t');
                writer.write(spelling);
                writer.write('\t');
                writer.write(Boolean.toString(sharesStorage));
                writer.write('\n');
            }
        }
        System.out.println("M3_NUMBER_SPACE_EXPORT_FIXTURE_PASS records="
                + space.precomputedValueCount());
    }
}
