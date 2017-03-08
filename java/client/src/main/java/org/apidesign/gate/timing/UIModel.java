package org.apidesign.gate.timing;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import org.apidesign.gate.timing.js.Dialogs;
import org.apidesign.gate.timing.shared.Contact;
import org.apidesign.gate.timing.shared.Events;
import org.apidesign.gate.timing.shared.Event;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import net.java.html.json.ComputedProperty;
import net.java.html.json.Function;
import net.java.html.json.Model;
import net.java.html.json.ModelOperation;
import net.java.html.json.OnPropertyChange;
import net.java.html.json.OnReceive;
import net.java.html.json.Property;

/** Generates UI class that provides the application logic model for
 * the HTML page.
 */
@Model(className = "UI", targetId="", properties = {
    @Property(name = "url", type = String.class),
    @Property(name = "message", type = String.class),
    @Property(name = "alert", type = boolean.class),
    @Property(name = "events", type = Event.class, array = true),
    @Property(name = "contacts", type = Contact.class, array = true),
    @Property(name = "selected", type = Contact.class),
    @Property(name = "edited", type = Contact.class),
    @Property(name = "showContacts", type = boolean.class)
})
final class UIModel {

    @ComputedProperty
    static boolean showEvents(Contact edited, boolean showContacts) {
        return !showContacts && edited == null;
    }

    //
    // REST API callbacks
    //

    @OnReceive(url = "{url}?newerThan={since}", onError = "cannotConnect")
    static void loadEvents(UI ui, List<Event> arr, boolean reattach) {
        TreeSet<Event> all = new TreeSet<>(Events.COMPARATOR);
        all.addAll(ui.getEvents());
        all.addAll(arr);

        long newest = all.isEmpty() ? 1 : all.first().getWhen();
        
        Set<Integer> toDelete = new HashSet<>();
        Iterator<Event> it = all.iterator();
        while (it.hasNext()) {
            Event ev = it.next();
            if ("IGNORE".equals(ev.getType())) {
                toDelete.add(ev.getRef());
                it.remove();
            } else if (toDelete.contains(ev.getId())) {
                it.remove();
            }
        }

        ui.getEvents().clear();
        ui.getEvents().addAll(all);
        ui.setMessage("Máme tu " + all.size() + " události.");

        if (reattach) {
            ui.loadEvents(ui.getUrl(), "" + newest, true);
        }
    }

    @OnReceive(url = "{url}/add?type={type}&ref={ref}", onError = "cannotConnect")
    static void sendEvent(UI ui, Event reply) {
        loadEvents(ui, Collections.nCopies(1, reply), false);
    }

    @OnReceive(method = "POST", url = "{url}", data = Contact.class, onError = "cannotConnect")
    static void addContact(UI ui, List<Contact> updatedOnes, Contact newOne) {
        ui.getContacts().clear();
        ui.getContacts().addAll(updatedOnes);
        ui.setMessage("Created " + newOne.getLastName() + ". " + updatedOnes.size() + " contact(s) now.");
        ui.setSelected(null);
        ui.setEdited(null);
    }
    @OnReceive(method = "PUT", url = "{url}/{id}", data = Contact.class, onError = "cannotConnect")
    static void updateContact(UI ui, List<Contact> updatedOnes, Contact original) {
        ui.getContacts().clear();
        ui.getContacts().addAll(updatedOnes);
        ui.setMessage("Updated " + original.getLastName() + ". " + updatedOnes.size() + " contact(s) now.");
        ui.setSelected(null);
        ui.setEdited(null);
    }

    @OnReceive(method = "DELETE", url = "{url}/{id}", onError = "cannotConnect")
    static void deleteContact(UI ui, List<Contact> remainingOnes, Contact original) {
        ui.getContacts().clear();
        ui.getContacts().addAll(remainingOnes);
        ui.setMessage("Deleted " + original.getLastName() + ". " + remainingOnes.size() + " contact(s) now.");
    }

    static void cannotConnect(UI data, Exception ex) {
        data.setMessage("Cannot connect " + ex.getMessage() + ". Should not you start the server project first?");
        if (data.getUrl().contains("localhost")) {
            data.setUrl("http://skimb.xelfi.cz/timing/");
            data.connect();
        }
    }

    //
    // UI callback bindings
    //

    @ModelOperation @Function static void connect(UI data) {
        final String u = data.getUrl();
        if (u.endsWith("/")) {
            data.setUrl(u.substring(0, u.length() - 1));
        }
        data.loadEvents(data.getUrl(), "0", true);
    }

    @Function static void addNew(UI ui) {
        ui.setSelected(null);
        final Contact c = new Contact();
        ui.setEdited(c);
    }

    @Function static void edit(UI ui, Contact data) {
        ui.setSelected(data);
        ui.setEdited(data.clone());
    }

    @Function static void delete(UI ui, Contact data) {
        ui.deleteContact(ui.getUrl(), data.getId(), data);
    }

    @Function static void ignore(UI ui, Event data) {
        ui.sendEvent(ui.getUrl(), "IGNORE", "" + data.getId());
    }

    @Function static void cancel(UI ui) {
        ui.setEdited(null);
        ui.setSelected(null);
    }

    @Function static void commit(UI ui) {
        final Contact e = ui.getEdited();
        if (e == null) {
            return;
        }
        String invalid = null;
        if (e.getValidate() != null) {
            invalid = e.getValidate();
        }
        if (invalid != null && !Dialogs.confirm("Not all data are valid (" +
                invalid + "). Do you want to proceed?", null
        )) {
            return;
        }

        final Contact s = ui.getSelected();
        if (s != null) {
            ui.updateContact(ui.getUrl(), s.getId(), e, e);
        } else {
            ui.addContact(ui.getUrl(), e, e);
        }
    }

    @Function static void addPhoneEdited(UI ui) {
    }

    @Function static void removePhoneEdited(UI ui, String data) {
    }
    
    @Function static void hideAlert(UI ui) {
        ui.setAlert(false);
    }
    
    @OnPropertyChange(value = "message") static void messageChanged(UI ui) {
        ui.setAlert(true);
    }    

    private static UI uiModel;
    /**
     * Called when the page is ready.
     */
    static void onPageLoad() throws Exception {
        uiModel = new UI();
        final String baseUrl = "http://localhost:8080/timing/";
        uiModel.setUrl(baseUrl);
        uiModel.setEdited(null);
        uiModel.setSelected(null);
        uiModel.applyBindings();
        uiModel.connect();
    }

}
