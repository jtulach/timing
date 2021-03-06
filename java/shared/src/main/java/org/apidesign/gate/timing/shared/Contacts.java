package org.apidesign.gate.timing.shared;

import java.util.Collection;
import net.java.html.json.ComputedProperty;
import net.java.html.json.Function;
import net.java.html.json.Model;
import net.java.html.json.Property;

@Model(className = "Contact", builder = "with", properties = {
    @Property(name = "id", type = int.class),
    @Property(name = "name", type = String.class),
    @Property(name = "aliases", type = String.class, array = true),
    @Property(name = "imgSrc", type = String.class),
})
public final class Contacts {
    @ComputedProperty
    static String validate(String name) {
        String res = null;
        if (name == null || name.isEmpty()) {
            res = "Jméno musí být zadáno";
        }
        return res;
    }

    @Function
    static void unalias(Contact model, String data) {
        model.getAliases().remove(data);
    }

    public static Contact findById(Collection<Contact> contacts, int id) {
        return findById(contacts, Integer.toString(id));
    }

    public static Contact findById(Collection<Contact> contacts, String id) {
        for (Contact c : contacts) {
            if (Integer.toString(c.getId()).equals(id)) {
                return c;
            }
        }
        for (Contact c : contacts) {
            if (c.getAliases().contains(id)) {
                return c;
            }
        }
        return null;
    }

}
