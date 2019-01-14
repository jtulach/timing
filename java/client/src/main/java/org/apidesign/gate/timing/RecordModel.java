package org.apidesign.gate.timing;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.NavigableSet;
import java.util.TreeSet;
import net.java.html.json.ComputedProperty;
import net.java.html.json.Model;
import net.java.html.json.ModelOperation;
import net.java.html.json.Models;
import net.java.html.json.Property;
import org.apidesign.gate.timing.shared.Contact;
import org.apidesign.gate.timing.shared.Event;
import org.apidesign.gate.timing.shared.Events;

@Model(className = "Record", builder = "with", properties = {
    @Property(name = "start", type = Event.class),
    @Property(name = "finish", type = Event.class),
    @Property(name = "ignore", type = boolean.class),
    @Property(name = "firstFinished", type = boolean.class),
    @Property(name = "who", type = Avatar.class),
    @Property(name = "current", type = Current.class)
})
final class RecordModel {
    @ComputedProperty
    static String seconds(Event start, Event finish, Current current) {
        long actual = actualTime(finish, current);
        if (!validActual(actual, start)) {
            return "--";
        }
        long time = (actual - start.getWhen());
        final long hourMillis = 3600L * 1000L;
        long rem = time % hourMillis;
        if (rem < 0) {
            rem += hourMillis;
        }
        String digits = Long.toString(rem / 1000L);
        if (digits.length() < 2) {
            digits = "0" + digits;
        }
        return digits;
    }

    @ComputedProperty
    static boolean valid(Event start, Event finish, Current current) {
        long actual = actualTime(finish, current);
        return validActual(actual, start);
    }

    private static boolean validActual(long actual, Event start) {
        return start != null;// && actual >= start.getWhen();
    }

    private static long actualTime(Event finish, Current current) {
        long actual;
        if (finish != null && finish.getWhen() > 0) {
            actual = finish.getWhen();
        } else {
            actual = current.getMillis();
        }
        return actual;
    }

    @ComputedProperty
    static String hundreds(Event start, Event finish, Current current) {
        long actual = actualTime(finish, current);
        if (!validActual(actual, start)) {
            return "--";
        }
        long time = actual - start.getWhen();
        long rem = time % 1000;
        if (rem < 0) {
            rem += 1000;
        }
        String digits = Long.toString(rem / 10);
        if (digits.length() < 2) {
            digits = "0" + digits;
        }
        return digits;
    }

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
        return compute(ui, arr, limit, false);
    }
    static Record[] compute(UI ui, List<Event> arr, int limit, boolean onlyValid) {
        LinkedList<Record> records = new LinkedList<>();
        int ignored = 0;
        for (Event ev : arr) {
            Record r;
            if (ev.getType() == null) {
                continue;
            }
            switch (ev.getType()) {
                case "START":
                    r = findRecord(records, ev.getId(), true, false);
                    if (r == null) {
                        r = new Record();
                        r.withCurrent(ui.getCurrent());
                        r.empty();
                        r.withStart(ev);
                    }
                    records.addFirst(r);
                    break;
                case "FINISH":
                    r = findRecord(records, ev.getId(), false, true);
                    if (r == null) {
                        r = new Record();
                        r.withCurrent(ui.getCurrent());
                        r.empty();
                        r.withFinish(ev);
                        ListIterator<Record> it = records.listIterator(records.size());
                        while (it.hasPrevious()) {
                            Record prev = it.previous();
                            if (prev.isIgnore()) {
                                continue;
                            }
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
                case "IGNORE": {
                        r = findRecord(records, ev.getRef(), true, true);
                        if (r != null && !r.isIgnore()) {
                            r.setIgnore(true);
                            ignored++;
                            Record ignoresFinish = findRecord(records, r.getFinish(), false, true);
                            if (ignoresFinish != null) {
                                ignoresFinish.setFinish(null);
                            }
                            Record ignoresStart = findRecord(records, r.getStart(), true, false);
                            if (ignoresStart != null) {
                                ignoresStart.setStart(null);
                            }
                        }
                    }
                    break;
                case "ASSIGN":
                    r = findRecord(records, ev.getRef(), true, true);
                    if (r != null) {
                        r.getWho().withContact(findContact(ui.getContacts(), ev.getWho()));
                    }
                    break;
                case "CONNECT":
                    Record finish = findRecord(records, ev.getRef(), false, true);
                    Record start = findRecord(records, ev.getWho(), true, false);
                    if (start != null && finish != null) {
                        start.setFinish(finish.getFinish());
                        finish.setStart(start.getStart());
                    }
                    break;
            }
        }

        List<Record> newRecords = Models.asList();
        TreeSet<Event> events = new TreeSet<>(Events.TIMELINE);
        for (Record r : records) {
            if (!r.isIgnore() && r.getFinish() != null) {
                events.add(r.getFinish());
            }
        }
        int i = 0;
        boolean someFinished = false;
        for (Record r : records) {
            if (r.isIgnore()) {
                continue;
            }
            if (onlyValid && !r.isValid()) {
                continue;
            }
            if (r.getFinish() != null && !someFinished) {
                r.setFirstFinished(true);
                someFinished = true;
            }
            newRecords.add(r);
        }
        return newRecords.toArray(new Record[newRecords.size()]);
    }

    private static Record findRecord(Collection<Record> records, int searchId, boolean checkStart, boolean checkFinish) {
        for (Record r : records) {
            if (r.isIgnore()) {
                continue;
            }

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
    private static Record findRecord(Collection<Record> records, Event searchEvent, boolean checkStart, boolean checkFinish) {
        if (searchEvent == null) {
            return null;
        } else {
            return findRecord(records, searchEvent.getId(), checkStart, checkFinish);
        }
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
            if (r.getStart() == null && r.getFinish() == data.getFinish()) {
                r.getWho().setContact(null);
            }
        }
        data.setFinish(currentFinish);
        NavigableSet<Event> onlyNewer = events.tailSet(data.getStart(), true);
        for (Record r : records) {
            if (r.getStart() == null && r.getFinish() == currentFinish) {
                r.getWho().setContact(data.getWho().getContact());
            }
        }

    }

}
