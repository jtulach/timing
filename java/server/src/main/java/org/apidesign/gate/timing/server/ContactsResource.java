package org.apidesign.gate.timing.server;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.apidesign.gate.timing.shared.Contact;

public final class ContactsResource {
    private List<Contact> contacts = new ArrayList<>();
    private int counter;
    {
        contacts.add(new Contact().
            withId("" + ++counter).
            withName("Jarda").
            withImgSrc("http://wiki.apidesign.org/images/b/b7/Tulach.png")
        );
        contacts.add(new Contact().
            withId("" + ++counter).
            withName("Noname")
        );
    }

    @GET @Produces(MediaType.APPLICATION_JSON)
    public List<Contact> allContacts() {
        return contacts;
    }
}
