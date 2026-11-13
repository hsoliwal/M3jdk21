import java.lang.management.ManagementFactory;
public class OmProbe {
    public static void main(String[] a) throws Exception {
        var os = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        Object[] objs = { new Object(), "x", new int[10], new long[3], new StringBuilder(), new Object[7] };
        StringBuilder sizes = new StringBuilder();
        for (Object o : objs) sizes.append(jdk.internal.m3.M3ObjectMetrics.shallowSize(o)).append(',');
        long before = os.getCommittedVirtualMemorySize();
        long sum = 0;
        for (int i = 0; i < Integer.parseInt(a[0]); i++) sum += jdk.internal.m3.M3ObjectMetrics.shallowSize(objs[i % objs.length]);
        long after = os.getCommittedVirtualMemorySize();
        System.out.println("sizes=" + sizes + " sum=" + sum + " committedGrowthMB=" + (after - before) / (1 << 20));
    }
}
