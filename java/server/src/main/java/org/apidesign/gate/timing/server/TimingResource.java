package org.apidesign.gate.timing.server;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;
import javax.inject.Singleton;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import org.apidesign.gate.timing.shared.Event;
import org.apidesign.gate.timing.shared.Events;

@Path("/timing/") @Singleton
public final class TimingResource {
    private final NavigableSet<Event> events = new TreeSet<>(Events.COMPARATOR);
    private final Map<AsyncResponse,Long> awaiting = new HashMap<>();
    private final Storage storage = new Storage();
    private final ContactsResource contacts;
    private int counter;
    
    public TimingResource() throws IOException {
        this.contacts = new ContactsResource(storage);
        this.storage.readInto("timing", Event.class, events);
        for (Event e : events) {
            if (e.getId() > counter) {
                counter = e.getId();
            }
        }
        if (events.isEmpty()) {
            events.add(
                new Event().withId(++counter).withWhen(System.currentTimeMillis()).withType("INITIALIZED")
            );
        }
    }
    
    @GET @Produces(MediaType.APPLICATION_JSON)
    public synchronized void allEvents(
        @QueryParam("newerThan") @DefaultValue("0") long newerThan,
        @Suspended AsyncResponse response
    ) {
        Collection<Event> result;
        if (newerThan <= 0) {
            result = events;
        } else {
            result = new ArrayList<>();
            Iterator<Event> it = events.iterator();
            while (it.hasNext()) {
                Event ev = it.next();
                if (ev.getWhen() > newerThan) {
                    result.add(ev);
                } else {
                    break;
                }
            }
            if (result.isEmpty()) {
                awaiting.put(response, newerThan);
                return;
            }
        }
        response.resume(result.toArray(new Event[result.size()]));
    }
    
    @GET @Produces(MediaType.APPLICATION_JSON)
    @Path("add")
    public synchronized Event addEvent(
        @QueryParam("type") String type,
        @QueryParam("when") long when,
        @QueryParam("who") int who,
        @QueryParam("ref") int ref
    ) {
        if (when <= 0) {
            when = System.currentTimeMillis();
        }
        final Event newEvent = new Event().withId(++counter).
            withWhen(when).
            withRef(ref).
            withWho(who).
            withType(type);
        events.add(newEvent);
        storage.scheduleStore("timings", Event.class, events);
        handleAwaiting(when);
        return newEvent;
    }

    private void handleAwaiting(long newest) {
        assert Thread.holdsLock(this);
        AGAIN: for (;;) {
            for (Map.Entry<AsyncResponse, Long> entry : awaiting.entrySet()) {
                AsyncResponse ar = entry.getKey();
                Long since = entry.getValue();
                if (since <= newest) {
                    awaiting.remove(ar);
                    allEvents(since, ar);
                    continue AGAIN;
                }
            }
            return;
        }
    }

    @Path("contacts")
    public ContactsResource getContacts() {
        return contacts;
    }
}
