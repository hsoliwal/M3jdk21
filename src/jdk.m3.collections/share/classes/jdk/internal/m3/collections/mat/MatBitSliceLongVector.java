/* *****************************************************************************
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

import java.util.Objects;

/**
 * Unsigned bit-sliced values. Plane p stores bit p for up to 64 rows per machine word.
 * Equality/comparison/addition operate over rows word-wise.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-value/src/main/java/jdk/internal/m3/matvalue/MatBitSliceLongVector.java}
 * blob {@code 756b5d82a0dab21c73790e9c8f2df04855e6f5e7} at com.synexia develop {@code 35474ccf} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class MatBitSliceLongVector {
    public record Addition(MatBitSliceLongVector values, MatBits overflow) { }

    private final int size;
    private final int width;
    private final int words;
    private final long[] planes;

    public MatBitSliceLongVector(int size,int width){
        if(size<0) throw new IllegalArgumentException("negative size");
        if(width<1 || width>64) throw new IllegalArgumentException("width must be 1..64");
        this.size=size;
        this.width=width;
        this.words=(size+63)>>>6;
        this.planes=new long[Math.multiplyExact(words,width)];
    }

    public int size(){ return size; }
    public int width(){ return width; }
    public long payloadBytes(){ return 8L*planes.length; }

    public void set(int row,long value){
        Objects.checkIndex(row,size);
        if(width<64 && (value<0 || (value & ~MatPackedLongVector.mask(width))!=0))
            throw new IllegalArgumentException("unsigned value exceeds width");
        long bit=1L<<(row&63);
        int word=row>>>6;
        for(int p=0;p<width;p++){
            int index=p*words+word;
            if(((value>>>p)&1L)!=0) planes[index]|=bit; else planes[index]&=~bit;
        }
    }

    public long get(int row){
        Objects.checkIndex(row,size);
        int word=row>>>6;
        int shift=row&63;
        long value=0;
        for(int p=0;p<width;p++) value|=((planes[p*words+word]>>>shift)&1L)<<p;
        return value;
    }

    public MatBits equalTo(long value){
        check(value);
        long[] result=new long[words];
        for(int w=0;w<words;w++){
            long equal=valid(w);
            for(int p=0;p<width;p++){
                long plane=planes[p*words+w];
                equal &= ((value>>>p)&1L)==0 ? ~plane : plane;
            }
            result[w]=equal & valid(w);
        }
        return MatBits.ofWords(size,result);
    }

    public MatBits unsignedGreaterOrEqual(long value){
        check(value);
        long[] result=new long[words];
        for(int w=0;w<words;w++){
            long greater=0;
            long equal=valid(w);
            for(int p=width-1;p>=0;p--){
                long plane=planes[p*words+w];
                if(((value>>>p)&1L)==0){
                    greater |= equal & plane;
                    equal &= ~plane;
                }else{
                    equal &= plane;
                }
            }
            result[w]=(greater|equal)&valid(w);
        }
        return MatBits.ofWords(size,result);
    }

    /**
     * Treats the bit-sliced value as a flag word and evaluates predicates 64 rows at a time.
     */
    public MatBits matchFlags(long requireAll,long requireAny,long rejectAny){
        long referenced=requireAll|requireAny|rejectAny;
        check(referenced);
        long[] result=new long[words];
        for(int w=0;w<words;w++){
            long selected=valid(w);

            long bits=requireAll;
            while(bits!=0L){
                int p=Long.numberOfTrailingZeros(bits);
                selected &= planes[p*words+w];
                bits &= bits-1L;
            }

            if(requireAny!=0L){
                long any=0L;
                bits=requireAny;
                while(bits!=0L){
                    int p=Long.numberOfTrailingZeros(bits);
                    any |= planes[p*words+w];
                    bits &= bits-1L;
                }
                selected &= any;
            }

            if(rejectAny!=0L){
                long rejected=0L;
                bits=rejectAny;
                while(bits!=0L){
                    int p=Long.numberOfTrailingZeros(bits);
                    rejected |= planes[p*words+w];
                    bits &= bits-1L;
                }
                selected &= ~rejected;
            }

            result[w]=selected&valid(w);
        }
        return MatBits.ofWords(size,result);
    }

    public Addition add(MatBitSliceLongVector other,boolean saturating){
        Objects.requireNonNull(other,"other");
        if(size!=other.size || width!=other.width) throw new IllegalArgumentException("layout mismatch");
        MatBitSliceLongVector out=new MatBitSliceLongVector(size,width);
        long[] overflowWords=new long[words];
        for(int w=0;w<words;w++){
            long carry=0;
            for(int p=0;p<width;p++){
                int index=p*words+w;
                long a=planes[index], b=other.planes[index];
                long x=a^b;
                out.planes[index]=(x^carry)&valid(w);
                carry=(a&b)|(x&carry);
            }
            carry &= valid(w);
            overflowWords[w]=carry;
            if(saturating){
                for(int p=0;p<width;p++) out.planes[p*words+w]|=carry;
            }
        }
        return new Addition(out,MatBits.ofWords(size,overflowWords));
    }

    private long valid(int word){
        if(word!=words-1 || (size&63)==0) return -1L;
        return MatPackedLongVector.mask(size&63);
    }

    private void check(long value){
        if(width<64 && (value<0 || (value & ~MatPackedLongVector.mask(width))!=0))
            throw new IllegalArgumentException("unsigned value exceeds width");
    }
}
