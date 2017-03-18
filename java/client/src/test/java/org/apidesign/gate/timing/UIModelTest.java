package org.apidesign.gate.timing;

import java.util.Arrays;
import org.apidesign.gate.timing.shared.Contact;
import net.java.html.junit.BrowserRunner;
import org.apidesign.gate.timing.shared.Event;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
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
        UIModel.onRecordsChangeUpdateWho(model);
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
        final Event eventStart = new Event().withId(3).withType("START").withWhen(now + 20000).withWho(1);
        final Event eventFinish1 = new Event().withId(4).withType("FINISH").withWhen(now + 27000);
        final Event eventFinish2 = new Event().withId(5).withType("FINISH").withWhen(now + 33000);
        final Event eventFinish3 = new Event().withId(6).withType("FINISH").withWhen(now + 39000);
        loadEvents(model, eventStart, eventFinish1, eventFinish2, eventFinish3);

        assertEquals("One start and three finish records", 4, model.getRecords().size());
        final Record startRecord = model.getRecords().get(3);

        assertEquals("Start event", eventStart, startRecord.getStart());
        assertEquals("1st finish used", eventFinish1, startRecord.getFinish());
        assertNull("No earlier finish", startRecord.getPrev());
        assertEquals("00:00", startRecord.getPrevTime());
        assertEquals("Next finish available", eventFinish2, startRecord.getNext());
        assertEquals("13:00", startRecord.getNextTime());

        UIModel.nextRecord(model, startRecord);

        assertEquals("2nd finish used", eventFinish2, startRecord.getFinish());
        assertEquals("First finish available", eventFinish1, startRecord.getPrev());
        assertEquals("Last finish available", eventFinish3, startRecord.getNext());
        assertEquals("07:00", startRecord.getPrevTime());
        assertEquals("19:00", startRecord.getNextTime());

        UIModel.nextRecord(model, startRecord);

        assertEquals("3rd finish used", eventFinish3, startRecord.getFinish());
        assertEquals("2nd is previous", eventFinish2, startRecord.getPrev());
        assertNull("No next ", startRecord.getNext());
        assertEquals("13:00", startRecord.getPrevTime());
        assertEquals("00:00", startRecord.getNextTime());

        UIModel.prevRecord(model, startRecord);

        assertEquals("2nd finish used again", eventFinish2, startRecord.getFinish());
        assertEquals("First finish available again", eventFinish1, startRecord.getPrev());
        assertEquals("Last finish available again", eventFinish3, startRecord.getNext());

    }
}
