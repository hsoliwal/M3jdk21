/* SPDX-License-Identifier: Apache-2.0 */
import java.io.PrintWriter;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.TagFilter;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
public final class ClosureJUnit {
    private ClosureJUnit() { }
    public static void main(String[] args) {
        if(args.length!=2)throw new IllegalArgumentException("tag expected-test-count");
        var builder=LauncherDiscoveryRequestBuilder.request().selectors(DiscoverySelectors.selectClass("com.m3.collections.interop.M3ApacheContractTest"));
        if(!args[0].equals("all"))builder.filters(TagFilter.includeTags(args[0]));
        var listener=new SummaryGeneratingListener();var launcher=LauncherFactory.create();launcher.registerTestExecutionListeners(listener);launcher.execute(builder.build());
        var s=listener.getSummary();s.printTo(new PrintWriter(System.out,true));s.printFailuresTo(new PrintWriter(System.out,true));long expected=Long.parseLong(args[1]);
        if(s.getTestsFoundCount()!=expected||s.getTestsSucceededCount()!=expected||s.getTestsFailedCount()!=0||s.getTestsSkippedCount()!=0||s.getTestsAbortedCount()!=0)throw new AssertionError("Exact test inventory failed: "+args[0]+"="+expected);
        System.out.println("CLOSURE_JUNIT tag="+args[0]+" tests="+expected+" PASS");
    }
}
