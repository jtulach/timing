package org.apidesign.gate.timing.shared;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class EventsTest {
    @Test
    public void shortWhenShowsCents() {
        Event ev1 = new Event().withWhen(1488855865759L).withType(Events.START);
        assertEquals("04'25:75", ev1.getShortWhen());
    }

    @Test
    public void shortWhenShowsCents2() {
        Event ev2 = new Event().withWhen(1488855865973L).withType(Events.START);
        assertEquals("04'25:97", ev2.getShortWhen());
    }


    @Test
    public void shortWhenShowsCents3() {
        Event ev3 = new Event().withWhen(1488855866759L).withType(Events.START);
        assertEquals("04'26:75", ev3.getShortWhen());
    }
}
