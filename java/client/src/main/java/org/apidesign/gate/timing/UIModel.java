package org.apidesign.gate.timing;

import java.util.Iterator;
import org.apidesign.gate.timing.js.Dialogs;
import org.apidesign.gate.timing.shared.Contact;
import org.apidesign.gate.timing.shared.Event;
import java.util.List;
import java.util.Objects;
import net.java.html.json.ComputedProperty;
import net.java.html.json.Function;
import net.java.html.json.Model;
import net.java.html.json.ModelOperation;
import net.java.html.json.Models;
import net.java.html.json.OnPropertyChange;
import net.java.html.json.OnReceive;
import net.java.html.json.Property;
import org.apidesign.gate.timing.shared.Run;
import org.apidesign.gate.timing.shared.Running;

@Model(className = "UI", targetId="", builder="with", instance = true, properties = {
    @Property(name = "url", type = String.class),
    @Property(name = "message", type = String.class),
    @Property(name = "alert", type = boolean.class),

    @Property(name = "current", type = Current.class),

    @Property(name = "choose", type = Avatar.class),
    @Property(name = "contacts", type = Contact.class, array = true),
    @Property(name = "selected", type = Contact.class),
    @Property(name = "edited", type = Contact.class),

    @Property(name = "nextOnStart", type = Avatar.class),

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
    static void chooseContact(UI ui, Avatar data) {
        ui.setChoose(data);
        data.onSelect(() -> {
            if (ui.getNextOnStart() == data) {
                ui.updateWhoRef(ui.getUrl(), "" + data.getContact().getId(), "-1", ui.getUrl());
                return;
            }
            for (Record r : ui.getRecords()) {
                if (r.getWho() == data && data.getContact() != null) {
                    int who = r.getWho().getContact().getId();
                    if (who <= 0) {
                        continue;
                    }
                    if (r.getFinish() != null) {
                        ui.updateWhoRef(ui.getUrl(), "" + who, "" + r.getFinish().getId(), ui.getUrl());
                    }
                    if (r.getStart() != null) {
                        ui.updateWhoRef(ui.getUrl(), "" + who, "" + r.getStart().getId(), ui.getUrl());
                    }
                }
            }
        });
    }

    @Function
    static void contactSelected(UI model, Contact data) {
        if (model.getChoose() != null) {
            model.getChoose().setContact(data);
        }
        model.setChoose(null);
    }

    @Function
    static void continueTimer(UI model, Record data) {
        Event f = data.getFinish();
        if (f != null) {
            data.getRun().setIgnore(true);
            data.getRun().setFinish(null);
            model.sendEvent(model.getUrl(), "IGNORE", "" + f.getId(), model.getUrl());
        }
    }

    @ModelOperation
    static void selectNextOnStart(UI ui, int id) {
        if (id >= 0) {
            for (Contact c : ui.getContacts()) {
                if (c.getId() == id) {
                    ui.getNextOnStart().setContact(c);
                    break;
                }
            }
        } else {
            ui.getNextOnStart().setContact(null);
        }
    }

    //
    // REST API callbacks
    //

    @OnReceive(url = "{url}/runs", onError = "cannotConnect")
    static void loadRuns(UI ui, Running runs, String previousURL) {
        List<Record> oldRecords = ui.getRecords();
        List<Record> newRecords = Models.asList();
        int currentId = Integer.MAX_VALUE;
        for (Run run : runs.getRuns()) {
            Record record = new Record().withCurrent(ui.getCurrent()).withRun(run);
            record.findWhoAvatar(ui.getContacts());
            newRecords.add(record);
            currentId = run.getId();
        }
        for (Record record : oldRecords) {
            if (record.getRun().getId() < currentId) {
                newRecords.add(record);
            }
        }
        markFirstFinished(newRecords);
        final Record[] result = newRecords.toArray(new Record[newRecords.size()]);
        ui.withRecords(result);
        ui.selectNextOnStart(runs.getStarting());
        ui.setMessage("Máme tu " + result.length + " jízd.");
        ui.checkPendingRuns(runs.getTimestamp(), previousURL);
    }

    static void markFirstFinished(List<Record> newRecords) {
        boolean foundFirst = false;
        for (Record record : newRecords) {
            if (!foundFirst && record.getFinish() != null) {
                record.setFirstFinished(true);
                foundFirst = true;
                continue;
            }
            record.setFirstFinished(false);
        }
    }

    @OnReceive(url = "{url}/add?type={type}&ref={ref}", onError = "cannotConnect")
    static void sendEvent(UI ui, Event reply, String previousURL) {
        ui.loadRuns(ui.getUrl(), previousURL);
    }

    @OnReceive(url = "{url}/add?type=ASSIGN&who={who}&ref={ref}", onError = "cannotConnect")
    static void updateWhoRef(UI ui, Event reply, String previousURL) {
        ui.loadRuns(ui.getUrl(), previousURL);
    }

    @OnReceive(url = "{url}/contacts", onError = "cannotConnect")
    static void loadContacts(UI ui, Contact[] arr, String previousURL) {
        ui.withContacts(arr);
        ui.setMessage("Máme tu " + arr.length + " závodníků.");
        ui.loadRuns(ui.getUrl(), previousURL);
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
    // Pending events
    //

    @ModelOperation
    static void checkPendingRuns(UI model, long timestamp, String previousURL) {
        if (Objects.equals(model.getUrl(), previousURL)) {
            model.loadPendingRuns(previousURL, String.valueOf(timestamp), previousURL);
        } else {
            if (previousURL != null) {
                cannotLoadPending(model, new Exception());
            }
        }
    }

    @OnReceive(url = "{url}/runs?newerThan={since}", onError = "cannotLoadPending")
    static void loadPendingRuns(UI model, Running runs, String previousUrl) {
        loadRuns(model, runs, previousUrl);
    }

    static void cannotLoadPending(UI model, Exception ex) {
        model.setMessage("Poslouchání na změnách selhalo. " + ex.getMessage() + " - načti ručně.");
    }

    //
    // UI callback bindings
    //

    @ModelOperation @Function static void connect(UI data) {
        final String u = data.getUrl();
        if (u.endsWith("/")) {
            data.setUrl(u.substring(0, u.length() - 1));
        }
        data.loadContacts(data.getUrl(), data.getUrl());
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
        if (data.getStart() != null) {
            ui.sendEvent(ui.getUrl(), "IGNORE", "" + data.getStart().getId(), ui.getUrl());
        }
        if (data.getFinish()!= null) {
            ui.sendEvent(ui.getUrl(), "IGNORE", "" + data.getFinish().getId(), ui.getUrl());
        }
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

        contactSelected(ui, e);
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
        uiModel.getCurrent().start();
    }

}
