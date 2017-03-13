package org.apidesign.gate.timing;

import java.util.Comparator;
import net.java.html.json.ComputedProperty;
import net.java.html.json.Model;
import net.java.html.json.Property;
import org.apidesign.gate.timing.shared.Event;
import org.apidesign.gate.timing.shared.Events;

@Model(className = "Record", builder = "with", properties = {
    @Property(name = "start", type = Event.class),
    @Property(name = "finish", type = Event.class),
    @Property(name = "who", type = Avatar.class),
})
final class RecordModel {
    static final Comparator<Record> COMPARATOR = (r1, r2) -> {
        return Events.COMPARATOR.compare(r1.getStart(), r2.getStart());
    };

    @ComputedProperty
    static long lengthMillis(Event start, Event finish) {
        if (start == null || finish == null) {
            return 0;
        }
        return finish.getWhen() - start.getWhen();
    }

    @ComputedProperty
    static String length(Event start, Event finish) {
        long time = lengthMillis(start, finish);
        long sec = time / 1000L;
        long cent = (time % 1000L) / 10;
        return twoDigits(sec) + ":" + twoDigits(cent);
    }

    static String twoDigits(long value) {
        if (value < 10) {
            return "0" + value;
        }
        return "" + value;
    }
}
