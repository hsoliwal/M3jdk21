/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.parallel;

import com.m3.parallel.spi.BackendProvider;
import com.m3.parallel.spi.Probe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Process-wide backend discovery, run once on first use. Never throws for a faulty provider. */
final class BackendRegistry {
    static final Backend REFERENCE = new Backend(Backend.REFERENCE_ID, new ReferenceProvider(),
            EnumSet.of(Backend.Capability.REFERENCE, Backend.Capability.CPU, Backend.Capability.MULTI_THREADED), 0);

    private static final Pattern ID = Pattern.compile("[a-z0-9][a-z0-9._-]{0,63}");
    private static final int MAX_LOAD_FAILURES = 64;
    private static final Comparator<Backend> PREFERENCE = Comparator.comparingInt(Backend::score).reversed()
            .thenComparing(backend -> !backend.isReference())
            .thenComparing(Backend::id);

    private static final Map<String, Probe> PROBES;
    private static final Map<String, Backend> AVAILABLE;

    static {
        Map<String, Probe> probes = new TreeMap<>();
        Map<String, Backend> available = new TreeMap<>();
        probes.put(Backend.REFERENCE_ID, Probe.ready());
        Map<String, List<BackendProvider>> byId = new TreeMap<>();
        loadProviders(probes).forEach(provider -> idOf(provider, probes)
                .ifPresent(id -> byId.computeIfAbsent(id, key -> new ArrayList<>()).add(provider)));
        byId.forEach((id, providers) -> admit(id, providers, probes, available));
        PROBES = Collections.unmodifiableMap(probes);
        AVAILABLE = Collections.unmodifiableMap(available);
    }

    private BackendRegistry() {
    }

    static Map<String, Probe> probes() {
        return PROBES;
    }

    static Backend byId(String id) {
        if (Backend.REFERENCE_ID.equals(id)) {
            return REFERENCE;
        }
        Backend backend = AVAILABLE.get(id);
        if (backend != null) {
            return backend;
        }
        Probe probe = PROBES.get(id);
        if (probe == null) {
            throw new IllegalArgumentException("unknown backend id: " + id);
        }
        throw new IllegalStateException("backend " + id + " is unavailable: " + probe.reason());
    }

    static Backend select(Backend.Policy policy) {
        return switch (policy) {
            case REFERENCE -> REFERENCE;
            case BEST -> Stream.concat(Stream.of(REFERENCE), AVAILABLE.values().stream())
                    .min(PREFERENCE).orElse(REFERENCE);
            case ACCELERATOR -> AVAILABLE.values().stream().min(PREFERENCE)
                    .orElseThrow(() -> new IllegalStateException("no accelerator backend available: " + PROBES));
        };
    }

    private static List<BackendProvider> loadProviders(Map<String, Probe> probes) {
        List<BackendProvider> providers = new ArrayList<>();
        Iterator<ServiceLoader.Provider<BackendProvider>> iterator =
                ServiceLoader.load(BackendProvider.class).stream().iterator();
        int failures = 0;
        while (failures < MAX_LOAD_FAILURES) {
            try {
                if (!iterator.hasNext()) {
                    break;
                }
                instantiate(iterator.next(), probes).ifPresent(providers::add);
            } catch (ServiceConfigurationError error) {
                failures++;
                probes.put("unloadable:#" + failures, failure(error));
            }
        }
        return providers;
    }

    private static Optional<BackendProvider> instantiate(ServiceLoader.Provider<BackendProvider> provider,
            Map<String, Probe> probes) {
        try {
            return Optional.of(provider.get());
        } catch (ServiceConfigurationError | RuntimeException | LinkageError error) {
            probes.put("unloadable:" + provider.type().getName(), failure(error));
            return Optional.empty();
        }
    }

    private static Optional<String> idOf(BackendProvider provider, Map<String, Probe> probes) {
        String key = "invalid:" + provider.getClass().getName();
        try {
            String id = provider.id();
            if (id == null || !ID.matcher(id).matches()) {
                probes.put(key, Probe.unavailable("invalid backend id: " + id));
                return Optional.empty();
            }
            if (Backend.REFERENCE_ID.equals(id)) {
                probes.put(key, Probe.unavailable("backend id " + id + " is reserved for the CPU reference"));
                return Optional.empty();
            }
            return Optional.of(id);
        } catch (RuntimeException | LinkageError error) {
            probes.put(key, failure(error));
            return Optional.empty();
        }
    }

    private static void admit(String id, List<BackendProvider> providers, Map<String, Probe> probes,
            Map<String, Backend> available) {
        if (providers.size() > 1) {
            probes.put(id, Probe.unavailable("ambiguous: " + providers.size() + " providers declare id " + id + " ("
                    + providers.stream().map(p -> p.getClass().getName()).sorted()
                            .collect(Collectors.joining(", ")) + ")"));
            return;
        }
        BackendProvider provider = providers.get(0);
        try {
            Set<Backend.Capability> capabilities = Set.copyOf(provider.capabilities());
            int score = provider.score();
            Probe probe = Objects.requireNonNull(provider.probe(), "probe");
            probes.put(id, probe);
            if (probe.available()) {
                available.put(id, new Backend(id, provider, capabilities, score));
            }
        } catch (RuntimeException | LinkageError error) {
            probes.put(id, failure(error));
        }
    }

    private static Probe failure(Throwable error) {
        return Probe.unavailable("probe failed: " + error.getClass().getName() + ": " + error.getMessage());
    }
}
