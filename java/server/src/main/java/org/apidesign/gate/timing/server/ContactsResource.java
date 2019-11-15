package org.apidesign.gate.timing.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
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

@Singleton
public final class ContactsResource {
    private static final Logger LOG = Main.LOG;
    private final List<Contact> contacts = new ArrayList<>();
    private Map<Integer,String> pendingAliases = new HashMap<>();

    @Inject
    private Storage storage;
    private int counter;


    public ContactsResource() throws IOException {
    }

    @PostConstruct
    public void init() throws IOException {
        this.storage.readInto("people", Contact.class, contacts);
        for (Contact c : contacts) {
            if (c.getId() > counter) {
                counter = c.getId();
            }
        }
    }

    @GET @Produces(MediaType.APPLICATION_JSON)
    public synchronized List<Contact> allContacts() {
        return contacts;
    }

    @POST @Produces(MediaType.APPLICATION_JSON)
    public synchronized List<Contact> addContact(Contact newOne) {
        LOG.log(Level.FINE, "addContact {0}", newOne);
        contacts.add(newOne.withId(++counter));
        this.storage.scheduleStore("people", Contact.class, contacts);
        return contacts;
    }

    @PUT @Produces(MediaType.APPLICATION_JSON) @Path("{id}")
    public synchronized List<Contact> updateContact(
        @PathParam("id") @DefaultValue("-1") int id, Contact newOne
    ) {
        LOG.log(Level.FINE, "updateContact {0} value: {1}", new Object[]{id, newOne});
        ListIterator<String> alIt = newOne.getAliases().listIterator();
        while (alIt.hasNext()) {
            String a = alIt.next();
            try {
                int hashCode = Integer.parseInt(a);
                String realName = pendingAliases.remove(hashCode);
                if (realName != null) {
                    alIt.set(realName);
                }
            } catch (NumberFormatException ex) {
                // OK
            }
        }
        ListIterator<Contact> it = contacts.listIterator();
        while (it.hasNext()) {
            Contact c = it.next();
            if (id == c.getId()) {
                it.set(newOne.withId(id));
                this.storage.scheduleStore("people", Contact.class, contacts);
                return contacts;
            }
        }
        throw new WebApplicationException(Response.Status.NOT_FOUND);
    }

    @DELETE @Produces(MediaType.APPLICATION_JSON) @Path("{id}")
    public synchronized List<Contact> deleteContact(@PathParam("id") String id) {
        LOG.log(Level.FINE, "deleteContact {0}", id);
        ListIterator<Contact> it = contacts.listIterator();
        while (it.hasNext()) {
            Contact c = it.next();
            if (id.equals(c.getId())) {
                it.remove();
                this.storage.scheduleStore("people", Contact.class, contacts);
                return contacts;
            }
        }
        throw new WebApplicationException(Response.Status.NOT_FOUND);
    }

    void pendingAliases(int whoNum, String who) {
        pendingAliases.put(whoNum, who);
    }

}
