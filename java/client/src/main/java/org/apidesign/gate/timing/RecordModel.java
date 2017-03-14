package org.apidesign.gate.timing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;
import net.java.html.json.ComputedProperty;
import net.java.html.json.Model;
import net.java.html.json.ModelOperation;
import net.java.html.json.Property;
import org.apidesign.gate.timing.shared.Contact;
import org.apidesign.gate.timing.shared.Event;

@Model(className = "Record", builder = "with", properties = {
    @Property(name = "start", type = Event.class),
    @Property(name = "finish", type = Event.class),
    @Property(name = "ignore", type = boolean.class),
    @Property(name = "who", type = Avatar.class),
})
final class RecordModel {
    static final Comparator<Record> COMPARATOR = (r1, r2) -> {
        return r2.getStart().getId() - r1.getStart().getId();
    };

    @ModelOperation
    static void empty(Record model) {
        model.withStart(null).withFinish(null).withIgnore(false);
        model.getWho().withContact(null);
    }

    @ComputedProperty
    static long lengthMillis(Event start, Event finish) {
        if (start == null || finish == null) {
            return 0;
        }
        long time = finish.getWhen() - start.getWhen();
        return time < 0 ? 0 : time;
    }

    @ComputedProperty
    static String length(Event start, Event finish) {
        long time = lengthMillis(start, finish);
        long sec = time / 1000L;
        long cent = (time % 1000L) / 10;
        return twoDigits(sec) + ":" + twoDigits(cent);
    }

    static String twoDigits(long value) {
        if (value >= 0 && value < 10) {
            return "0" + value;
        }
        return "" + value;
    }

    static Record[] compute(UI ui, List<Event> arr, int limit) {
        List<Record> previous = ui.getRecords();
        List<Record> records = new ArrayList<>();
        int ignored = 0;
        for (Event ev : arr) {
            Record r;
            CASE: switch (ev.getType()) {
                case "START":
                    r = findRecord(previous, ev.getId(), ev, true);
                    if (r == null) {
                        r = new Record();
                        r.empty();
                        r.withStart(ev);
                    }
                    records.add(r);
                    break;
                case "FINISH":
                    r = findRecord(previous, ev.getId(), ev, false);
                    if (r == null) {
                        for (Record prev : records) {
                            if (prev.getStart() != null && prev.getFinish() == null) {
                                prev.setFinish(ev);
                                break CASE;
                            }
                        }
                    }
                    r = new Record();
                    r.empty();
                    r.withFinish(ev);
                    records.add(r);
                    break;
                case "IGNORE":
                    r = findRecord(records, ev.getRef(), null, null);
                    if (r != null && !r.isIgnore()) {
                        r.setIgnore(true);
                        ignored++;
                    }
                    break;
                case "ASSIGN":
                    r = findRecord(previous, ev.getRef(), null, null);
                    if (r != null) {
                        r.getWho().withContact(findContact(ui.getContacts(), ev.getWho()));
                    }
                    break;
                case "CONNECT":
                    Record finish = findRecord(previous, ev.getRef(), null, false);
                    Record start = findRecord(previous, ev.getWho(), null, true);
                    if (start != null && finish != null) {
                        start.setFinish(finish.getFinish());
                        finish.setStart(start.getStart());
                    }
                    break;
            }
        }

        int size = Math.min(limit, records.size() - ignored);
        Record[] newRecords = new Record[size];
        int i = 0;
        for (Record r : records) {
            if (r.isIgnore()) {
                continue;
            }
            if (i < newRecords.length) {
                newRecords[i++] = r;
            }
        }
        return newRecords;
    }

    static void onStartEvent(UI model, Event ev) {
        if (ev.getWho() <= 0 && model.getNextOnStart() != null && model.getNextOnStart().getContact() != null) {
            ev.setWho(model.getNextOnStart().getContact().getId());
            model.getNextOnStart().setContact(null);
            model.updateWhoRef(model.getUrl(), "" + ev.getId(), "" + ev.getWho(), "0");
        }
    }

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

    private static Record findRecord(Collection<Record> records, int searchId, Event searchFor, Boolean startOnly) {
        final boolean checkStart = !Boolean.FALSE.equals(startOnly);
        final boolean checkFinish = !Boolean.TRUE.equals(startOnly);
        for (Record r : records) {
            if (checkStart) {
                Event ev = r.getStart();
                if (ev != null && ev.getId() == searchId) {
                    return r;
                }
            }
            if (checkFinish) {
                Event ev = r.getFinish();
                if (ev != null && ev.getId() == searchId) {
                    return r;
                }
            }
        }
        return null;
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

}
