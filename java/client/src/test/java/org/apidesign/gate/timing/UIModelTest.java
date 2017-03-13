package org.apidesign.gate.timing;

import java.util.Arrays;
import java.util.Collections;
import org.apidesign.gate.timing.shared.Contact;
import net.java.html.junit.BrowserRunner;
import org.apidesign.gate.timing.shared.Event;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests for behavior of your application in isolation. Verify
 * behavior of your MVVC code in a unit test.
  */
@RunWith(BrowserRunner.class)
public class UIModelTest {
    @Test public void addNewSetsEdited() {
        UI model = new UI();
        Contact c = new Contact();
        UIModel.editContact(model, c);
        assertEquals("c is now edited", model.getEdited(), c);
    }

    @Test
    public void ignoringAnEvent() {
        UI model = new UI();
        UIModel.loadEvents(model,
            Collections.nCopies(1, new Event().withId(22).withType("DATA").withWhen(432)),
            false
        );
        assertEquals("One event", 1, model.getRecords().size());
        UIModel.loadEvents(model,
            Collections.nCopies(1, new Event().withId(23).withType("DATA").withWhen(433)),
            false
        );
        assertEquals("Two events", 2, model.getRecords().size());

        UIModel.loadEvents(model,
            Collections.nCopies(1, new Event().withId(24).withType("IGNORE").withWhen(455).withRef(22)),
            false
        );
        assertEquals("One event again", 1, model.getRecords().size());
    }

    @Test
    public void oneEventPerId() {
        UI model = new UI();
        final Event ev = new Event().withId(22).withType("DATA").withWhen(432);
        UIModel.loadEvents(model,
            Collections.nCopies(1, ev), false
        );
        assertEquals("One event", 1, model.getRecords().size());
        UIModel.loadEvents(model,
            Collections.nCopies(1, ev), false
        );
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
        model.setNextOnStart(new Avatar().withContact(ondra));
        UIModel.loadEvents(model, Collections.nCopies(1,
            new Event().withId(1).withType("START").withWhen(now)
        ), false);

        assertEquals("One record now", 1, model.getRecords().size());
        assertEquals("Reference to Ondra", ondra.getId(), model.getRecords().get(0).getStart().getWho());
        assertEquals("Reference to Ondra", ondra, model.getRecords().get(0).getWho().getContact());
    }


    @Test
    public void onConnectStartAndFinish() {
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
        UIModel.loadEvents(model, Arrays.asList(eventStart), false);

        final Event eventFinish = new Event().withId(2).withType("FINISH").withWhen(now + 13000);
        UIModel.loadEvents(model, Arrays.asList(eventFinish), false);

        assertEquals("Two events visible", 2, model.getRecords().size());
        Record finishRecord = model.getRecords().get(0);
        Record runRecord = model.getRecords().get(1);

        assertEquals("FINISH", finishRecord.getStart().getType());
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

        long now = System.currentTimeMillis();
        final Event eventStart = new Event().withId(1).withType("START").withWhen(now).withWho(3).withRef(2);
        final Event eventFinish = new Event().withId(2).withType("FINISH").withWhen(now + 13000).withRef(1);
        UIModel.loadEvents(model, Arrays.asList(eventStart, eventFinish), false);

        assertEquals("Two events visible", 2, model.getRecords().size());
        Record finishRecord = model.getRecords().get(0);
        Record runRecord = model.getRecords().get(1);

        assertEquals("FINISH", finishRecord.getStart().getType());
        assertEquals("START", runRecord.getStart().getType());

        assertEquals(eventStart, runRecord.getStart());
        assertEquals(eventFinish, runRecord.getFinish());
        assertEquals(13000, runRecord.getLengthMillis());
        assertEquals("13:00", runRecord.getLength());

        assertEquals(anna, runRecord.getWho().getContact());
    }
}
