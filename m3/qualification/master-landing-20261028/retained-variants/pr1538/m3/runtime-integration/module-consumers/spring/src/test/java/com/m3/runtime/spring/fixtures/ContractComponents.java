/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.runtime.spring.fixtures;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.stereotype.Component;

public final class ContractComponents {
    public static final AtomicInteger INITIALIZED = new AtomicInteger();
    public static final AtomicInteger DESTROYED = new AtomicInteger();
    private ContractComponents() { }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @Component
    public @interface FixtureComponent { }

    @Component("visibleService")
    @Profile("active")
    public static final class Visible implements InitializingBean, DisposableBean {
        public String value() { return "indexed bean"; }
        @Override public void afterPropertiesSet() { INITIALIZED.incrementAndGet(); }
        @Override public void destroy() { DESTROYED.incrementAndGet(); }
    }
    @FixtureComponent
    public static final class MetaService { }
    @Component("prototypeService")
    @Scope("prototype")
    public static final class Prototype { }
    @Component("sampleFactory")
    public static final class SampleFactory implements FactoryBean<String> {
        @Override public String getObject() { return "factory product"; }
        @Override public Class<?> getObjectType() { return String.class; }
        @Override public boolean isSingleton() { return true; }
    }
    @Component("inactiveService")
    @Profile("inactive")
    public static final class Inactive { }
    @Component("conditionalService")
    @Conditional(Never.class)
    public static final class ConditionalService { }
    public static final class Never implements Condition {
        @Override public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) { return false; }
    }
    @Component("excludedService")
    public static final class Excluded { }
}
