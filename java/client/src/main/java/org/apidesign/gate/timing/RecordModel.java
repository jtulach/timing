package org.apidesign.gate.timing;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import net.java.html.json.ComputedProperty;
import net.java.html.json.Model;
import net.java.html.json.ModelOperation;
import net.java.html.json.Property;
import org.apidesign.gate.timing.shared.Contact;
import org.apidesign.gate.timing.shared.Event;
import org.apidesign.gate.timing.shared.Events;

@Model(className = "Record", builder = "with", properties = {
    @Property(name = "start", type = Event.class),
    @Property(name = "finish", type = Event.class),
    @Property(name = "ignore", type = boolean.class),
    @Property(name = "who", type = Avatar.class),
    @Property(name = "next", type = Event.class),
    @Property(name = "prev", type = Event.class),
})
final class RecordModel {
    static final Comparator<Record> COMPARATOR = (r1, r2) -> {
        return r2.getStart().getId() - r1.getStart().getId();
    };

    @ModelOperation
    static void empty(Record model) {
        model.withStart(null).withFinish(null).withIgnore(false);
        model.withPrev(null).withNext(null);
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

    @ComputedProperty
    static String nextTime(Event start, Event next) {
        return length(start, next);
    }

    @ComputedProperty
    static String prevTime(Event start, Event prev) {
        return length(start, prev);
    }

    static String twoDigits(long value) {
        if (value >= 0 && value < 10) {
            return "0" + value;
        }
        return "" + value;
    }

    static Record[] compute(UI ui, List<Event> arr, int limit) {
        LinkedList<Record> records = new LinkedList<>();
        int ignored = 0;
        for (Event ev : arr) {
            Record r;
            if (ev.getType() == null) {
                continue;
            }
            switch (ev.getType()) {
                case "START":
                    r = findRecord(records, ev.getId(), ev, true);
                    if (r == null) {
                        r = new Record();
                        r.empty();
                        r.withStart(ev);
                    }
                    records.addFirst(r);
                    break;
                case "FINISH":
                    r = findRecord(records, ev.getId(), ev, false);
                    if (r == null) {
                        r = new Record();
                        r.empty();
                        r.withFinish(ev);
                        for (Record prev : records) {
                            if (prev.getFinish() == null) {
                                prev.withFinish(ev);
                                break;
                            }
                        }
                        records.addFirst(r);
                    } else {
                        r.setFinish(ev);
                    }
                    break;
                case "IGNORE":
                    r = findRecord(records, ev.getRef(), null, null);
                    if (r != null && !r.isIgnore()) {
                        r.setIgnore(true);
                        ignored++;
                    }
                    break;
                case "ASSIGN":
                    r = findRecord(records, ev.getRef(), null, null);
                    if (r != null) {
                        r.getWho().withContact(findContact(ui.getContacts(), ev.getWho()));
                    }
                    break;
                case "CONNECT":
                    Record finish = findRecord(records, ev.getRef(), null, false);
                    Record start = findRecord(records, ev.getWho(), null, true);
                    if (start != null && finish != null) {
                        start.setFinish(finish.getFinish());
                        finish.setStart(start.getStart());
                    }
                    break;
            }
        }

        int size = Math.min(limit, records.size() - ignored);
        Record[] newRecords = new Record[size];
        TreeSet<Event> events = new TreeSet<>(Events.TIMELINE);
        for (Record r : records) {
            if (!r.isIgnore() && r.getFinish() != null) {
                events.add(r.getFinish());
            }
        }
        int i = 0;
        for (Record r : records) {
            if (r.isIgnore()) {
                continue;
            }
            if (r.getStart() != null) {
                r.setNext(events.higher(r.getFinish()));
                r.setPrev(events.lower(r.getFinish()));
            }

            if (i < newRecords.length) {
                newRecords[i++] = r;
            }
        }
        return newRecords;
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

    static void resetFinish(Record data, final Event currentFinish, final List<Record> records) {
        TreeSet<Event> events = new TreeSet<>(Events.TIMELINE);
        for (Record r : records) {
            if (r.getFinish() != null) {
                events.add(r.getFinish());
            }
            if (r.getFinish() == currentFinish) {
                r.setFinish(null);
            }
        }
        data.setFinish(currentFinish);
        data.setNext(events.higher(currentFinish));
        data.setPrev(events.lower(currentFinish));
    }

}
