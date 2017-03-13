package org.apidesign.gate.timing;

import java.util.Comparator;
import net.java.html.json.ComputedProperty;
import net.java.html.json.Model;
import net.java.html.json.Property;
import org.apidesign.gate.timing.shared.Event;

@Model(className = "Record", builder = "with", properties = {
    @Property(name = "start", type = Event.class),
    @Property(name = "finish", type = Event.class),
    @Property(name = "who", type = Avatar.class),
})
final class RecordModel {
    static final Comparator<Record> COMPARATOR = (r1, r2) -> {
        return r2.getStart().getId() - r1.getStart().getId();
    };

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
}
