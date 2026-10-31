/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

open module uimaj.core {
    requires java.management;
    requires java.prefs;
    requires java.rmi;

    requires transitive java.desktop;
    requires transitive java.logging;
    requires transitive java.xml;
    requires transitive org.slf4j;

    exports org.apache.uima;
    exports org.apache.uima.analysis_component;
    exports org.apache.uima.analysis_engine;
    exports org.apache.uima.analysis_engine.annotator;
    exports org.apache.uima.analysis_engine.asb;
    exports org.apache.uima.analysis_engine.asb.impl;
    exports org.apache.uima.analysis_engine.impl;
    exports org.apache.uima.analysis_engine.impl.compatibility;
    exports org.apache.uima.analysis_engine.metadata;
    exports org.apache.uima.analysis_engine.metadata.impl;
    exports org.apache.uima.analysis_engine.service.impl;
    exports org.apache.uima.cas;
    exports org.apache.uima.cas.admin;
    exports org.apache.uima.cas.impl;
    exports org.apache.uima.cas.text;
    exports org.apache.uima.cas_data;
    exports org.apache.uima.cas_data.impl;
    exports org.apache.uima.collection;
    exports org.apache.uima.collection.base_cpm;
    exports org.apache.uima.collection.impl;
    exports org.apache.uima.collection.metadata;
    exports org.apache.uima.examples;
    exports org.apache.uima.flow;
    exports org.apache.uima.flow.impl;
    exports org.apache.uima.impl;
    exports org.apache.uima.internal.util;
    exports org.apache.uima.internal.util.function;
    exports org.apache.uima.internal.util.rb_trees;
    exports org.apache.uima.jcas;
    exports org.apache.uima.jcas.cas;
    exports org.apache.uima.jcas.impl;
    exports org.apache.uima.jcas.tcas;
    exports org.apache.uima.pear.tools;
    exports org.apache.uima.pear.util;
    exports org.apache.uima.resource;
    exports org.apache.uima.resource.impl;
    exports org.apache.uima.resource.metadata;
    exports org.apache.uima.resource.metadata.impl;
    exports org.apache.uima.resource.service.impl;
    exports org.apache.uima.search;
    exports org.apache.uima.search.impl;
    exports org.apache.uima.spi;
    exports org.apache.uima.uimacpp;
    exports org.apache.uima.util;
    exports org.apache.uima.util.impl;

    uses org.apache.uima.spi.FsIndexCollectionProvider;
    uses org.apache.uima.spi.JCasClassProvider;
    uses org.apache.uima.spi.TypePrioritiesProvider;
    uses org.apache.uima.spi.TypeSystemDescriptionProvider;
    uses org.apache.uima.spi.TypeSystemProvider;
}
