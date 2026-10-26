package com.mrfuzzihead.additionalpipes.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.junit.Test;

public class PipePermissionsTest {

    private final UUID owner = UUID.randomUUID();
    private final UUID other = UUID.randomUUID();

    @Test
    public void publicPipeIsModifiable() {
        assertTrue(PipePermissions.canModify(owner, true, false, other));
    }

    @Test
    public void creativeCanModify() {
        assertTrue(PipePermissions.canModify(owner, false, true, other));
    }

    @Test
    public void ownerCanModify() {
        assertTrue(PipePermissions.canModify(owner, false, false, owner));
    }

    @Test
    public void otherCannotModify() {
        assertFalse(PipePermissions.canModify(owner, false, false, other));
    }

    @Test
    public void unownedPrivatePipeIsNotModifiable() {
        assertFalse(PipePermissions.canModify(null, false, false, other));
    }

    @Test
    public void unownedPublicPipeIsModifiable() {
        assertTrue(PipePermissions.canModify(null, true, false, other));
    }
}
