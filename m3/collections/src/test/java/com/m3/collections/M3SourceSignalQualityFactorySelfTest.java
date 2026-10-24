/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Original javac SHA-256 source image → exact typed scalar quality/epoch contract. */
public final class M3SourceSignalQualityFactorySelfTest {
    private static long checks;
    private M3SourceSignalQualityFactorySelfTest() { }
    private static void check(boolean value, String reason) {
        checks++;
        if (!value) throw new AssertionError(reason);
    }
    private static void refuses(Class<? extends Throwable> kind, Runnable action) {
        checks++;
        try { action.run(); }
        catch (Throwable error) {
            if (kind.isInstance(error)) return;
            throw new AssertionError("wrong exception: " + error, error);
        }
        throw new AssertionError("missing " + kind.getSimpleName());
    }
    private static String sha(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException error) {
            throw new AssertionError(error);
        }
    }
    private static String imageText() {
        var text=new StringBuilder("# M3JDK21-SOURCE-SIGNALS/1\n");
        for(String name:new String[]{"catalog.Alpha","catalog.Beta"}) {
            text.append("S\t").append(name);
            for(int i=0;i<15;i++) text.append('\t').append(i+1);
            text.append('\n');
        }
        String source=text.toString();
        return source + "# sha256\t" + sha(source) + "\n";
    }
    public static void main(String[] args) {
        String image=imageText();
        var verified=M3SourceSignalImage.parse(image);
        check(verified.rowCount()==2,"two sorted observed source types");
        check(verified.columns().columnCount()==M3SourceSignalImage.Scalar.values().length,
                "original static scalar column count");
        var epochs=M3SignalDependencyEpochs.from(verified.relationships(),true);
        var quality=M3SourceSignalQualityFactory.fromVerifiedCompilerImage(verified,epochs,11);
        check(quality.dimensionCount()==17,"17 original compiler columns");
        check(quality.sourceAuthority()==
                M3SignalQualitySchema.SourceAuthority.COMPILER_IMAGE_SHA256_VERIFIED,
                "SHA-verified image not caller digest");
        check(quality.sourceSha256().equals(verified.sourceSha256()),"exact original SHA");
        for(int i=0;i<M3SourceSignalImage.Scalar.values().length;i++) {
            var scalar=M3SourceSignalImage.Scalar.values()[i];
            var descriptor=quality.descriptor(i);
            check(descriptor.name().equals("javac."+scalar.name().toLowerCase(java.util.Locale.ROOT)),
                    "stable dimension name");
            check(descriptor.phase()==M3SignalQualitySchema.Phase.COMPILER,"compiler phase");
            check(descriptor.reliability()==M3SignalQualitySchema.Reliability.EXACT_FACT,
                    "source-observation exactness only");
            check(descriptor.confidence()!=descriptor.confidence(),"no guessed confidence");
            check(quality.validAt(i,0),"original primitive source validity");
            check(quality.origin(i)==M3SignalColumns.Origin.STATIC,"source origin");
        }
        check(quality.descriptor(4).family()==M3SignalQualitySchema.Family.CALL_GRAPH,
                "recorded compiler call expression count");
        check(quality.descriptor(10).family()==M3SignalQualitySchema.Family.CONTROL_FLOW,
                "recorded compiler loop count");
        check(quality.descriptor(15).family()==M3SignalQualitySchema.Family.GRAPH,
                "recorded known-source incoming sites");
        var ticket=quality.capture(new int[]{0,1});
        check(quality.unchanged(ticket,11,verified.sourceSha256()),"fresh under original SHA");
        epochs.invalidateAll();
        check(!quality.unchanged(ticket,11,verified.sourceSha256()),"conservative unknown dependency");
        check(!quality.unchanged(quality.capture(new int[]{0}),12,verified.sourceSha256()),
                "changed source version");
        String tampered=image.replace("catalog.Beta","catalog.Zeta");
        refuses(IllegalArgumentException.class, () -> M3SourceSignalImage.parse(tampered));
        refuses(IllegalArgumentException.class, () ->
                M3SourceSignalQualityFactory.fromVerifiedCompilerImage(verified,
                        M3SignalDependencyEpochs.from(M3SignalRelations.of(1,
                                new int[0],new int[0],new byte[0],new int[0]),true),11));
        refuses(IllegalArgumentException.class, () ->
                M3SourceSignalQualityFactory.fromVerifiedCompilerImage(verified,epochs,-1));
        System.out.println("M3_COMPILER_SIGNAL_QUALITY_PASS checks="+checks);
    }
}
