package org.apidesign.gate.timing;

import net.java.html.json.Model;
import net.java.html.json.Property;
import org.apidesign.gate.timing.shared.Contact;

@Model(className = "Avatar", builder = "with", properties = {
    @Property(name = "contact", type = Contact.class)
})
class AvatarModel {

}
