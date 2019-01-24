package org.apidesign.gate.timing.shared;

import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class RunsTest {
    @Test
    public void sequenceOfRuns() {
        NavigableSet<Event> events = new TreeSet<>(Events.COMPARATOR);
        long now = System.currentTimeMillis();

        Event start1 = sendEvent(events, "START", now + 100);
        Event start2 = sendEvent(events, "START", now + 200);
        Event start3 = sendEvent(events, "START", now + 300);
        Event start4 = sendEvent(events, "START", now + 400);

        Event finish1 = sendEvent(events, "FINISH", now + 1000);
        Event finish2 = sendEvent(events, "FINISH", now + 2000);

        List<Run> runs1 = Runs.compute(events);

        assertEquals("Four: " + runs1, 4, runs1.size());

        assertFinished(900, runs1.get(3));
        assertFinished(1800, runs1.get(2));
        assertFinished(-1, runs1.get(1));
        assertFinished(-1, runs1.get(0));

        assertEquals(1, runs1.get(3).getId());
        assertEquals(2, runs1.get(2).getId());
        assertEquals(3, runs1.get(1).getId());
        assertEquals(4, runs1.get(0).getId());
        
        Event ignore1 = sendEvent(events, "IGNORE", now + 9000, finish2.getId());

        List<Run> runs2 = Runs.compute(events);

        assertEquals("Four: " + runs2, 4, runs1.size());

        assertFinished(900, runs2.get(3));
        assertFinished(-1, runs2.get(2));
        assertFinished(-1, runs2.get(1));
        assertFinished(-1, runs2.get(0));
        
        assertEquals(1, runs2.get(3).getId());
        assertEquals(2, runs2.get(2).getId());
        assertEquals(3, runs2.get(1).getId());
        assertEquals(4, runs2.get(0).getId());
    }

    @Test
    public void connectRuns() {
        NavigableSet<Event> events = new TreeSet<>(Events.COMPARATOR);
        long now = System.currentTimeMillis();

        Event start1 = sendEvent(events, "START", now + 100);

        List<Run> runs1 = Runs.compute(events);
        assertEquals("One run", 1, runs1.size());
        assertEquals("Assigned to nobody", 0, runs1.get(0).getWho());

        Event assign1 = sendEvent(events, "ASSIGN", now + 400, start1.getId(), 77);

        List<Run> runs2 = Runs.compute(events);
        assertEquals("Still One run", 1, runs2.size());
        assertEquals("Assigned to 77", 77, runs2.get(0).getWho());
    }

    private static int cnt;
    private static Event sendEvent(NavigableSet<Event> events, String type, long when, int... refWho) {
        final Event ev = new Event().withId(cnt++).withType(type).withWhen(when);
        if (refWho.length > 0) {
            ev.withRef(refWho[0]);
        }
        if (refWho.length > 1) {
            ev.withWho(refWho[1]);
        }
        events.add(ev);
        return ev;
    }

    private static void assertFinished(long time, Run run) {
        assertNotNull("Started", run.getStart());
        if (time == -1) {
            assertNull("Not finished", run.getFinish());
            return;
        }
        assertNotNull("Finished", run.getFinish());

        long took = run.getFinish().getWhen() - run.getStart().getWhen();

        assertEquals(time, took);
    }

}
