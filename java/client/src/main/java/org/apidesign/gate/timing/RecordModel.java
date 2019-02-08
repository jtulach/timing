package org.apidesign.gate.timing;

import java.util.Comparator;
import java.util.List;
import net.java.html.json.ComputedProperty;
import net.java.html.json.Model;
import net.java.html.json.ModelOperation;
import net.java.html.json.Property;
import org.apidesign.gate.timing.shared.Contact;
import org.apidesign.gate.timing.shared.Event;
import org.apidesign.gate.timing.shared.Run;

@Model(className = "Record", builder = "with", properties = {
    @Property(name = "run", type = Run.class),
    @Property(name = "firstFinished", type = boolean.class),
    @Property(name = "who", type = Avatar.class),
    @Property(name = "current", type = Current.class)
})
final class RecordModel {
    @ComputedProperty
    static Event start(Run run) {
        return run.getStart();
    }

    @ComputedProperty
    static Event finish(Run run) {
        return run.getFinish();
    }

    @ComputedProperty
    static boolean dnf(Run run) {
        return run.isDnf();
    }

    @ComputedProperty
    static String seconds(Run run, Current current) {
        Event start = run.getStart();
        Event finish = run.getFinish();
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
    static boolean valid(Current current, Run run) {
        Event start = run.getStart();
        Event finish = run.getFinish();
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
    static String hundreds(Current current, Run run) {
        Event start = run.getStart();
        Event finish = run.getFinish();
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
        model.getRun().empty();
        model.getWho().withContact(null);
    }

    @ModelOperation
    static void findWhoAvatar(Record model, List<Contact> contacts) {
        int who = model.getRun().getWho();
        for (Contact c : contacts) {
            if (c.getId() == who) {
                model.getWho().withContact(c);
                return;
            }
        }
        model.getWho().withContact(null);
    }

}
