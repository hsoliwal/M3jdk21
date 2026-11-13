import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
/** CloseProbe: close() of a thread-per-task executor must wait for a task whose future was cancelled while running. */
public final class CloseProbe {
    public static void main(String[] a) throws Exception {
        int early = 0, rounds = Integer.parseInt(a[0]);
        for (int i = 0; i < rounds; i++) {
            AtomicBoolean started = new AtomicBoolean(), finished = new AtomicBoolean();
            ExecutorService ex = Executors.newThreadPerTaskExecutor(Thread.ofPlatform().factory());
            Future<?> f = ex.submit(() -> {
                started.set(true);
                long end = System.nanoTime() + 30_000_000L;   // 30 ms of interrupt-ignoring work
                while (System.nanoTime() < end) { Thread.interrupted(); }
                finished.set(true);
            });
            while (!started.get()) Thread.onSpinWait();
            f.cancel(true);
            ex.close();
            if (!finished.get()) early++;
        }
        System.out.println("CLOSE_AFTER_CANCEL rounds=" + rounds + " close_returned_before_task_finished=" + early);
    }
}
