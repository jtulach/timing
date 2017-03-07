package org.apidesign.gate.timing.shared;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class EventsTest {
    @Test
    public void shortWhenShowsMinutesAndSeconds() {
        Event ev1 = new Event().withWhen(1488855865759L).withType("TIME-CHECK");
        assertEquals("04'25''", ev1.getShortWhen());
        Event ev2 = new Event().withWhen(1488855865973L).withType("TIME-CHECK");
        assertEquals("04'25''", ev2.getShortWhen());
        Event ev3 = new Event().withWhen(1488855866759L).withType("TIME-CHECK");
        assertEquals("04'26''", ev3.getShortWhen());
    }
}
