package org.apidesign.gate.timing.shared;

import net.java.html.json.ComputedProperty;
import net.java.html.json.Model;
import net.java.html.json.Property;

@Model(className = "Contact", builder = "with", properties = {
    @Property(name = "id", type = int.class),
    @Property(name = "name", type = String.class),
    @Property(name = "imgSrc", type = String.class),
})
final class Contacts {
    @ComputedProperty static String validate(String name) {
        String res = null;
        if (name == null || name.isEmpty()) {
            res = "Jméno musí být zadáno";
        }
        return res;
    }

}
