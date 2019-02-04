package org.apidesign.gate.timing.shared;

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
}