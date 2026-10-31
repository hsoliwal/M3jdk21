// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.jmod;

/** Standalone strict-Java21 negative, ambiguity and refusal checks. */
public final class CpuToolReceiverAdmissionChecks {
    private CpuToolReceiverAdmissionChecks() { }
    private static int count;
    private static void yes(boolean value) { count++; if (!value) throw new AssertionError("check " + count); }
    private static void refuses(String a, String b, String c) {
        count++;
        try { CpuToolReceiverAdmission.verify(a, b, c); }
        catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("expected refusal " + count);
    }
    public static void main(String[] args) {
        String dh = "module\tmain_class\tfamily\tstatus\tlicense\n";
        String mh = "source_module\tcandidate_target_module\tintake_mode\tphase\tstatus\n";
        String hh = "source_module\tdisposition\tproblem_category\trationale\n";
        String cpu = "com.synexia.m3.text.regex\tcom.synexia.m3.text.regex.RegexMatcher\tTEXT\tIMPLEMENTED\tApache-2.0\n";
        String mapped = "com.synexia.m3.text.regex\tcom.m3.tools.text.regex\tINDEPENDENT_APACHE_MODULE\tOPTIONAL_TOOLING\tQUALIFICATION_PENDING\n";
        String gpu = "com.synexia.m3.compute.webgpu\tcom.synexia.m3.compute.webgpu.WebGpuCompute\tWEBGPU\tIMPLEMENTED\tApache-2.0\n";
        String hold = "com.synexia.m3.compute.webgpu\tSEPARATE_QUALIFICATION_PENDING\tOPTIONAL_GPU_NATIVE\tNative proof pending\n";
        var receipt = CpuToolReceiverAdmission.verify(dh + cpu + gpu, mh + mapped, hh + hold);
        yes(receipt.donorCount() == 2 && receipt.mappedCount() == 1 && receipt.deferredCount() == 1);
        yes(receipt.deferredModules().equals(java.util.List.of("com.synexia.m3.compute.webgpu")));
        // The new CPU-only int8 receiver is an actual independent candidate module.
        // It does not weaken the mandatory GPU deferral or admit a changed target name.
        String quant = "com.synexia.m3.model.quantize\t"
                + "com.synexia.m3.model.quantize.BlockInt8Quantizer\t"
                + "MODEL_QUANTIZE\tIMPLEMENTED\tApache-2.0\n";
        String mappedQuant = "com.synexia.m3.model.quantize\t"
                + "com.m3.tools.model.quantize\tINDEPENDENT_APACHE_MODULE\t"
                + "OPTIONAL_TOOLING\tQUALIFICATION_PENDING\n";
        var quantReceipt = CpuToolReceiverAdmission.verify(
                dh + cpu + gpu + quant, mh + mapped + mappedQuant, hh + hold);
        yes(quantReceipt.donorCount() == 3 && quantReceipt.mappedCount() == 2
                && quantReceipt.deferredCount() == 1);
        yes(quantReceipt.deferredModules().equals(java.util.List.of(
                "com.synexia.m3.compute.webgpu")));
        refuses(dh + cpu + gpu + quant, mh + mapped, hh + hold);
        refuses(dh + cpu + gpu + quant, mh + mapped
                + mappedQuant.replace("com.m3.tools.model.quantize",
                        "com.m3.tools.text.regex"), hh + hold);
        refuses(dh + cpu + gpu + quant, mh + mapped
                + mappedQuant.replace("QUALIFICATION_PENDING", "PASSED"), hh + hold);

        refuses(dh + cpu + gpu, mh + mapped, hh);
        refuses(dh + cpu + gpu, mh + mapped + mapped, hh + hold);
        refuses(dh + cpu + gpu, mh + mapped.replace("com.m3.tools", "java"), hh + hold);
        refuses(dh + cpu + gpu, mh + mapped, hh + hold.replace("OPTIONAL_GPU_NATIVE", "CPU_MODEL_MATH"));
        refuses(dh + cpu, mh + mapped, hh + hold);
        refuses(dh + cpu, mh + mapped.replace("QUALIFICATION_PENDING", "PASSED"), hh);
        refuses(dh + cpu.replace("Apache-2.0", "UNKNOWN"), mh + mapped, hh);
        refuses(dh + cpu, mh + mapped, hh + "bad");
        refuses(dh + cpu, mh + mapped, hh + "com.synexia.m3.text.foo\tSEPARATE_QUALIFICATION_PENDING\tCPU_MODEL_MATH\twrong\n");
        refuses(dh + cpu, mh + mapped, hh + "com.synexia.m3.text.regex\tSEPARATE_QUALIFICATION_PENDING\tCPU_MODEL_MATH\twrong\n");
        System.out.println("CPU_TOOL_RECEIVER_CHECKS_PASS checks=" + count);
    }
}
