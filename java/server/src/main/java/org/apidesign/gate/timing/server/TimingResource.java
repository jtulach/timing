package org.apidesign.gate.timing.server;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
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
import org.apidesign.gate.timing.shared.Run;
import org.apidesign.gate.timing.shared.Runs;

@Path("/timing/") @Singleton
public final class TimingResource {
    private final Map<AsyncResponse,Request> awaiting = new HashMap<>();
    private final NavigableSet<Event> events = new TreeSet<>(Events.COMPARATOR);
    private List<Run> runs = Collections.emptyList();
    private int counter;
    @Inject
    private Storage storage;
    @Inject
    private ContactsResource contacts;

    public TimingResource() {
    }

    @PostConstruct
    public synchronized void init() throws IOException {
        storage.readInto("timing", Event.class, events);
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
        runs = Runs.compute(events);
    }

    @GET @Produces(MediaType.APPLICATION_JSON)
    public void allEvents(
        @QueryParam("newerThan") @DefaultValue("0") long newerThan,
        @Suspended AsyncResponse response
    ) {
        allEvents(new Request(false, newerThan), response);
    }

    @GET @Produces(MediaType.APPLICATION_JSON)
    @Path("runs")
    public void allRuns(
        @QueryParam("newerThan") @DefaultValue("0") long newerThan,
        @Suspended AsyncResponse response
    ) {
        allEvents(new Request(true, newerThan), response);
    }

    private synchronized void allEvents(Request request, AsyncResponse response) {
        long first = events.isEmpty() ? -1L : events.iterator().next().getWhen();
        if (first <= request.newerThan) {
            awaiting.put(response, request);
            return;
        }

        abstract class Loop<T> {
            abstract long when(T item);
            abstract T[] array(int size);

            final void produce(Collection<T> all) {
                Collection<T> result = new ArrayList<>();
                for (T item : all) {
                    final long when = when(item);
                    if (when > request.newerThan) {
                        result.add(item);
                    }
                }
                T[] arr = array(result.size());
                response.resume(result.toArray(arr));
            }
        }

        if (request.computeRuns) {
            new Loop<Run>() {
                long when(Run r) {
                    return r.getWhen();
                }

                Run[] array(int size) {
                    return new Run[size];
                }
            }.produce(runs);
        } else {
            new Loop<Event>() {
                long when(Event e) {
                    return e.getWhen();
                }

                Event[] array(int size) {
                    return new Event[size];
                }
            }.produce(events);
        }
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
        runs = Runs.compute(events);
        storage.scheduleStore("timings", Event.class, events);
        handleAwaiting(when);
        return newEvent;
    }

    private void handleAwaiting(long newest) {
        assert Thread.holdsLock(this);
        Iterator<Map.Entry<AsyncResponse, Request>> it;
        for (it = awaiting.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<AsyncResponse, Request> entry = it.next();
            AsyncResponse ar = entry.getKey();
            Request since = entry.getValue();
            if (since.newerThan <= newest) {
                it.remove();
                allEvents(since, ar);
            }
        }
    }

    @Path("contacts")
    public ContactsResource getContacts() {
        return contacts;
    }

    private static final class Request {
        final boolean computeRuns;
        final long newerThan;

        Request(boolean runs, long newerThan) {
            this.computeRuns = runs;
            this.newerThan = newerThan;
        }
    }
}
