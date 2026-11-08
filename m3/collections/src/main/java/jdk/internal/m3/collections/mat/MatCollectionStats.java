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
package jdk.internal.m3.collections.mat;
/**
 * <p>Lineage: absorbed from Synexia {@code m3-mat-collections/src/main/java/jdk/internal/m3/matcollections/MatCollectionStats.java}
 * blob {@code 37f0558b5910a1b6fdd196a5ef69a10f61b20455} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public record MatCollectionStats(int size, int distinct, int min, int max, int runs) {
    public static MatCollectionStats of(int[] values) {
        if (values == null) throw new NullPointerException("values");
        if (values.length == 0) return new MatCollectionStats(0,0,0,0,0);
        int[] sorted=values.clone(); java.util.Arrays.sort(sorted);
        int distinct=1,runs=1,previous=sorted[0];
        for(int i=1;i<sorted.length;i++){
            int current=sorted[i];
            if(current!=previous){
                distinct++;
                if((long)current!=(long)previous+1L) runs++;
                previous=current;
            }
        }
        return new MatCollectionStats(values.length,distinct,sorted[0],sorted[sorted.length-1],runs);
    }
    public long span(){ return distinct==0?0L:(long)max-min+1L; }
}
