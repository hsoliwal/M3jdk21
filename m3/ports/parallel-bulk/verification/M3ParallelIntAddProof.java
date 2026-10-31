/* SPDX-License-Identifier: Apache-2.0 */
import com.m3.parallel.bulk.M3ParallelIntAddProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import jdk.internal.vm.parallel.BulkExecution;
import jdk.internal.vm.parallel.BulkScheduler;
import jdk.internal.vm.parallel.BulkTask;

public final class M3ParallelIntAddProof {
    private static int checks;
    private static void check(boolean value) {
        checks++;
        if (!value) throw new AssertionError("check " + checks);
    }
    private static void reject(Class<? extends Throwable> type, Runnable action) {
        try { action.run(); } catch (Throwable error) {
            check(type.isInstance(error)); return;
        }
        throw new AssertionError("missing " + type.getName());
    }
    private static BulkTask task(int size) {
        return new BulkTask(M3ParallelIntAddProvider.OPERATION_ID, size, 0,
                List.of("left", "right"), List.of("sum"));
    }
    private static void evaluate(M3ParallelIntAddProvider provider, int[] left, int[] right) {
        int[] l = left.clone(), r = right.clone();
        BulkExecution result = provider.execute(task(l.length), Map.of("left", left, "right", right));
        check(result.provider().equals("m3-parallel-java"));
        check(result.receiptRoot().matches("[0-9a-f]{64}"));
        check(Arrays.equals(left, l));
        check(Arrays.equals(right, r));
        int[] sum = (int[]) result.outputs().get("sum");
        check(sum.length == l.length);
        for (int i=0;i<l.length;i++) check(sum[i] == l[i] + r[i]);
        check(sum != left && sum != right);
    }
    public static void main(String[] args) throws Exception {
        M3ParallelIntAddProvider provider = M3ParallelIntAddProvider.reference();
        BulkScheduler scheduler = new BulkScheduler(List.of(provider));
        check(scheduler.providerIds().equals(List.of("m3-parallel-java")));
        check(scheduler.select(task(1)) == provider);
        reject(IllegalArgumentException.class, () -> provider.execute(task(1), Map.of("left", new int[1])));
        reject(IllegalArgumentException.class, () -> provider.execute(task(2), Map.of("left", new int[1], "right", new int[2])));
        reject(IllegalArgumentException.class, () -> provider.execute(new BulkTask("other", 1, 0, List.of("left","right"), List.of("sum")),
                Map.of("left", new int[1], "right", new int[1])));
        reject(IllegalArgumentException.class, () -> provider.execute(new BulkTask(M3ParallelIntAddProvider.OPERATION_ID, 2, 1, List.of("left","right"), List.of("sum")),
                Map.of("left", new int[2], "right", new int[2])));
        reject(IllegalArgumentException.class, () -> provider.execute(
                new BulkTask(M3ParallelIntAddProvider.OPERATION_ID, M3ParallelIntAddProvider.MAX_ITEMS + 1, 0,
                    List.of("left","right"), List.of("sum")), Map.of("left", new int[1], "right", new int[1])));

        Random random = new Random(0x4d33504152414c4cL);
        for (int pass=0; pass<180; pass++) {
            int n = 1 + random.nextInt(512);
            int[] a = new int[n], b = new int[n];
            for (int i=0;i<n;i++) { a[i]=random.nextInt(); b[i]=random.nextInt(); }
            evaluate(provider,a,b);
        }
        try(var pool = Executors.newFixedThreadPool(8)) {
            ArrayList<Callable<Void>> tasks=new ArrayList<>();
            for(int i=0;i<32;i++) {
                final int num=i;
                tasks.add(()->{
                    int[] a=new int[32], b=new int[32];
                    Arrays.fill(a,num); Arrays.fill(b,Integer.MAX_VALUE);
                    BulkExecution result=scheduler.execute(task(32),Map.of("left",a,"right",b));
                    int[] sums=(int[])result.outputs().get("sum");
                    for(int v:sums) if(v!=num+Integer.MAX_VALUE)throw new AssertionError("concurrent mismatch");
                    return null;
                });
            }
            for(var f:pool.invokeAll(tasks)) f.get();
        }
        System.out.println("PASS checks="+checks);
    }
}
