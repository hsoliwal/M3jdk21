/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
public final class CircularFifoNullConstructorProbe {
    private CircularFifoNullConstructorProbe() { }
    public static void main(String[] args) throws Exception {
        for(String name:args) {
            try {Class.forName(name).getConstructor(Collection.class).newInstance(new Object[]{null});}
            catch(InvocationTargetException failure) {
                System.out.println(name+"|"+failure.getCause().getClass().getName()+"|"+failure.getCause().getMessage());
            }
        }
    }
}
