/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0 */
public class SegmentedGates {
    public static void main(String[] args)throws Exception{
        if(!System.getProperty("java.vm.info").contains("interpreted mode"))throw new AssertionError("compiler mode");
        if(System.getProperty("java.vm.info").contains("sharing"))throw new AssertionError("CDS sharing remains active");
        boolean blocked=false;
        try(var recording=new jdk.jfr.Recording()){recording.start();}
        catch(IllegalStateException|UnsupportedOperationException expected){blocked=true;}
        if(!blocked)throw new AssertionError("dynamic JFR unexpectedly available");
        System.out.println("SEGMENTED_GATES_PASS pid="+ProcessHandle.current().pid());
        if(args.length!=0)System.in.read();
    }
}
