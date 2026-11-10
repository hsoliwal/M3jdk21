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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.util.Objects;

/**
 * Correctness-oriented reflective codec for Java records.
 * Generated codecs should replace it on hot paths, but they must preserve these bytes.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-value/src/main/java/jdk/internal/m3/matvalue/MatRecordCodec.java}
 * blob {@code 5a7d53bbbec2dc0ac7b7e8f0576675b8b7df2bc9} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class MatRecordCodec<R extends Record> implements MatCodec<R> {
    private final Class<R> type;
    private final RecordComponent[] components;
    private final MatCodec<Object>[] codecs;
    private final Constructor<R> constructor;
    private final String typeId;

    private MatRecordCodec(Class<R> type) {
        this.type = Objects.requireNonNull(type, "type");
        if (!type.isRecord()) throw new IllegalArgumentException("not a record: " + type.getName());
        this.components = type.getRecordComponents();
        @SuppressWarnings("unchecked")
        MatCodec<Object>[] resolved = (MatCodec<Object>[]) new MatCodec<?>[components.length];
        Class<?>[] parameterTypes = new Class<?>[components.length];
        StringBuilder id = new StringBuilder("synex:record/1:").append(type.getName()).append('(');
        for (int i=0;i<components.length;i++) {
            RecordComponent component=components[i];
            parameterTypes[i]=component.getType();
            @SuppressWarnings("unchecked")
            MatCodec<Object> codec=MatCodecs.of((Class<Object>)component.getType());
            if (!component.getType().isPrimitive()) codec=MatCodecs.nullable(codec);
            resolved[i]=codec;
            if(i>0) id.append(',');
            id.append(component.getName()).append(':').append(codec.typeId());
        }
        id.append(')');
        this.codecs=resolved;
        this.typeId=id.toString();
        try {
            this.constructor=type.getDeclaredConstructor(parameterTypes);
            if (!constructor.canAccess(null)) constructor.setAccessible(true);
        } catch (ReflectiveOperationException | SecurityException e) {
            throw new IllegalArgumentException("record canonical constructor inaccessible: "+type.getName(),e);
        }
    }

    public static <R extends Record> MatRecordCodec<R> create(Class<R> type) {
        return new MatRecordCodec<>(type);
    }

    @Override public String typeId(){ return typeId; }
    @Override public Class<R> javaType(){ return type; }

    @Override
    public void encode(R value, MatSink sink) {
        Objects.requireNonNull(value,"value");
        for(int i=0;i<components.length;i++) {
            try {
                Object field=components[i].getAccessor().invoke(value);
                codecs[i].encode(field,sink);
            } catch (ReflectiveOperationException e) {
                throw new IllegalArgumentException("record accessor failed: "+components[i].getName(),e);
            }
        }
    }

    @Override
    public R decode(MatSource source) {
        Object[] args=new Object[components.length];
        for(int i=0;i<args.length;i++) args[i]=codecs[i].decode(source);
        try {
            return constructor.newInstance(args);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException("record construction failed: "+type.getName(),e);
        }
    }
}
