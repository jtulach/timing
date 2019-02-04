package org.apidesign.gate.timing.server;

import org.junit.Test;
import static org.junit.Assert.*;

public class AdminResourceTest {

    public AdminResourceTest() {
    }

    @Test
    public void validStrings() {
        assertTrue(AdminResource.isValidName("Jarda jede"));
        assertTrue(AdminResource.isValidName("Jarouš se řítí"));
    }

    @Test
    public void slashInvalid() {
        assertFalse(AdminResource.isValidName("Jde/Jede"));
    }

    @Test
    public void backslashInvalid() {
        assertFalse(AdminResource.isValidName("Jede\\Jde"));
    }

    @Test
    public void dotInvalid() {
        assertFalse(AdminResource.isValidName("je.je"));
    }
}
