package org.apidesign.gate.timing.server;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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
    public synchronized List<Contact> allContacts() {
        return contacts;
    }

    @POST @Produces(MediaType.APPLICATION_JSON)
    public synchronized List<Contact> addContact(Contact newOne) {
        contacts.add(newOne.withId("" + ++counter));
        return contacts;
    }

    @PUT @Produces(MediaType.APPLICATION_JSON) @Path("{id}")
    public synchronized List<Contact> updateContact(@PathParam("id") String id, Contact newOne) {
        ListIterator<Contact> it = contacts.listIterator();
        while (it.hasNext()) {
            Contact c = it.next();
            if (id.equals(c.getId())) {
                it.set(newOne.withId(id));
                return contacts;
            }
        }
        throw new WebApplicationException(Response.Status.NOT_FOUND);
    }

    @DELETE @Produces(MediaType.APPLICATION_JSON) @Path("{id}")
    public synchronized List<Contact> deleteContact(@PathParam("id") String id) {
        ListIterator<Contact> it = contacts.listIterator();
        while (it.hasNext()) {
            Contact c = it.next();
            if (id.equals(c.getId())) {
                it.remove();
                return contacts;
            }
        }
        throw new WebApplicationException(Response.Status.NOT_FOUND);
    }

}
