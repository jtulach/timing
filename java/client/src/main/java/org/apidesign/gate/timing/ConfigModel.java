package org.apidesign.gate.timing;

import net.java.html.json.ComputedProperty;
import net.java.html.json.Model;
import net.java.html.json.Property;

@Model(className = "Config", builder = "with", properties = {
    @Property(name = "name", type = String.class),
    @Property(name = "date", type = String.class),
    @Property(name = "min", type = String.class),
    @Property(name = "max", type = String.class),
    @Property(name = "ui", type = UI.class),
})
final class ConfigModel {
    @ComputedProperty(write = "url")
    static String url(UI ui) {
        return ui.getUrl();
    }
    static void url(Config model, String newUrl) {
        model.getUi().setUrl(newUrl);
    }
}
