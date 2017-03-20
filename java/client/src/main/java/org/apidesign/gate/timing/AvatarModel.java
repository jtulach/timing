package org.apidesign.gate.timing;

import net.java.html.json.Model;
import net.java.html.json.ModelOperation;
import net.java.html.json.OnPropertyChange;
import net.java.html.json.Property;
import org.apidesign.gate.timing.shared.Contact;

@Model(className = "Avatar", builder = "with", instance = true, properties = {
    @Property(name = "contact", type = Contact.class)
})
class AvatarModel {
    private Runnable onSelect;

    @ModelOperation
    public void onSelect(Avatar model, Runnable callback) {
        onSelect = callback;
    }

    @OnPropertyChange("contact")
    public void handleContactChange(Avatar model) {
        Runnable callback = onSelect;
        onSelect = null;
        if (callback != null) {
            callback.run();
        }
    }
}
