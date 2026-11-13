// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
import jdk.m3.collections.spring.M3MultiValueMap;
import jdk.m3.collections.spring.M3SingleValueMapView;

/**
 * Merge-loss leaf 59 binary probe: compiled against the restored owner, this client links the published
 * {@code (Ljdk/m3/collections/spring/M3MultiValueMap;)V} constructor. Run against the master owner it fails
 * with NoSuchMethodError; against the restored owner the delegate keeps the Map constructor's null rejection.
 * With the argument {@code live} it also builds a view over a real M3MultiValueMap (needs the m3_arrays JNI library).
 */
public final class SpringCtorAbiProbe {
    private SpringCtorAbiProbe() { }

    public static void main(String[] args) {
        //FLLDCIM: MRL - merge-loss leaf 59 probe: the published constructor descriptor links and validates
        try {
            new M3SingleValueMapView<String, String>((M3MultiValueMap<String, String>) null);
            throw new AssertionError("null target accepted");
        } catch (IllegalArgumentException expected) {
            System.out.println("published ctor linked; null -> IllegalArgumentException: " + expected.getMessage());
        }
        if (args.length > 0 && "live".equals(args[0])) {
            M3MultiValueMap<String, String> source = new M3MultiValueMap<>();
            source.add("k", "v1");
            source.add("k", "v2");
            M3SingleValueMapView<String, String> view = new M3SingleValueMapView<>(source);
            if (!"v1".equals(view.get("k")) || view.size() != 1) throw new AssertionError("live view " + view);
            view.put("j", "w");
            if (!source.get("j").equals(java.util.List.of("w"))) throw new AssertionError("write-through " + source);
            System.out.println("live view over published ctor: " + view);
        }
        System.out.println("PASS SpringCtorAbiProbe");
    }
}
