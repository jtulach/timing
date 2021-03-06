package org.apidesign.gate.timing;

import java.util.Collections;
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
import org.apidesign.gate.timing.shared.Contacts;
import org.apidesign.gate.timing.shared.Run;
import org.apidesign.gate.timing.shared.Running;
import org.apidesign.gate.timing.shared.Settings;

@Model(className = "UI", targetId="", builder="with", instance = true, properties = {
    @Property(name = "url", type = String.class),
    @Property(name = "message", type = String.class),
    @Property(name = "alert", type = boolean.class),

    @Property(name = "current", type = Current.class),

    @Property(name = "choose", type = Avatar.class),
    @Property(name = "contacts", type = Contact.class, array = true),
    @Property(name = "selected", type = Contact.class),
    @Property(name = "edited", type = Contact.class),
    @Property(name = "config", type = Config.class),

    @Property(name = "nextOnStart", type = Avatar.class),

    @Property(name = "records", type = Record.class, array = true),
    @Property(name = "settings", type = Settings.class),
})
final class UIModel {
    private Connection currentConnection;

    @ComputedProperty
    static boolean showEvents(Object config, Contact edited, Avatar choose) {
        return config == null && choose == null && edited == null;
    }

    @ComputedProperty
    static boolean showContacts(Object config, Contact edited, Avatar choose) {
        return config == null && choose != null && edited == null;
    }

    @ComputedProperty
    static boolean showConnect(List<Contact> contacts, Avatar choose) {
        return choose != null && !contacts.contains(choose.getContact());
    }

    @ComputedProperty
    static String resultUrl(String url) {
        return url + "/výsledky.xlsx";
    }

    @ComputedProperty
    static String name(Settings settings) {
        return settings.getName();
    }

    @ComputedProperty
    static List<String> measurements(Settings settings) {
        return settings.getMeasurements();
    }

    @Function
    static void selectMeasurement(UI ui, String data) {
        ui.selectConfig(ui.getUrl(), data);
    }

    @Function
    static void setup(UI ui) {
        Settings s = ui.getSettings();

        Config c = new Config().withUi(ui);
        if (s != null) {
            c.setName(s.getName());
            c.setDate(s.getDate());
            c.setMax(s.getMax());
            c.setMin(s.getMin());
        }
        ui.setConfig(c);
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
    static void contactConnect(UI model, Contact data) {
        if (model.getChoose() != null) {
            Contact original = data.clone();
            Contact unknownContext = model.getChoose().getContact();
            data.getAliases().add(unknownContext.getName());
            model.getChoose().setContact(data);
            model.updateContact(model.getUrl(), "" + data.getId(), data, original);
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
        Contact selected;
        FOUND: if (id >= 0) {
            selected = Contacts.findById(ui.getContacts(), id);
            if (selected == null) {
                selected = new Contact().withId(id).withName("" + id);
            }
        } else {
            selected = null;
        }
        ui.getNextOnStart().withContact(selected);
    }

    //
    // REST API callbacks
    //

    @OnReceive(url = "{url}/runs", onError = "cannotConnect")
    static void loadRuns(UI ui, Running runs, Connection conn) {
        List<Record> oldRecords = ui.getRecords();
        List<Record> newRecords = Models.asList();
        int currentId = Integer.MAX_VALUE;
        List<Contact> used = Models.asList();
        for (Run run : runs.getRuns()) {
            Record record = new Record().withCurrent(ui.getCurrent()).withRun(run);
            record.findWhoAvatar(ui.getContacts());
            newRecords.add(record);
            addUsed(used, record.getWho());
            currentId = run.getId();
        }
        for (Record record : oldRecords) {
            if (record.getRun().getId() < currentId) {
                newRecords.add(record);
            }
        }
        long latest = markFirstFinished(newRecords);
        final Record[] result = newRecords.toArray(new Record[newRecords.size()]);
        ui.withRecords(result);
        Settings s = runs.getSettings();
        if (s != null) {
            ui.withSettings(s);
        }
        ui.selectNextOnStart(runs.getStarting());
        long since = System.currentTimeMillis() - latest;
        long sinceHours = since / (3600L * 1000);
        if (sinceHours > 4 && runs.getRuns().size() > 4) {
            ui.setMessage("Žádná jízda za posledních " + sinceHours + " hodin.");
            setup(ui);
        } else {
            ui.setMessage("Máme tu " + result.length + " jízd.");
        }

        List<Contact> newOrder = Models.asList();
        newOrder.addAll(ui.getContacts());
        Collections.sort(newOrder, (c1, c2) -> {
            int nameOrder = c1.getName() == null ? 1 : c1.getName().compareTo(c2.getName());
            if (nameOrder == 0) {
                return c1.getId() - c2.getId();
            }
            return nameOrder;
        });
        newOrder.removeAll(used);
        newOrder.addAll(0, used);
        ui.getContacts().clear();
        ui.getContacts().addAll(newOrder);

        if (conn != null) {
            ui.checkPendingRuns(runs.getTimestamp(), conn);
        }
    }

    private static void addUsed(List<Contact> used, Avatar who) {
        if (who == null) {
            return;
        }
        Contact contact = who.getContact();
        if (contact == null) {
            return;
        }
        for (Contact u : used) {
            if (u.getId() == contact.getId()) {
                return;
            }
        }
        used.add(0, contact);
    }

    static long markFirstFinished(List<Record> newRecords) {
        long latest = 0;
        boolean foundFirst = false;
        for (Record record : newRecords) {
            if (record.getStart() != null && record.getStart().getWhen() > latest) {
                latest = record.getStart().getWhen();
            }
            if (record.getFinish()!= null && record.getFinish().getWhen() > latest) {
                latest = record.getFinish().getWhen();
            }
            if (!foundFirst && record.getFinish() != null) {
                record.setFirstFinished(true);
                foundFirst = true;
                continue;
            }
            record.setFirstFinished(false);
        }
        return latest;
    }

    @OnReceive(url = "{url}/add?type={type}&ref={ref}", onError = "cannotConnect")
    static void sendEvent(UI ui, Event reply, String previousURL) {
        ui.loadRuns(ui.getUrl(), null);
    }

    @OnReceive(url = "{url}/add?type=ASSIGN&who={who}&ref={ref}", onError = "cannotConnect")
    static void updateWhoRef(UI ui, Event reply, String previousURL) {
        ui.loadRuns(ui.getUrl(), null);
    }

    @OnReceive(url = "{url}/contacts", onError = "cannotConnect")
    static void loadContacts(UI ui, Contact[] arr, Connection conn) {
        ui.withContacts(arr);
        ui.setMessage("Máme tu " + arr.length + " závodníků.");
        ui.loadRuns(conn.url, conn);
    }

    @OnReceive(method = "PUT", data = Settings.class, url = "{url}/admin?name={name}")
    static void updateConfig(UI ui, Settings data) {
        ui.withSettings(data);
    }

    @OnReceive(method = "GET", url = "{url}/admin?name={name}")
    static void selectConfig(UI ui, Settings data) {
        ui.withSettings(data);
        ui.loadRuns(ui.getUrl(), new Connection(ui.getUrl()));
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
    void checkPendingRuns(UI model, long timestamp, Connection conn) {
        if (Objects.equals(model.getUrl(), conn.url) && currentConnection == conn) {
            model.loadPendingRuns(conn.url, String.valueOf(timestamp), conn);
        } else {
            if (currentConnection != null && conn.counter < currentConnection.counter) {
                return;
            }
            cannotLoadPending(model, new Exception());
        }
    }

    @OnReceive(url = "{url}/runs?newerThan={since}", onError = "cannotLoadPending")
    static void loadPendingRuns(UI model, Running runs, Connection conn) {
        loadRuns(model, runs, conn);
    }

    static void cannotLoadPending(UI model, Exception ex) {
        model.setMessage("Poslouchání na změnách selhalo. " + ex.getMessage() + " - načti ručně.");
    }

    //
    // UI callback bindings
    //

    @ModelOperation
    @Function
    void connect(UI data) {
        final String u = data.getUrl();
        if (u.endsWith("/")) {
            data.setUrl(u.substring(0, u.length() - 1));
        }
        currentConnection = new Connection(data.getUrl());
        data.loadContacts(data.getUrl(), currentConnection);
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

    @ModelOperation
    @Function
    static void cancel(UI ui) {
        ui.setEdited(null);
        ui.setSelected(null);
        ui.setConfig(null);
        ui.setChoose(null);
    }

    @Function static void commit(UI ui) {
        final Contact e = ui.getEdited();
        if (e != null) {
            commitContact(ui, e);
        }
        final Config c = ui.getConfig();
        if (c != null) {
            commitConfig(ui, c);
        }
    }

    private static void commitContact(UI ui, Contact e) {
        String invalid = null;
        if (e.getValidate() != null) {
            invalid = e.getValidate();
        }
        if (invalid != null) {
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

    private static void commitConfig(UI ui, Config c) {
        Settings s = new Settings().
            withName(c.getName()).
            withDate(c.getDate()).
            withMin(c.getMin()).
            withMax(c.getMax());
        ui.updateConfig(c.getUrl(), c.getName(), s);
        ui.cancel();
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

    @ModelOperation
    void initialize(UI model, String baseUrl) {
        model.setUrl(baseUrl);
        model.setEdited(null);
        model.setSelected(null);
        model.setChoose(null);
        model.setConfig(null);
    }

    private static UI uiModel;
    /**
     * Called when the page is ready.
     */
    static void onPageLoad() throws Exception {
        uiModel = new UI();
        uiModel.initialize("http://localhost:8080/timing/");
        uiModel.applyBindings();
        uiModel.connect();
        uiModel.getCurrent().start();
    }

    static final class Connection {
        private static int cnt;

        final String url;
        final int counter;

        Connection(String url) {
            this.url = url;
            this.counter = ++cnt;
        }
    }
}
