/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.fs;

import java.io.IOException;

/**
 * Optional contract implemented by a provider's file object to supply revision
 * evidence when {@link MFile#observation()} is explicitly requested. Existing
 * providers need no implementation and keep their ordinary metadata behavior.
 * A provider must qualify its source, authority scope, byte layer, mechanism and
 * epoch; sampled timestamp/size combinations are heuristic, not authoritative.
 * Provider failures propagate as I/O failures and never imply source absence.
 */
public interface MFileChangeSource {
    /** Returns unavailable unless a provider explicitly supplies revision evidence. */
    default MFileObservation.ChangeStamp changeStamp() throws IOException {
        return MFileObservation.ChangeStamp.unavailable();
    }
}
