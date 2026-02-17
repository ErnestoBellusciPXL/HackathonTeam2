package be.codeforbelgium.openinzichten.api.response;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InviteConnectionStatusTests {

    @Test
    void enumContainsAllExpectedConstants() {
        InviteConnectionStatus[] values = InviteConnectionStatus.values();
        assertEquals(5, values.length);
        assertArrayEquals(new InviteConnectionStatus[]{
                InviteConnectionStatus.SUCCESS,
                InviteConnectionStatus.FAILURE,
                InviteConnectionStatus.INVALID_UUID,
                InviteConnectionStatus.NOT_FOUND,
                InviteConnectionStatus.INVALID_ACTION
        }, values, "Order or contents of enum changed unexpectedly");
    }

    @Test
    void valueOfReturnsCorrectEnum() {
        assertEquals(InviteConnectionStatus.SUCCESS, InviteConnectionStatus.valueOf("SUCCESS"));
        assertEquals(InviteConnectionStatus.INVALID_UUID, InviteConnectionStatus.valueOf("INVALID_UUID"));
    }

    @Test
    void valueOfInvalidThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> InviteConnectionStatus.valueOf("MISSING"));
    }
}
