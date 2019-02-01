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

        List<Run> runs1 = computeRuns(events);

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

        List<Run> runs2 = computeRuns(events);

        assertEquals("Four: " + runs2, 4, runs2.size());

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

        List<Run> runs1 = computeRuns(events);
        assertEquals("One run", 1, runs1.size());
        assertEquals("Assigned to nobody", 0, runs1.get(0).getWho());

        Event assign1 = sendEvent(events, "ASSIGN", now + 400, start1.getId(), 77);

        List<Run> runs2 = computeRuns(events);
        assertEquals("Still One run", 1, runs2.size());
        assertEquals("Assigned to 77", 77, runs2.get(0).getWho());
    }

    @Test
    public void onStartBehavior() {
        NavigableSet<Event> events = new TreeSet<>(Events.COMPARATOR);
        long now = System.currentTimeMillis();

        Event assign1 = sendEvent(events, Events.ASSIGN, now + 50, -1, 77);

        Event start1 = sendEvent(events, Events.START, now + 100);
        assertNotNull(start1);

        List<Run> runs1 = computeRuns(events);
        assertEquals("One run", 1, runs1.size());
        assertEquals("Assigned to 77", 77, runs1.get(0).getWho());


        Event start2 = sendEvent(events, Events.START, now + 2000);

        List<Run> runs2 = computeRuns(events);
        assertEquals("2nd run", 2, runs2.size());
        assertEquals("Assigned to nobody", 0, runs2.get(0).getWho());
        assertEquals("Remains assigned to 77", 77, runs2.get(1).getWho());
    }

    @Test
    public void ignoringAnEventOfStart() {
        NavigableSet<Event> events = new TreeSet<>(Events.COMPARATOR);
        events.add(new Event().withId(22).withType(Events.START).withWhen(432));
        List<Run> runs1 = computeRuns(events);
        assertEquals("One event", 1, runs1.size());

        events.add(new Event().withId(23).withType(Events.START).withWhen(433));
        List<Run> runs2 = computeRuns(events);
        assertEquals("Two events", 2, runs2.size());

        events.add(new Event().withId(24).withType(Events.IGNORE).withWhen(455).withRef(22));
        List<Run> runs3 = computeRuns(events);
        assertEquals("One event again", 1, runs3.size());
    }

    @Test
    public void nextOnStartTest() {
        NavigableSet<Event> events = new TreeSet<>(Events.COMPARATOR);


        long firstRound = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            int expectedWho = (i + 1) * 10;
            long next = firstRound + i * 1000;
            Event on1 = sendEvent(events, "ASSIGN", next + 50, -1, expectedWho);
            final int actualWho = Runs.compute(events).getStarting();
            assertEquals("Who starts next?", expectedWho, actualWho);

            Event start1 = sendEvent(events, "START", next + 100);
            assertEquals("Who knows who starts next", -1, Runs.compute(events).getStarting());
        }

        long secondRound = firstRound + 30 * 1000;
        Event firstOneHasToBeSelected = sendEvent(events, "ASSIGN", secondRound + 50, -1, 10);
        assertNotNull("#10 selected for start", firstOneHasToBeSelected);
        for (int i = 0; i < 10; i++) {
            int expectedWho = (i + 1) * 10;
            long next = secondRound + i * 1000;
            final int actualWho = Runs.compute(events).getStarting();
            assertEquals("Who starts next?", expectedWho, actualWho);

            Event start1 = sendEvent(events, "START", next + 100);
        }
        assertEquals("#10 is selected again", 10, Runs.compute(events).getStarting());

        long afterRounds = secondRound + 30 * 1000;

        Event selectFromMiddle = sendEvent(events, "ASSIGN", afterRounds + 50, -1, 50);
        final int actualWho50 = Runs.compute(events).getStarting();
        assertEquals("#50 explicitly selected", 50, Runs.compute(events).getStarting());

        Event start1 = sendEvent(events, "START", afterRounds + 100);

        final int actualWho60 = Runs.compute(events).getStarting();
        assertEquals("#60 automatically selected as next", 60, Runs.compute(events).getStarting());
    }

    private static int cnt;
    private static Event sendEvent(NavigableSet<Event> events, String type, long when, int... refWho) {
        return sendEvent(events, Events.valueOf(type), when, refWho);
    }
    private static Event sendEvent(NavigableSet<Event> events, Events type, long when, int... refWho) {
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

    private static List<Run> computeRuns(NavigableSet<Event> events) {
        return Runs.compute(events).getRuns();
    }
}
