/* SPDX-License-Identifier: Apache-2.0 */
import java.io.PrintWriter;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
public final class RuntimeJUnit {
    private RuntimeJUnit() { }
    public static void main(String[] args) {
        var request=LauncherDiscoveryRequestBuilder.request().selectors(DiscoverySelectors.selectPackage("com.m3.collections.interop")).build();
        var listener=new SummaryGeneratingListener();var launcher=LauncherFactory.create();launcher.registerTestExecutionListeners(listener, new org.junit.platform.launcher.TestExecutionListener(){
            @Override public void executionStarted(org.junit.platform.launcher.TestIdentifier t){if(t.isTest())System.out.println("START "+t.getDisplayName());}
            @Override public void executionFinished(org.junit.platform.launcher.TestIdentifier t,org.junit.platform.engine.TestExecutionResult r){if(t.isTest())System.out.println("END "+t.getDisplayName()+" "+r.getStatus());}
        });launcher.execute(request);
        var s=listener.getSummary();s.printTo(new PrintWriter(System.out,true));s.printFailuresTo(new PrintWriter(System.out,true));
        if(s.getTestsFoundCount()!=43||s.getTestsSucceededCount()!=43||s.getTestsFailedCount()!=0||s.getTestsSkippedCount()!=0||s.getTestsAbortedCount()!=0)throw new AssertionError("expected all 43 runtime tests");
        System.out.println("PASS runtime JUnit tests=43 failures=0 errors=0 skipped=0");
    }
}
