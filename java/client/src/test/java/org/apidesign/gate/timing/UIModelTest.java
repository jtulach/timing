package org.apidesign.gate.timing;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import net.java.html.BrwsrCtx;
import net.java.html.json.Models;
import org.apidesign.gate.timing.shared.Contact;
import net.java.html.junit.BrowserRunner;
import org.apidesign.gate.timing.shared.Event;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests for behavior of your application in isolation. Verify
 * behavior of your MVVC code in a unit test.
  */
@RunWith(BrowserRunner.class)
public class UIModelTest {
    private static void loadEvents(UI model, Event... events) {
        UIModel.loadEvents(model, Arrays.asList(events));
        UIModel.onEventsChangeUpdateRecords(model);
        model.onRecordsChangeUpdateWho();
    }

    @Test public void addNewSetsEdited() {
        UI model = new UI();
        Contact c = new Contact();
        UIModel.editContact(model, c);
        assertEquals("c is now edited", model.getEdited(), c);
    }

    @Test
    public void ignoringAnEvent() {
        UI model = new UI();
        loadEvents(model, new Event().withId(22).withType("START").withWhen(432));
        assertEquals("One event", 1, model.getRecords().size());
        loadEvents(model, new Event().withId(23).withType("START").withWhen(433));
        assertEquals("Two events", 2, model.getRecords().size());
        loadEvents(model, new Event().withId(24).withType("IGNORE").withWhen(455).withRef(22));
        assertEquals("One event again", 1, model.getRecords().size());
    }

    @Test
    public void oneEventPerId() {
        UI model = new UI();
        final Event ev = new Event().withId(22).withType("START").withWhen(432);
        loadEvents(model, ev);
        assertEquals("One event", 1, model.getRecords().size());
        loadEvents(model, ev);
        assertEquals("Still one event", 1, model.getRecords().size());
    }

    @Test
    public void onStartPersonIsTakenToNextStart() {
        UI model = new UI();
        Contact jarda = new Contact().withId(1).withName("Jarouš");
        Contact ondra = new Contact().withId(2).withName("Ondra");
        Contact anna = new Contact().withId(3).withName("Anna");
        Contact lazy = new Contact().withId(4).withName("Lazy");
        model.getContacts().add(jarda);
        model.getContacts().add(ondra);
        model.getContacts().add(anna);
        model.getContacts().add(lazy);

        assertEquals("No events so far", 0, model.getRecords().size());

        long now = System.currentTimeMillis();
        model.getNextOnStart().withContact(ondra);
        loadEvents(model, new Event().withId(1).withType("START").withWhen(now));

        assertEquals("One record now", 1, model.getRecords().size());
        assertEquals("Reference to Ondra", ondra.getId(), model.getRecords().get(0).getStart().getWho());
        assertEquals("Reference to Ondra", ondra, model.getRecords().get(0).getWho().getContact());
    }


    @Test
    public void onConnectButDuplicateStartAndFinish() {
        UI model = new UI();
        Contact jarda = new Contact().withId(1).withName("Jarouš");
        Contact ondra = new Contact().withId(2).withName("Ondra");
        Contact anna = new Contact().withId(3).withName("Anna");
        Contact lazy = new Contact().withId(4).withName("Lazy");
        model.getContacts().add(jarda);
        model.getContacts().add(ondra);
        model.getContacts().add(anna);
        model.getContacts().add(lazy);

        assertEquals("No events so far", 0, model.getRecords().size());

        long now = System.currentTimeMillis();
        model.setNextOnStart(new Avatar().withContact(ondra));
        final Event eventStart = new Event().withId(1).withType("START").withWhen(now).withWho(3);
        loadEvents(model, eventStart);

        final Event eventFinish = new Event().withId(2).withType("FINISH").withWhen(now + 13000);
        loadEvents(model, eventFinish);

        assertEquals("Two records exist", 2, model.getRecords().size());
        Record finishRecord = model.getRecords().get(0);
        assertEquals("FINISH", finishRecord.getFinish().getType());
        assertNull("No start", finishRecord.getStart());

        Record runRecord = model.getRecords().get(1);

        assertEquals("FINISH", runRecord.getFinish().getType());
        assertEquals("START", runRecord.getStart().getType());

        assertEquals(runRecord.getStart(), eventStart);
        assertEquals(runRecord.getFinish(), eventFinish);
        assertEquals(13000, runRecord.getLengthMillis());
        assertEquals("13:00", runRecord.getLength());
    }

    @Test
    public void onConnectStartAndFinishOnLoad() {
        UI model = new UI();
        Contact jarda = new Contact().withId(1).withName("Jarouš");
        Contact ondra = new Contact().withId(2).withName("Ondra");
        Contact anna = new Contact().withId(3).withName("Anna");
        Contact lazy = new Contact().withId(4).withName("Lazy");
        model.getContacts().add(jarda);
        model.getContacts().add(ondra);
        model.getContacts().add(anna);
        model.getContacts().add(lazy);

        assertEquals("No events so far", 0, model.getRecords().size());

        model.getNextOnStart().withContact(anna);

        long now = System.currentTimeMillis();
        {
            final Event eventStart = new Event().withId(1).withType("START").withWhen(now).withWho(3).withRef(2);
            final Event eventFinish = new Event().withId(2).withType("FINISH").withWhen(now + 13000).withRef(1);
            loadEvents(model, eventStart, eventFinish);

            assertEquals("Two records visible", 2, model.getRecords().size());
            Record finishRecord = model.getRecords().get(0);
            Record runRecord = model.getRecords().get(1);

            assertNull("No start", finishRecord.getStart());
            assertEquals("FINISH", finishRecord.getFinish().getType());

            assertEquals(eventStart, runRecord.getStart());
            assertEquals(eventFinish, runRecord.getFinish());
            assertEquals(13000, runRecord.getLengthMillis());
            assertEquals("13:00", runRecord.getLength());

            assertEquals(anna, runRecord.getWho().getContact());
        }


        model.getNextOnStart().withContact(ondra);
        {
            final Event eventStart = new Event().withId(3).withType("START").withWhen(now + 20000).withWho(2).withRef(4);
            final Event eventFinish = new Event().withId(4).withType("FINISH").withWhen(now + 27000).withRef(3);
            loadEvents(model, eventStart, eventFinish);
            assertEquals("Four records visible", 4, model.getRecords().size());
            Record runRecord = model.getRecords().get(1);

            assertEquals("START", runRecord.getStart().getType());
            assertEquals("FINISH", runRecord.getFinish().getType());

            assertEquals(eventStart, runRecord.getStart());
            assertEquals(eventFinish, runRecord.getFinish());
            assertEquals(7000, runRecord.getLengthMillis());
            assertEquals("07:00", runRecord.getLength());

            assertEquals(ondra, runRecord.getWho().getContact());
        }

    }

    @Test
    public void chooseFromMultipleFinishTimes() {
        UI model = new UI();
        Contact ondra = new Contact().withId(1).withName("Ondra");
        model.getContacts().add(ondra);

        long now = System.currentTimeMillis();
        final Event eventStart1 = new Event().withId(3).withType("START").withWhen(now + 20000).withWho(1);
        final Event eventStart2 = new Event().withId(4).withType("START").withWhen(now + 27000);
        final Event eventStart3 = new Event().withId(5).withType("START").withWhen(now + 33000);
        final Event eventFinish = new Event().withId(6).withType("FINISH").withWhen(now + 39000);
        loadEvents(model, eventStart1, eventStart2, eventStart3, eventFinish);

        assertEquals("Three starts and three finish records", 4, model.getRecords().size());
        final Record startRecord = model.getRecords().get(3);

        assertEquals("Start event", eventStart1, startRecord.getStart());
        assertEquals("1st finish used", eventFinish, startRecord.getFinish());

        assertEquals("The last finish chooses earliest start", "19:00", startRecord.getLength());

        assertNull("Second start hasn't finished yet", model.getRecords().get(2).getFinish());
        assertNull("3rd start hasn't finished yet", model.getRecords().get(1).getFinish());
    }

    @Test
    public void cannotFinishSoonerThanStarted() {
        UI model = new UI();
        Contact ondra = new Contact().withId(1).withName("Ondra");
        model.getContacts().add(ondra);

        long now = System.currentTimeMillis();
        final Event eventFinish = new Event().withId(3).withType("FINISH").withWhen(now + 20000).withWho(1);
        final Event eventStart = new Event().withId(4).withType("START").withWhen(now + 27000);
        loadEvents(model, eventFinish, eventStart);

        assertEquals("Two records", 2, model.getRecords().size());
        final Record startRecord = model.getRecords().get(0);

        assertEquals("Start event", eventStart, startRecord.getStart());
        assertNull("No finish yet", startRecord.getFinish());
    }

    @Test
    public void continueExistingRide() throws Exception {
        UI ui = new UI();
        InputStream is = new ByteArrayInputStream(
("[\n" +
"    {\n" +
"        \"id\": 7,\n" +
"        \"when\": 1547433305864,\n" +
"        \"type\": \"IGNORE\",\n" +
"        \"ref\": 6,\n" +
"        \"who\": 0\n" +
"    }, {\n" +
"        \"id\": 6,\n" +
"        \"when\": 1547433061965,\n" +
"        \"type\": \"FINISH\",\n" +
"        \"ref\": 0,\n" +
"        \"who\": 0\n" +
"    }, {\n" +
"        \"id\": 5,\n" +
"        \"when\": 1547409752796,\n" +
"        \"type\": \"ASSIGN\",\n" +
"        \"ref\": 4,\n" +
"        \"who\": 1\n" +
"    }, {\n" +
"        \"id\": 4,\n" +
"        \"when\": 1547409299881,\n" +
"        \"type\": \"START\",\n" +
"        \"ref\": 0,\n" +
"        \"who\": 0\n" +
"    }, {\n" +
"        \"id\": 3,\n" +
"        \"when\": 1547409180432,\n" +
"        \"type\": \"ASSIGN\",\n" +
"        \"ref\": 2,\n" +
"        \"who\": 1\n" +
"    }, {\n" +
"        \"id\": 2,\n" +
"        \"when\": 1547409180412,\n" +
"        \"type\": \"START\",\n" +
"        \"ref\": 0,\n" +
"        \"who\": 0\n" +
"    }, {\n" +
"        \"id\": 1,\n" +
"        \"when\": 1547409137637,\n" +
"        \"type\": \"INITIALIZED\",\n" +
"        \"ref\": 0,\n" +
"        \"who\": 0\n" +
"    }\n" +
"]").getBytes("UTF-8")
        );

        List<Event> arr = new ArrayList<>();
        final BrwsrCtx ctx = BrwsrCtx.findDefault(ui.getClass());
        Models.parse(ctx, Event.class, is, arr);

        assertEquals("Seven elements: " + arr, 7, arr.size());

        List<Record> res = Arrays.asList(RecordModel.compute(ui, arr, 10, true));
        assertEquals("Two starts left: " + res, 2, res.size());
    }

    @Test
    public void timeWithDifferentZone() {
        Random random = new Random();
        final int offset = random.nextInt(10) - 5;
        assertSecondsHundreds(offset);
    }

    @Test
    public void timeWithSameZone() {
        assertSecondsHundreds(0);
    }

    @Test
    public void timeWithMinusThreeZone() {
        assertSecondsHundreds(-3);
    }

    private void assertSecondsHundreds(final int offset) {
        Current c = new Current();
        final long now = System.currentTimeMillis();
        c.setMillis(now);
        Record r = new Record();
        r.setCurrent(c);

        long hour = 3600 * 1000 * offset;
        r.getStart().setWhen(now - hour - 1432);

        assertEquals("One second (" + offset + ")", "01", r.getSeconds());
        assertEquals("Hundreds (" + offset + ")", "43", r.getHundreds());
    }
}
