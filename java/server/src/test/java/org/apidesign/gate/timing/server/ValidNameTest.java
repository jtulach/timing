package org.apidesign.gate.timing.server;

import org.junit.Test;
import static org.junit.Assert.*;

public class ValidNameTest {

    public ValidNameTest() {
    }

    @Test
    public void validStrings() {
        assertTrue(TimingResource.isValidName("Jarda jede"));
        assertTrue(TimingResource.isValidName("Jarouš se řítí"));
    }

    @Test
    public void slashInvalid() {
        assertFalse(TimingResource.isValidName("Jde/Jede"));
    }

    @Test
    public void backslashInvalid() {
        assertFalse(TimingResource.isValidName("Jede\\Jde"));
    }

    @Test
    public void dotInvalid() {
        assertFalse(TimingResource.isValidName("je.je"));
    }
}
