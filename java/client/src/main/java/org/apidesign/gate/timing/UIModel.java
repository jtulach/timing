package org.apidesign.gate.timing;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import org.apidesign.gate.timing.js.Dialogs;
import org.apidesign.gate.timing.shared.Contact;
import org.apidesign.gate.timing.shared.Event;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import net.java.html.json.ComputedProperty;
import net.java.html.json.Function;
import net.java.html.json.Model;
import net.java.html.json.ModelOperation;
import net.java.html.json.OnPropertyChange;
import net.java.html.json.OnReceive;
import net.java.html.json.Property;

@Model(className = "UI", targetId="", builder="with", properties = {
    @Property(name = "url", type = String.class),
    @Property(name = "message", type = String.class),
    @Property(name = "alert", type = boolean.class),

    @Property(name = "choose", type = Avatar.class),
    @Property(name = "contacts", type = Contact.class, array = true),
    @Property(name = "selected", type = Contact.class),
    @Property(name = "edited", type = Contact.class),

    @Property(name = "nextOnStart", type = Avatar.class),
    @Property(name = "orderOnStart", type = Contact.class, array = true),

    @Property(name = "records", type = Record.class, array = true),
})
final class UIModel {

    @ComputedProperty
    static boolean showEvents(Contact edited, Avatar choose) {
        return choose == null && edited == null;
    }

    @ComputedProperty
    static boolean showContacts(Contact edited, Avatar choose) {
        return choose != null && edited == null;
    }

    @Function
    static void chooseContact(UI model, Avatar data) {
        model.setChoose(data);
    }

    @Function
    static void contactSelected(UI model, Contact data) {
        if (model.getChoose() != null) {
            model.getChoose().setContact(data);
        }
        model.setChoose(null);
    }

    @ModelOperation
    static void onStartEvent(UI model, Event ev) {
        if (ev.getWho() <= 0 && model.getNextOnStart() != null && model.getNextOnStart().getContact() != null) {
            ev.setWho(model.getNextOnStart().getContact().getId());
            model.getNextOnStart().setContact(null);
            model.updateWhoRef(model.getUrl(), "" + ev.getId(), "" + ev.getWho(), "0");
        }
    }

    @ModelOperation
    static void onFinishEvent(UI model, Event finish, Stack<Record> startList) {
        if (finish.getWho() <= 0 && !startList.isEmpty()) {
            Record start = startList.pop();
            Event startEvent = start.getStart();
            finish.setWho(startEvent.getWho());
            finish.setRef(startEvent.getId());
            start.setFinish(finish);
            startEvent.setRef(finish.getId());
            model.updateWhoRef(model.getUrl(), "" + finish.getId(), "" + finish.getWho(), "" + finish.getRef());
            model.updateWhoRef(model.getUrl(), "" + startEvent.getId(), "" + startEvent.getWho(), "" + startEvent.getRef());
        }
    }

    //
    // REST API callbacks
    //

    @OnReceive(url = "{url}?newerThan={since}", onError = "cannotConnect")
    static void loadEvents(UI ui, List<Event> arr, boolean reattach) {
        TreeSet<Record> all = new TreeSet<>(RecordModel.COMPARATOR);
        Stack<Record> unassignedStart = new Stack<>();
        for (Record r : ui.getRecords()) {
            all.add(r);
            if ("START".equals(r.getStart().getType()) && r.getStart().getRef() <= 0) {
                unassignedStart.add(r);
            }
        }
        for (Event newEvent : arr) {
            final Record r = new Record().withStart(newEvent).withFinish(null).withWho(null);
            if ("START".equals(newEvent.getType())) {
                ui.onStartEvent(newEvent);
                if (newEvent.getRef() <= 0) {
                    unassignedStart.add(r);
                }
            }
            if ("FINISH".equals(newEvent.getType())) {
                onFinishEvent(ui, newEvent, unassignedStart);
            }
            all.add(r);
        }

        long newest = all.isEmpty() ? 1 : all.first().getStart().getWhen();
        
        Set<Integer> toDelete = new HashSet<>();
        Iterator<Record> it = all.iterator();
        while (it.hasNext()) {
            final Record r = it.next();
            final Event ev = r.getStart();
            if ("IGNORE".equals(ev.getType())) {
                toDelete.add(ev.getRef());
                it.remove();
            } else if (toDelete.contains(ev.getId())) {
                it.remove();
            } else {
                if ("START".equals(ev.getType())) {
                    r.setFinish(findEvent(all, ev.getRef()));
                }
                if (r.getWho() == null) {
                    r.withWho(new Avatar().withContact(findContact(ui.getContacts(), r.getStart().getWho())));
                }
            }
        }

        int size = Math.min(10, all.size());
        Record[] newRecords = new Record[size];
        int i = 0;
        for (Record r : all) {
            if (i < newRecords.length) {
                newRecords[i++] = r;
            }
        }
        ui.withRecords(newRecords);
        ui.setMessage("Máme tu " + newRecords.length + " události.");

        if (reattach) {
            ui.loadEvents(ui.getUrl(), "" + newest, true);
        }
    }

    @OnReceive(url = "{url}/add?type={type}&ref={ref}", onError = "cannotConnect")
    static void sendEvent(UI ui, Event reply) {
        loadEvents(ui, Collections.nCopies(1, reply), false);
    }

    @OnReceive(url = "{url}/assign?event={id}&who={who}&ref={ref}")
    static void updateWhoRef(UI ui, Event reply) {
        loadEvents(ui, Collections.nCopies(1, reply), false);
    }

    @OnReceive(url = "{url}/contacts", onError = "cannotConnect")
    static void loadContacts(UI ui, List<Contact> arr, String alsoEventsFrom, boolean alsoReattach) {
        ui.getContacts().clear();
        ui.getContacts().addAll(arr);
        ui.setMessage("Máme tu " + arr.size() + " závodníků.");
        if (alsoEventsFrom != null) {
            ui.loadEvents(ui.getUrl(), alsoEventsFrom, alsoReattach);
        }
    }

    @OnReceive(method = "POST", url = "{url}/contacts", data = Contact.class, onError = "cannotConnect")
    static void addContact(UI ui, List<Contact> updatedOnes, Contact newOne) {
        ui.getContacts().clear();
        ui.getContacts().addAll(updatedOnes);
        ui.setMessage("Created " + newOne.getName() + ". " + updatedOnes.size() + " contact(s) now.");
        ui.setSelected(null);
        ui.setEdited(null);
    }
    @OnReceive(method = "PUT", url = "{url}/contacts/{id}", data = Contact.class, onError = "cannotConnect")
    static void updateContact(UI ui, List<Contact> updatedOnes, Contact original) {
        ui.getContacts().clear();
        ui.getContacts().addAll(updatedOnes);
        ui.setMessage("Updated " + original.getName() + ". " + updatedOnes.size() + " contact(s) now.");
        ui.setSelected(null);
        ui.setEdited(null);
    }

    @OnReceive(method = "DELETE", url = "{url}/contacts/{id}", onError = "cannotConnect")
    static void deleteContact(UI ui, List<Contact> remainingOnes, Contact original) {
        ui.getContacts().clear();
        ui.getContacts().addAll(remainingOnes);
        ui.setMessage("Deleted " + original.getName() + ". " + remainingOnes.size() + " contact(s) now.");
    }

    static void cannotConnect(UI data, Exception ex) {
        if (data.getUrl().contains("localhost")) {
            data.setUrl("http://skimb.xelfi.cz/timing/");
            data.connect();
        } else {
            data.setMessage("Spojení odmítnuto: " + ex.getMessage());
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
        data.loadContacts(data.getUrl(), "0", true);
    }

    @Function static void addContact(UI ui) {
        ui.setSelected(null);
        final Contact c = new Contact();
        ui.setEdited(c);
    }

    @Function static void editContact(UI ui, Contact data) {
        ui.setSelected(data);
        ui.setEdited(data.clone());
    }

    @Function static void deleteContact(UI ui, Contact data) {
        ui.deleteContact(ui.getUrl(), "" + data.getId(), data);
    }

    @Function static void ignoreEvent(UI ui, Record data) {
        ui.sendEvent(ui.getUrl(), "IGNORE", "" + data.getStart().getId());
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
            ui.updateContact(ui.getUrl(), "" + s.getId(), e, e);
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
        uiModel.setChoose(null);
        uiModel.applyBindings();
        uiModel.connect();
    }

    private static Contact findContact(List<Contact> contacts, int who) {
        if (who <= 0) {
            return null;
        } else {
            for (Contact c : contacts) {
                if (c.getId() == who) {
                    return c;
                }
            }
            return null;
        }
    }

    private static Event findEvent(Collection<Record> records, int id) {
        if (id <= 0) {
            return null;
        } else {
            for (Record r : records) {
                if (r.getStart().getId() == id) {
                    return r.getStart();
                }
            }
            return null;
        }
    }

}
