// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
import com.m3.collections.M3IndexMaskCompactor;
import com.m3.collections.M3IndexPathMasks;
import com.m3.collections.M3LongAddressKernel;
import com.m3.collections.M3LongRankBatchNative;
import com.m3.collections.M3SignalOrderNative;
import com.m3.collections.M3SpanIndex;
import com.m3.collections.M3StateMaskHistogram;
import com.m3.collections.M3ResidencyStateMaskIndex;
import java.lang.reflect.Method;

/** Test-only feature/ABI witness over existing production JNI owners. */
public final class NativeCapabilityProbe {
    private NativeCapabilityProbe() { }
    public static void main(String[] args) throws ReflectiveOperationException {
        boolean[] present = {M3IndexMaskCompactor.nativeAvailable(),
            M3IndexPathMasks.nativeAvailable(), M3LongAddressKernel.nativeAvailable(),
            M3LongRankBatchNative.nativeAvailable(), M3SignalOrderNative.available(),
            M3SpanIndex.nativeAvailable(), M3StateMaskHistogram.nativeAvailable(),
            M3ResidencyStateMaskIndex.nativeAvailable()};
        for (int i=0; i<present.length; i++) {
            if (!present[i]) throw new AssertionError("missing native owner " + i);
        }
        for (String name : new String[] {"rangeAbiNative", "positionsAbiNative"}) {
            Method abi = M3IndexMaskCompactor.class.getDeclaredMethod(name);
            abi.setAccessible(true);
            Object version = abi.invoke(null);
            if (!Integer.valueOf(1).equals(version)) throw new AssertionError(name+"="+version);
            System.out.println(name + "=" + version);
        }
        System.out.println("PASS native owner availability=8, actual ABI version checks=2");
    }
}
