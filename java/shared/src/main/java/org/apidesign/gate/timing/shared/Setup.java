package org.apidesign.gate.timing.shared;

import net.java.html.json.ComputedProperty;
import net.java.html.json.Model;
import net.java.html.json.Property;

@Model(className = "Settings", builder = "with", properties = {
    @Property(name = "name", type = String.class),
    @Property(name = "date", type = String.class),
    @Property(name = "min", type = String.class),
    @Property(name = "max", type = String.class),
    @Property(name = "measurements", type = String.class, array = true),
})
final class Setup {
    @ComputedProperty
    static long minMillis(String min) {
        long ms = Long.MIN_VALUE;
        try {
            ms = Long.parseLong(min) * 1000L;
        } catch (Exception ex) {
            // ignore
        }
        return ms;
    }
    @ComputedProperty
    static long maxMillis(String max) {
        long ms = Long.MAX_VALUE;
        try {
            ms = Long.parseLong(max) * 1000L;
        } catch (Exception ex) {
            // ignore
        }
        return ms;
    }
}
