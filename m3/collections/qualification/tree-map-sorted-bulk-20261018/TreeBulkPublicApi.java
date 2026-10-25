/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
import java.lang.reflect.*;
import java.util.*;
public final class TreeBulkPublicApi {
    private static String types(Type[] types) {
        return Arrays.toString(Arrays.stream(types).map(Type::getTypeName).toArray(String[]::new));
    }
    public static void main(String[] args) throws Exception {
        Class<?> owner=Class.forName("com.m3.collections.M3TreeMap",false,TreeBulkPublicApi.class.getClassLoader());
        TreeSet<String> rows=new TreeSet<>();
        for(Method m:owner.getMethods()) {
            rows.add("M "+Modifier.toString(m.getModifiers())+" "+m.getName()+" "+m.getGenericReturnType().getTypeName()
                +" "+types(m.getGenericParameterTypes())+" "+types(m.getGenericExceptionTypes())
                +" "+Arrays.toString(m.getTypeParameters())+" bridge="+m.isBridge()+" default="+m.isDefault()
                +" annotations="+Arrays.toString(m.getDeclaredAnnotations()));
        }
        for(Constructor<?> c:owner.getConstructors()) {
            rows.add("C "+Modifier.toString(c.getModifiers())+" "+types(c.getGenericParameterTypes())
                +" "+types(c.getGenericExceptionTypes())+" "+Arrays.toString(c.getTypeParameters())
                +" annotations="+Arrays.toString(c.getDeclaredAnnotations()));
        }
        for(String row:rows)System.out.println(row);
    }
}

