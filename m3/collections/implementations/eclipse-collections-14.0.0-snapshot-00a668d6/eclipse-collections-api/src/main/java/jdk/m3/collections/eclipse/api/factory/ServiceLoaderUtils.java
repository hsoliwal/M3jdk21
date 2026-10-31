/*
 * Copyright (c) 2021 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.api.factory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

public final class ServiceLoaderUtils
{
    private static final Map<String, String> FACTORY_IMPL = new HashMap<>();

    static
    {
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.bag.ImmutableBagFactory", "jdk.m3.collections.eclipse.impl.bag.immutable.ImmutableBagFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.bag.MultiReaderBagFactory", "jdk.m3.collections.eclipse.impl.bag.mutable.MultiReaderMutableBagFactory");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.bag.MutableBagFactory", "jdk.m3.collections.eclipse.impl.bag.mutable.MutableBagFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.bag.primitive.ImmutableBooleanBagFactory", "jdk.m3.collections.eclipse.impl.bag.immutable.primitive.ImmutableBooleanBagFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.bag.primitive.ImmutableByteBagFactory", "jdk.m3.collections.eclipse.impl.bag.immutable.primitive.ImmutableByteBagFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.bag.primitive.ImmutableCharBagFactory", "jdk.m3.collections.eclipse.impl.bag.immutable.primitive.ImmutableCharBagFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.bag.primitive.ImmutableDoubleBagFactory", "jdk.m3.collections.eclipse.impl.bag.immutable.primitive.ImmutableDoubleBagFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.bag.primitive.ImmutableFloatBagFactory", "jdk.m3.collections.eclipse.impl.bag.immutable.primitive.ImmutableFloatBagFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.bag.primitive.ImmutableIntBagFactory", "jdk.m3.collections.eclipse.impl.bag.immutable.primitive.ImmutableIntBagFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.bag.primitive.ImmutableLongBagFactory", "jdk.m3.collections.eclipse.impl.bag.immutable.primitive.ImmutableLongBagFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.bag.primitive.ImmutableShortBagFactory", "jdk.m3.collections.eclipse.impl.bag.immutable.primitive.ImmutableShortBagFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.bag.primitive.MutableBooleanBagFactory", "jdk.m3.collections.eclipse.impl.bag.mutable.primitive.MutableBooleanBagFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.bag.primitive.MutableByteBagFactory", "jdk.m3.collections.eclipse.impl.bag.mutable.primitive.MutableByteBagFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.bag.primitive.MutableCharBagFactory", "jdk.m3.collections.eclipse.impl.bag.mutable.primitive.MutableCharBagFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.bag.primitive.MutableDoubleBagFactory", "jdk.m3.collections.eclipse.impl.bag.mutable.primitive.MutableDoubleBagFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.bag.primitive.MutableFloatBagFactory", "jdk.m3.collections.eclipse.impl.bag.mutable.primitive.MutableFloatBagFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.bag.primitive.MutableIntBagFactory", "jdk.m3.collections.eclipse.impl.bag.mutable.primitive.MutableIntBagFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.bag.primitive.MutableLongBagFactory", "jdk.m3.collections.eclipse.impl.bag.mutable.primitive.MutableLongBagFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.bag.primitive.MutableShortBagFactory", "jdk.m3.collections.eclipse.impl.bag.mutable.primitive.MutableShortBagFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.bag.sorted.ImmutableSortedBagFactory", "jdk.m3.collections.eclipse.impl.bag.sorted.immutable.ImmutableSortedBagFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.bag.sorted.MutableSortedBagFactory", "jdk.m3.collections.eclipse.impl.bag.sorted.mutable.MutableSortedBagFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.bimap.ImmutableBiMapFactory", "jdk.m3.collections.eclipse.impl.bimap.immutable.ImmutableBiMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.bimap.MutableBiMapFactory", "jdk.m3.collections.eclipse.impl.bimap.mutable.MutableBiMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.list.FixedSizeListFactory", "jdk.m3.collections.eclipse.impl.list.fixed.FixedSizeListFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.list.ImmutableListFactory", "jdk.m3.collections.eclipse.impl.list.immutable.ImmutableListFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.list.MultiReaderListFactory", "jdk.m3.collections.eclipse.impl.list.mutable.MultiReaderMutableListFactory");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.list.MutableListFactory", "jdk.m3.collections.eclipse.impl.list.mutable.MutableListFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.list.primitive.ImmutableBooleanListFactory", "jdk.m3.collections.eclipse.impl.list.immutable.primitive.ImmutableBooleanListFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.list.primitive.ImmutableByteListFactory", "jdk.m3.collections.eclipse.impl.list.immutable.primitive.ImmutableByteListFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.list.primitive.ImmutableCharListFactory", "jdk.m3.collections.eclipse.impl.list.immutable.primitive.ImmutableCharListFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.list.primitive.ImmutableDoubleListFactory", "jdk.m3.collections.eclipse.impl.list.immutable.primitive.ImmutableDoubleListFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.list.primitive.ImmutableFloatListFactory", "jdk.m3.collections.eclipse.impl.list.immutable.primitive.ImmutableFloatListFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.list.primitive.ImmutableIntListFactory", "jdk.m3.collections.eclipse.impl.list.immutable.primitive.ImmutableIntListFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.list.primitive.ImmutableLongListFactory", "jdk.m3.collections.eclipse.impl.list.immutable.primitive.ImmutableLongListFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.list.primitive.ImmutableShortListFactory", "jdk.m3.collections.eclipse.impl.list.immutable.primitive.ImmutableShortListFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.list.primitive.MutableBooleanListFactory", "jdk.m3.collections.eclipse.impl.list.mutable.primitive.MutableBooleanListFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.list.primitive.MutableByteListFactory", "jdk.m3.collections.eclipse.impl.list.mutable.primitive.MutableByteListFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.list.primitive.MutableCharListFactory", "jdk.m3.collections.eclipse.impl.list.mutable.primitive.MutableCharListFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.list.primitive.MutableDoubleListFactory", "jdk.m3.collections.eclipse.impl.list.mutable.primitive.MutableDoubleListFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.list.primitive.MutableFloatListFactory", "jdk.m3.collections.eclipse.impl.list.mutable.primitive.MutableFloatListFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.list.primitive.MutableIntListFactory", "jdk.m3.collections.eclipse.impl.list.mutable.primitive.MutableIntListFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.list.primitive.MutableLongListFactory", "jdk.m3.collections.eclipse.impl.list.mutable.primitive.MutableLongListFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.list.primitive.MutableShortListFactory", "jdk.m3.collections.eclipse.impl.list.mutable.primitive.MutableShortListFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.FixedSizeMapFactory", "jdk.m3.collections.eclipse.impl.map.fixed.FixedSizeMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.ImmutableMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.ImmutableMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.MutableMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.MutableMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableBooleanBooleanMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableBooleanBooleanMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableBooleanByteMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableBooleanByteMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableBooleanCharMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableBooleanCharMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableBooleanDoubleMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableBooleanDoubleMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableBooleanFloatMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableBooleanFloatMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableBooleanIntMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableBooleanIntMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableBooleanLongMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableBooleanLongMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableBooleanShortMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableBooleanShortMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableByteBooleanMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableByteBooleanMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableByteByteMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableByteByteMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableByteCharMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableByteCharMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableByteDoubleMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableByteDoubleMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableByteFloatMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableByteFloatMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableByteIntMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableByteIntMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableByteLongMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableByteLongMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableByteObjectMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableByteObjectMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableByteShortMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableByteShortMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableCharBooleanMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableCharBooleanMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableCharByteMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableCharByteMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableCharCharMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableCharCharMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableCharDoubleMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableCharDoubleMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableCharFloatMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableCharFloatMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableCharIntMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableCharIntMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableCharLongMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableCharLongMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableCharObjectMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableCharObjectMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableCharShortMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableCharShortMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableDoubleBooleanMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableDoubleBooleanMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableDoubleByteMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableDoubleByteMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableDoubleCharMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableDoubleCharMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableDoubleDoubleMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableDoubleDoubleMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableDoubleFloatMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableDoubleFloatMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableDoubleIntMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableDoubleIntMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableDoubleLongMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableDoubleLongMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableDoubleObjectMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableDoubleObjectMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableDoubleShortMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableDoubleShortMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableFloatBooleanMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableFloatBooleanMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableFloatByteMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableFloatByteMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableFloatCharMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableFloatCharMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableFloatDoubleMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableFloatDoubleMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableFloatFloatMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableFloatFloatMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableFloatIntMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableFloatIntMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableFloatLongMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableFloatLongMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableFloatObjectMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableFloatObjectMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableFloatShortMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableFloatShortMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableIntBooleanMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableIntBooleanMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableIntByteMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableIntByteMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableIntCharMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableIntCharMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableIntDoubleMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableIntDoubleMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableIntFloatMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableIntFloatMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableIntIntMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableIntIntMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableIntLongMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableIntLongMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableIntObjectMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableIntObjectMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableIntShortMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableIntShortMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableLongBooleanMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableLongBooleanMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableLongByteMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableLongByteMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableLongCharMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableLongCharMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableLongDoubleMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableLongDoubleMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableLongFloatMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableLongFloatMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableLongIntMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableLongIntMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableLongLongMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableLongLongMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableLongObjectMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableLongObjectMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableLongShortMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableLongShortMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableObjectBooleanMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableObjectBooleanMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableObjectByteMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableObjectByteMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableObjectCharMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableObjectCharMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableObjectDoubleMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableObjectDoubleMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableObjectFloatMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableObjectFloatMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableObjectIntMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableObjectIntMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableObjectLongMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableObjectLongMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableObjectShortMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableObjectShortMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableShortBooleanMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableShortBooleanMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableShortByteMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableShortByteMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableShortCharMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableShortCharMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableShortDoubleMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableShortDoubleMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableShortFloatMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableShortFloatMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableShortIntMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableShortIntMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableShortLongMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableShortLongMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableShortObjectMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableShortObjectMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableShortShortMapFactory", "jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableShortShortMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableBooleanBooleanMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableBooleanBooleanMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableBooleanByteMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableBooleanByteMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableBooleanCharMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableBooleanCharMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableBooleanDoubleMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableBooleanDoubleMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableBooleanFloatMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableBooleanFloatMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableBooleanIntMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableBooleanIntMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableBooleanLongMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableBooleanLongMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableBooleanShortMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableBooleanShortMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableByteBooleanMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableByteBooleanMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableByteByteMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableByteByteMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableByteCharMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableByteCharMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableByteDoubleMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableByteDoubleMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableByteFloatMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableByteFloatMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableByteIntMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableByteIntMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableByteLongMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableByteLongMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableByteObjectMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableByteObjectMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableByteShortMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableByteShortMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableCharBooleanMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableCharBooleanMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableCharByteMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableCharByteMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableCharCharMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableCharCharMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableCharDoubleMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableCharDoubleMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableCharFloatMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableCharFloatMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableCharIntMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableCharIntMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableCharLongMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableCharLongMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableCharObjectMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableCharObjectMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableCharShortMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableCharShortMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableDoubleBooleanMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableDoubleBooleanMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableDoubleByteMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableDoubleByteMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableDoubleCharMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableDoubleCharMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableDoubleDoubleMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableDoubleDoubleMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableDoubleFloatMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableDoubleFloatMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableDoubleIntMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableDoubleIntMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableDoubleLongMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableDoubleLongMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableDoubleObjectMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableDoubleObjectMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableDoubleShortMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableDoubleShortMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableFloatBooleanMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableFloatBooleanMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableFloatByteMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableFloatByteMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableFloatCharMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableFloatCharMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableFloatDoubleMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableFloatDoubleMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableFloatFloatMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableFloatFloatMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableFloatIntMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableFloatIntMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableFloatLongMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableFloatLongMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableFloatObjectMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableFloatObjectMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableFloatShortMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableFloatShortMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableIntBooleanMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableIntBooleanMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableIntByteMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableIntByteMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableIntCharMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableIntCharMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableIntDoubleMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableIntDoubleMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableIntFloatMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableIntFloatMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableIntIntMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableIntIntMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableIntLongMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableIntLongMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableIntObjectMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableIntObjectMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableIntShortMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableIntShortMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableLongBooleanMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableLongBooleanMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableLongByteMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableLongByteMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableLongCharMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableLongCharMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableLongDoubleMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableLongDoubleMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableLongFloatMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableLongFloatMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableLongIntMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableLongIntMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableLongLongMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableLongLongMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableLongObjectMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableLongObjectMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableLongShortMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableLongShortMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableObjectBooleanHashingStrategyMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableObjectBooleanHashingStrategyMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableObjectBooleanMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableObjectBooleanMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableObjectByteHashingStrategyMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableObjectByteHashingStrategyMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableObjectByteMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableObjectByteMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableObjectCharHashingStrategyMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableObjectCharHashingStrategyMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableObjectCharMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableObjectCharMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableObjectDoubleHashingStrategyMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableObjectDoubleHashingStrategyMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableObjectDoubleMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableObjectDoubleMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableObjectFloatHashingStrategyMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableObjectFloatHashingStrategyMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableObjectFloatMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableObjectFloatMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableObjectIntHashingStrategyMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableObjectIntHashingStrategyMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableObjectIntMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableObjectIntMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableObjectLongHashingStrategyMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableObjectLongHashingStrategyMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableObjectLongMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableObjectLongMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableObjectShortHashingStrategyMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableObjectShortHashingStrategyMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableObjectShortMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableObjectShortMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableShortBooleanMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableShortBooleanMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableShortByteMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableShortByteMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableShortCharMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableShortCharMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableShortDoubleMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableShortDoubleMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableShortFloatMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableShortFloatMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableShortIntMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableShortIntMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableShortLongMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableShortLongMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableShortObjectMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableShortObjectMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.primitive.MutableShortShortMapFactory", "jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableShortShortMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.sorted.ImmutableSortedMapFactory", "jdk.m3.collections.eclipse.impl.map.sorted.immutable.ImmutableSortedMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.sorted.MutableSortedMapFactory", "jdk.m3.collections.eclipse.impl.map.sorted.mutable.MutableSortedMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.map.ordered.MutableOrderedMapFactory", "jdk.m3.collections.eclipse.impl.map.ordered.mutable.MutableOrderedMapFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.set.FixedSizeSetFactory", "jdk.m3.collections.eclipse.impl.set.fixed.FixedSizeSetFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.set.ImmutableSetFactory", "jdk.m3.collections.eclipse.impl.set.immutable.ImmutableSetFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.set.MultiReaderSetFactory", "jdk.m3.collections.eclipse.impl.set.mutable.MultiReaderMutableSetFactory");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.set.MutableSetFactory", "jdk.m3.collections.eclipse.impl.set.mutable.MutableSetFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.set.primitive.ImmutableBooleanSetFactory", "jdk.m3.collections.eclipse.impl.set.immutable.primitive.ImmutableBooleanSetFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.set.primitive.ImmutableByteSetFactory", "jdk.m3.collections.eclipse.impl.set.immutable.primitive.ImmutableByteSetFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.set.primitive.ImmutableCharSetFactory", "jdk.m3.collections.eclipse.impl.set.immutable.primitive.ImmutableCharSetFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.set.primitive.ImmutableDoubleSetFactory", "jdk.m3.collections.eclipse.impl.set.immutable.primitive.ImmutableDoubleSetFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.set.primitive.ImmutableFloatSetFactory", "jdk.m3.collections.eclipse.impl.set.immutable.primitive.ImmutableFloatSetFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.set.primitive.ImmutableIntSetFactory", "jdk.m3.collections.eclipse.impl.set.immutable.primitive.ImmutableIntSetFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.set.primitive.ImmutableLongSetFactory", "jdk.m3.collections.eclipse.impl.set.immutable.primitive.ImmutableLongSetFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.set.primitive.ImmutableShortSetFactory", "jdk.m3.collections.eclipse.impl.set.immutable.primitive.ImmutableShortSetFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.set.primitive.MutableBooleanSetFactory", "jdk.m3.collections.eclipse.impl.set.mutable.primitive.MutableBooleanSetFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.set.primitive.MutableByteSetFactory", "jdk.m3.collections.eclipse.impl.set.mutable.primitive.MutableByteSetFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.set.primitive.MutableCharSetFactory", "jdk.m3.collections.eclipse.impl.set.mutable.primitive.MutableCharSetFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.set.primitive.MutableDoubleSetFactory", "jdk.m3.collections.eclipse.impl.set.mutable.primitive.MutableDoubleSetFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.set.primitive.MutableFloatSetFactory", "jdk.m3.collections.eclipse.impl.set.mutable.primitive.MutableFloatSetFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.set.primitive.MutableIntSetFactory", "jdk.m3.collections.eclipse.impl.set.mutable.primitive.MutableIntSetFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.set.primitive.MutableLongSetFactory", "jdk.m3.collections.eclipse.impl.set.mutable.primitive.MutableLongSetFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.set.primitive.MutableShortSetFactory", "jdk.m3.collections.eclipse.impl.set.mutable.primitive.MutableShortSetFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.set.sorted.ImmutableSortedSetFactory", "jdk.m3.collections.eclipse.impl.set.sorted.immutable.ImmutableSortedSetFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.set.sorted.MutableSortedSetFactory", "jdk.m3.collections.eclipse.impl.set.sorted.mutable.MutableSortedSetFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.stack.ImmutableStackFactory", "jdk.m3.collections.eclipse.impl.stack.immutable.ImmutableStackFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.stack.MutableStackFactory", "jdk.m3.collections.eclipse.impl.stack.mutable.MutableStackFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.stack.primitive.ImmutableBooleanStackFactory", "jdk.m3.collections.eclipse.impl.stack.immutable.primitive.ImmutableBooleanStackFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.stack.primitive.ImmutableByteStackFactory", "jdk.m3.collections.eclipse.impl.stack.immutable.primitive.ImmutableByteStackFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.stack.primitive.ImmutableCharStackFactory", "jdk.m3.collections.eclipse.impl.stack.immutable.primitive.ImmutableCharStackFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.stack.primitive.ImmutableDoubleStackFactory", "jdk.m3.collections.eclipse.impl.stack.immutable.primitive.ImmutableDoubleStackFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.stack.primitive.ImmutableFloatStackFactory", "jdk.m3.collections.eclipse.impl.stack.immutable.primitive.ImmutableFloatStackFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.stack.primitive.ImmutableIntStackFactory", "jdk.m3.collections.eclipse.impl.stack.immutable.primitive.ImmutableIntStackFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.stack.primitive.ImmutableLongStackFactory", "jdk.m3.collections.eclipse.impl.stack.immutable.primitive.ImmutableLongStackFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.stack.primitive.ImmutableShortStackFactory", "jdk.m3.collections.eclipse.impl.stack.immutable.primitive.ImmutableShortStackFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.stack.primitive.MutableBooleanStackFactory", "jdk.m3.collections.eclipse.impl.stack.mutable.primitive.MutableBooleanStackFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.stack.primitive.MutableByteStackFactory", "jdk.m3.collections.eclipse.impl.stack.mutable.primitive.MutableByteStackFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.stack.primitive.MutableCharStackFactory", "jdk.m3.collections.eclipse.impl.stack.mutable.primitive.MutableCharStackFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.stack.primitive.MutableDoubleStackFactory", "jdk.m3.collections.eclipse.impl.stack.mutable.primitive.MutableDoubleStackFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.stack.primitive.MutableFloatStackFactory", "jdk.m3.collections.eclipse.impl.stack.mutable.primitive.MutableFloatStackFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.stack.primitive.MutableIntStackFactory", "jdk.m3.collections.eclipse.impl.stack.mutable.primitive.MutableIntStackFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.stack.primitive.MutableLongStackFactory", "jdk.m3.collections.eclipse.impl.stack.mutable.primitive.MutableLongStackFactoryImpl");
        FACTORY_IMPL.put("jdk.m3.collections.eclipse.api.factory.stack.primitive.MutableShortStackFactory", "jdk.m3.collections.eclipse.impl.stack.mutable.primitive.MutableShortStackFactoryImpl");
    }

    private ServiceLoaderUtils()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    public static <T> T loadServiceClass(Class<T> serviceClass)
    {
        T result =
                ServiceLoaderUtils.loadServiceClass(serviceClass, Thread.currentThread().getContextClassLoader());
        if (result == null)
        {
            result = ServiceLoaderUtils.loadServiceClass(serviceClass, ServiceLoaderUtils.class.getClassLoader());
        }
        if (result == null)
        {
            result = ServiceLoaderUtils.loadByReflection(serviceClass, Thread.currentThread().getContextClassLoader());
        }
        if (result == null)
        {
            result = ServiceLoaderUtils.loadByReflection(serviceClass, ServiceLoaderUtils.class.getClassLoader());
        }
        if (result == null)
        {
            String message = "Could not find any implementations of "
                    + serviceClass.getSimpleName()
                    + ". Check that eclipse-collections.jar is on the classpath and that its META-INF/services directory is intact.";
            result = ServiceLoaderUtils.createProxyInstance(serviceClass, message);
        }
        return result;
    }

    private static <T> T loadServiceClass(Class<T> serviceClass, ClassLoader loader)
    {
        List<T> factories = new ArrayList<>();
        for (T factory : ServiceLoader.load(serviceClass, loader))
        {
            factories.add(factory);
        }
        if (factories.isEmpty())
        {
            return null;
        }
        if (factories.size() > 1)
        {
            String message = String.format(
                    "Found multiple implementations of %s on the classpath. Check that there is only one copy of eclipse-collections.jar on the classpath. Found implementations: %s.",
                    serviceClass.getSimpleName(),
                    factories.stream()
                            .map(T::getClass)
                            .map(Class::getSimpleName)
                            .collect(Collectors.joining(", ")));
            return ServiceLoaderUtils.createProxyInstance(serviceClass, message);
        }
        return factories.get(0);
    }

    private static <T> T loadByReflection(Class<T> serviceClass, ClassLoader loader)
    {
        String fallbackName = FACTORY_IMPL.get(serviceClass.getName());
        try
        {
            Class<T> fallbackClass = (Class<T>) Class.forName(fallbackName, true, loader);
            return fallbackClass.getDeclaredConstructor().newInstance();
        }
        catch (Exception e)
        {
            // ignore
        }
        return null;
    }

    private static <T> T createProxyInstance(Class<T> serviceClass, String message)
    {
        InvocationHandler handler = new ThrowingInvocationHandler(message);
        Object proxyInstance = Proxy.newProxyInstance(
                serviceClass.getClassLoader(),
                new Class[]{serviceClass},
                handler);
        return serviceClass.cast(proxyInstance);
    }
}
