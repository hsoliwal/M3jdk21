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
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.function.Supplier;
import com.m3.collections.spring.M3MultiValueMap;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public final class SingletonTraversalDiscriminator {
    private SingletonTraversalDiscriminator() { }
    private static final class Payload {
        private final List<String> events;
        Payload(List<String> events) {this.events=events;}
        @Override public int hashCode() {events.add("hashCode");throw new IllegalStateException("HASH_CALLBACK");}
    }
    private static String run(Supplier<MultiValueMap<String,Payload>> factory) {
        List<String> events=new ArrayList<>();MultiValueMap<String,Payload> map=factory.get();
        map.asSingleValueMap().put("key",new Payload(events));List<Payload> list=map.get("key");
        String outcome;
        try {outcome="count="+list.stream().distinct().count();}
        catch(RuntimeException failure) {outcome="error="+failure.getClass().getSimpleName();}
        return "flags="+list.spliterator().characteristics()+","+outcome+",events="+events;
    }
    private static String cursor(Supplier<MultiValueMap<String,Payload>> factory) {
        MultiValueMap<String,Payload> map=factory.get();map.asSingleValueMap().put("key",new Payload(new ArrayList<>()));
        Iterator<Payload> iterator=map.get("key").iterator();String before;
        try {iterator.remove();before="removed";}catch(RuntimeException failure){before=failure.getClass().getSimpleName();}
        String action;
        try {iterator.forEachRemaining(value->{throw new IllegalStateException("CALLBACK");});action="completed";}
        catch(RuntimeException failure){action=failure.getClass().getSimpleName();}
        return "removeBeforeNext="+before+",callback="+action+",remaining="+iterator.hasNext();
    }
    public static void main(String[] args) {
        String oracle=run(LinkedMultiValueMap::new),candidate=run(M3MultiValueMap::new);
        System.out.println("oracle="+oracle);System.out.println("candidate="+candidate);
        String oracleCursor=cursor(LinkedMultiValueMap::new),candidateCursor=cursor(M3MultiValueMap::new);
        System.out.println("oracleCursor="+oracleCursor);System.out.println("candidateCursor="+candidateCursor);
        if(!oracle.equals(candidate)||!oracleCursor.equals(candidateCursor))throw new AssertionError("IMMUTABLE_SINGLETON_TRAVERSAL_PROTOCOL_DIFFERENCE");
        System.out.println("PASS SINGLETON_TRAVERSAL protocol=flags+distinct-hash-callback+iterator-removal+throwing-consumer-lifecycle");
    }
}
