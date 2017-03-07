package org.apidesign.gate.timing;

import java.util.Collections;
import org.apidesign.gate.timing.shared.Contact;
import org.apidesign.gate.timing.shared.PhoneType;
import net.java.html.junit.BrowserRunner;
import org.apidesign.gate.timing.shared.Event;
import static org.junit.Assert.assertTrue;
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
        UIModel.edit(model, c);
        assertEquals("c is now edited", model.getEdited(), c);

        assertTrue("No phone yet", model.getEdited().getPhones().isEmpty());
        UIModel.addPhoneEdited(model);
        assertEquals("One phone added", model.getEdited().getPhones().size(), 1);
        assertEquals("First is home phone", model.getEdited().getPhones().get(0).getType(), PhoneType.HOME);

        UIModel.addPhoneEdited(model);
        assertEquals("2nd phone added", model.getEdited().getPhones().size(), 2);
        assertEquals("2nd is work phone", model.getEdited().getPhones().get(1).getType(), PhoneType.WORK);
    }

    @Test
    public void ignoringAnEvent() {
        UI model = new UI();
        UIModel.loadEvents(model, Collections.nCopies(1, new Event().withId(22).withType("DATA").withWhen(432)));
        assertEquals("One event", 1, model.getEvents().size());
        UIModel.loadEvents(model, Collections.nCopies(1, new Event().withId(23).withType("DATA").withWhen(433)));
        assertEquals("Two events", 2, model.getEvents().size());

        UIModel.loadEvents(model, Collections.nCopies(1, new Event().withId(24).withType("IGNORE").withWhen(455).withRef(22)));
        assertEquals("One event again", 1, model.getEvents().size());
    }
}
