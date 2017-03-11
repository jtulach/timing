package org.apidesign.gate.timing;

import net.java.html.json.Model;
import net.java.html.json.Property;
import org.apidesign.gate.timing.shared.Event;

@Model(className = "Record", builder = "with", properties = {
    @Property(name = "event", type = Event.class),
    @Property(name = "who", type = Avatar.class),
})
class RecordModel {

}
