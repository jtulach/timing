package org.apidesign.gate.timing.shared;

import java.util.Collections;
import java.util.Comparator;
import net.java.html.json.ComputedProperty;
import net.java.html.json.Model;
import net.java.html.json.Property;

@Model(className = "Event", builder = "with", properties = {
    @Property(name = "id", type=int.class),
    @Property(name = "when", type=long.class),
    @Property(name = "type", type=String.class),
    @Property(name = "ref", type=int.class),
})
/** Operations with {@link Event} model classes.
 */
public final class Events {
    /** Compares two {@link Event}s and sorts the newest ones first.
     * Comparing is based on the time, then id and then
     * it compares them for non-equality.
     */
    public static final Comparator<Event> COMPARATOR = Collections.reverseOrder((ev1, ev2) -> {
        if (ev1 == ev2) {
            return 0;
        }
        long timeDelta = ev1.getWhen() - ev2.getWhen();
        if (timeDelta != 0) {
            return timeDelta < 0 ? -1 : 1;
        }
        int idDelta = ev1.getId() - ev2.getId();
        if (idDelta != 0) {
            return idDelta;
        }
        return ev1.hashCode() - ev2.hashCode();
    });


    //
    // Events implementation
    //

    static String twoDigits(long value) {
        if (value < 10) {
            return "0" + value;
        }
        return "" + value;
    }

    @ComputedProperty
    static String shortWhen(long when) {
        long inSec = when / 1000L;
        long hourMinSec = inSec % 3600L;
        long min = hourMinSec / 60L;
        long sec = hourMinSec % 60L;
        return twoDigits(min) + "'" + twoDigits(sec) + "''";
    }
}

@Model(className = "Contact", properties = {
    @Property(name = "id", type = String.class),
    @Property(name = "firstName", type = String.class),
    @Property(name = "lastName", type = String.class),
    @Property(name = "imgSrc", type = String.class),
})
final class Contacts {
    @ComputedProperty static String fullName(
        String firstName, String lastName
    ) {
        if (firstName == null) {
            return lastName;
        }
        if (lastName == null) {
            return firstName;
        }
        return firstName + " " + lastName;
    }

    @ComputedProperty static String validate(
        String firstName, String lastName
    ) {
        String res = null;
        if (firstName == null || firstName.isEmpty()) {
            res = "Specify first name";
        }
        if (res == null && (lastName == null || lastName.isEmpty())) {
            res = "Specify last name";
        }
        return res;
    }

}
