package com.hoangluongtran0309.application;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ServerVersionTest {

    @Test
    void exactVersionIsAtLeastItself() {
        assertTrue(new ServerVersion(1, 21, 4).isAtLeast(1, 21, 4));
    }

    @Test
    void higherPatchIsAtLeast() {
        assertTrue(new ServerVersion(1, 21, 5).isAtLeast(1, 21, 4));
    }

    @Test
    void lowerPatchIsNotAtLeast() {
        assertFalse(new ServerVersion(1, 21, 3).isAtLeast(1, 21, 4));
    }

    @Test
    void higherMinorOverridesLowerPatch() {
        assertTrue(new ServerVersion(1, 22, 0).isAtLeast(1, 21, 4));
    }

    @Test
    void lowerMinorIsNotAtLeastEvenWithHigherPatch() {
        assertFalse(new ServerVersion(1, 20, 99).isAtLeast(1, 21, 4));
    }

    @Test
    void higherMajorOverridesEverything() {
        assertTrue(new ServerVersion(2, 0, 0).isAtLeast(1, 21, 4));
    }
}
