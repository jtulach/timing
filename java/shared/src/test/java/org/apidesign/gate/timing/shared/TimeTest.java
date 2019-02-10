package org.apidesign.gate.timing.shared;

import org.junit.Test;
import static org.junit.Assert.*;

public class TimeTest {

    public TimeTest() {
    }

    @Test
    public void testNoTime() {
        assertEquals(" --:--", Time.toString(null));
    }

    @Test
    public void testSecAndTen() {
        assertEquals("  1:10", Time.toString(1100L));
    }

    @Test
    public void test1111() {
        assertEquals(1.11, Time.toDouble(1111L), 0.01);
    }

}
