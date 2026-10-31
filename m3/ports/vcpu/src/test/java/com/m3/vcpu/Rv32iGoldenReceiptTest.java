/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.vcpu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.m3.parallel.Backend;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

/**
 * Receipts identical to the cpu-gpu oracle. The golden values below were produced by the unmodified donor classes
 * ({@code CpuRv32iEngine}, {@code Rv32iBulkExecutor.executeCpu}, {@code Rv32iBulkReceipt} at cpu-gpu@3bf82671,
 * compiled without the TornadoVM-only {@code executeVerifiedTornado}) running this package's {@link Rv32iCorpus}
 * source and the donor's sum-1..100 test batch; see PROVENANCE.md.
 */
final class Rv32iGoldenReceiptTest {
    private static final String INPUT_256 = "6fea414db63c77cdb4dd27b1b8cc326414d0cfcb9d31a59fc2dcb243e8892d65";
    private static final String[][] SLICES_256 = {
        {"8d0be84fc2093cd124f821bca726d7be516e750b4ef8eea13d80958fcbb6c6aa",
            "46e7f0e044640b09c489a90523a7a3cf11d188b678d7a547727047e47c9e6c5c"},
        {"b5980841eb68f5faf0306f3985f6d405058705fb9431cf0238335fe071ecaa2c",
            "b51d340e386b0169155722a5e613cc65c4d8624aea86b13dda7f262acf829c6b"},
        {"207039caeda5e9315dba9bc878250f933d81fafaa681131dec6011c51dcce268",
            "6ca70d693e2eab27889d392590090573282dedbf35bd9cae7cab0a8227e47569"},
        {"26ee9298f2df8de68e707a25274f186f11e968afe68e2ff4b9c730dffa5b9a7a",
            "d39778bb0fadea43ea71d09ed7c922e8096dc91fe4bd3e309d72a924ad7c4eb7"},
        {"12c03aa281f78ce368f656ee8d6b683b8c1b48eb025fe2e8404c283fba500a3f",
            "55770835c51f64b2817f249853a42b664641adb6898a80860aad22ac8a9cc8f9"},
        {"981debebf5737122e2094dde6dbdd67688f01028696759c92167e285f995990c",
            "1cdb0fd81c222693993e57d7f02a7be088ed11a7c9368e409ad03a765f7121a6"},
        {"981debebf5737122e2094dde6dbdd67688f01028696759c92167e285f995990c",
            "bd9ae149e1f8310d276cf835290f608a1714e4ef00a8f31efd57d212478769fa"},
    };
    private static final String INPUT_300 = "631d571b6c9b15e36d184c3111dabcd30f706de29b507cf61fe361a56633e89a";
    private static final String[][] SLICES_300 = {
        {"216dea63bcd5813a7190ff72b6ba777d0342c154eab996b486f133a8c74320bf",
            "aba833925fdd026d71cde0cc93bfe8206c3d075c71cdd26cda1ffa5ebc427bcf"},
        {"5ecac2ce92249ea385312fa59fd847530224790e9f0bde9ae50c43c46a535b3a",
            "34996efd275ed59a74d96525956bcdd7d41a8d01b60fcc58a395e371dd38fa8a"},
        {"8747469e8046c97fd00f3c09c6fbcf0fa7cbbd02a6961a790b429cf652bf323e",
            "dd1c32e144884c5184906cc68baf289d88d1c333f8de7eea0c5df657b8c6b458"},
        {"36fbcb6f8c504772ac3f4a6e073eef8706c87c625214c52f1bb70ebbfe1c5e67",
            "585667fa0e59eeb6d61fd88e3c300e5ad3a00b479514944f40d7ff3fd1754244"},
        {"5b763fdc673d1afdd82f4185ad27c102bf498195a65877eb36e425872427460d",
            "733436ac665eeecfd4f41e3231a31aebda9bd666ecc7188b6bc9c6ea9f26e4e4"},
        {"7ebf7085c8da5f3dbc600fc162003cb4551cc5ef8c3a4ddfccc88d2a20adff45",
            "4a9b2e9786d0e54020bf33ddb567a3d36e2b223e7c836ccd350a9f538488739e"},
        {"7ebf7085c8da5f3dbc600fc162003cb4551cc5ef8c3a4ddfccc88d2a20adff45",
            "fe8b302281ce032e00d7080ef875d665b17aff581348082ff209768bd3427fe1"},
    };
    private static final String SUM64_INPUT = "c07428208972a185e1533b7fa2dcc539da88fd0a081fe24fb4cd1999b5d41eb4";
    private static final String SUM64_OUTPUT = "3933fb28e1a7d5be08d561aa6441d59c6d37c57ae0110108fa7e47c504ca63de";
    private static final String SUM64_VERIFICATION =
            "7ec284c9d9d6d09cbcab40c0430069187f61ef655f4a262f214fd71ec0327754";
    private static final String SUM64_ROOT = "18224476339eaaffab7adce8ebf279174d2f3820dd0f67e93c79ca189efa5c65";

    @ParameterizedTest(name = "cores={0}")
    @ValueSource(ints = {256, 300})
    void cpuReceiptsAreIdenticalToTheDonorOracle(int cores) {
        Rv32iBatch batch = Rv32iCorpus.batch(cores, Rv32iCorpus.SEED);
        assertEquals(input(cores), Rv32iBulkExecutor.stateRoot(batch));
        Rv32iBulkExecutor executor = new Rv32iBulkExecutor();
        for (int slice = 0; slice < Rv32iCorpus.SLICE_BUDGETS.length; slice++) {
            Rv32iBulkReceipt receipt = executor.executeCpu(batch, Rv32iCorpus.SLICE_BUDGETS[slice]);
            assertEquals(slices(cores)[slice][0], receipt.outputRoot(), "output root, slice " + slice);
            assertEquals(slices(cores)[slice][1], receipt.root(), "receipt root, slice " + slice);
        }
    }

    @ParameterizedTest(name = "cores={0}")
    @ValueSource(ints = {256, 300})
    void parallelBackendReachesTheDonorStateRootsEverySlice(int cores) {
        Rv32iBatch batch = Rv32iCorpus.batch(cores, Rv32iCorpus.SEED);
        try (Rv32iSession session = new Rv32iSession(batch, Backend.reference(), 64)) {
            for (int slice = 0; slice < Rv32iCorpus.SLICE_BUDGETS.length; slice++) {
                session.runSlice(Rv32iCorpus.SLICE_BUDGETS[slice]);
                session.synchronizeAll();
                assertEquals(slices(cores)[slice][0], Rv32iBulkExecutor.stateRoot(batch), "slice " + slice);
            }
        }
    }

    @Test
    void sumProgramReceiptIsIdenticalToTheDonor() {
        Rv32iBulkReceipt receipt = new Rv32iBulkExecutor().executeCpu(Rv32iPrograms.newBatch(64), 1024);
        assertEquals(SUM64_INPUT, receipt.inputRoot());
        assertEquals(SUM64_OUTPUT, receipt.outputRoot());
        assertEquals(SUM64_VERIFICATION, receipt.verificationRoot());
        assertEquals(SUM64_ROOT, receipt.root());
    }

    private static String input(int cores) {
        return cores == 256 ? INPUT_256 : INPUT_300;
    }

    private static String[][] slices(int cores) {
        return cores == 256 ? SLICES_256 : SLICES_300;
    }
}
