package org.apidesign.gate.timing.server;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apidesign.gate.timing.shared.Contact;
import org.apidesign.gate.timing.shared.Contacts;
import org.apidesign.gate.timing.shared.Event;
import org.apidesign.gate.timing.shared.Events;
import org.apidesign.gate.timing.shared.Run;
import org.apidesign.gate.timing.shared.Running;
import org.apidesign.gate.timing.shared.Settings;
import org.apidesign.gate.timing.shared.Runs;

@Path("/timing/") @Singleton
public final class TimingResource {
    private static final Logger LOG = Main.LOG;

    private final Map<AsyncResponse,Request> awaiting = new HashMap<>();
    private final NavigableSet<Event> events = new TreeSet<>(Events.COMPARATOR);
    private Running runs = new Running();
    private int counter;
    @Inject
    private Storage storage;
    @Inject
    private ContactsResource contacts;
    @Inject
    private Settings settings;

    public TimingResource() {
    }

    @PostConstruct
    public void init() throws IOException {
        reloadSettings();
    }

    void updateSettings(Settings s) {
        settings.withName(s.getName());
        settings.withDate(s.getDate());
        settings.withMin(s.getMin());
        settings.withMax(s.getMax());
        try {
            reloadSettings();
        } catch (IOException ex) {
            throw new WebApplicationException(ex.getMessage(), ex, Response.Status.NOT_FOUND);
        }
    }

    private synchronized void reloadSettings() throws IOException {
        runs = new Running().withSettings(settings);
        events.clear();
        storage.readInto(settings.getName(), Event.class, events);
        for (Event e : events) {
            if (e.getId() > counter) {
                counter = e.getId();
            }
        }
        if (events.isEmpty()) {
            events.add(
                new Event().withId(++counter).withWhen(System.currentTimeMillis()).withType(Events.INITIALIZED)
            );
        }
        updateRunsAndReturnChanged();
        // notify all
        handleAwaiting(null, runs);
    }

    synchronized Settings settings() {
        Settings s = settings.clone();
        s.getMeasurements().clear();
        s.getMeasurements().addAll(listMeasurements());
        return s;
    }

    @GET @Produces(MediaType.APPLICATION_JSON)
    public void allEvents(
        @QueryParam("newerThan") @DefaultValue("0") long newerThan,
        @Suspended AsyncResponse response
    ) {
        allEvents(new Request(false, newerThan), response, null);
    }

    @GET @Produces(MediaType.APPLICATION_JSON)
    @Path("runs")
    public void allRuns(
        @QueryParam("newerThan") @DefaultValue("0") long newerThan,
        @Suspended AsyncResponse response
    ) {
        allEvents(new Request(true, newerThan), response, null);
    }

    private synchronized void allEvents(
        Request request, AsyncResponse response, Running changedRuns
    ) {
        long first = events.isEmpty() ? -1L : events.iterator().next().getWhen();
        if (first <= request.newerThan) {
            LOG.log(Level.FINE, "allEvents awating: {0}", request.newerThan);
            awaiting.put(response, request);
            return;
        }
        deliverAllEvents(request, response, changedRuns);
    }

    private synchronized void deliverAllEvents(
        Request request, AsyncResponse response, Running changedRuns
    ) {
        abstract class Loop<T> {
            abstract long when(T item);
            abstract T[] array(int size);
            abstract Object wrap(T[] arr);

            final void produce(Collection<T> all) {
                Collection<T> result = new ArrayList<>();
                for (T item : all) {
                    final long when = when(item);
                    if (when > request.newerThan) {
                        result.add(item);
                    }
                }
                T[] arr = array(result.size());
                result.toArray(arr);
                response.resume(wrap(arr));
            }
        }

        if (request.computeRuns) {
            if (changedRuns == null) {
                changedRuns = this.runs;
            }
            Running filterRuns = changedRuns.clone();
            filterRuns.withSettings(settings());
            new Loop<Run>() {
                @Override
                long when(Run r) {
                    return Long.MAX_VALUE;
                }

                @Override
                Run[] array(int size) {
                    return new Run[size];
                }

                @Override
                Object wrap(Run[] arr) {
                    return filterRuns.withRuns(arr);
                }
            }.produce(filterRuns.getRuns());
        } else {
            new Loop<Event>() {
                long when(Event e) {
                    return e.getWhen();
                }

                Event[] array(int size) {
                    return new Event[size];
                }

                @Override
                Object wrap(Event[] arr) {
                    return arr;
                }
            }.produce(events);
        }
    }

    @GET @Produces({ MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN })
    @Path("add")
    public synchronized Response addEvent(
        @HeaderParam("Accept") String accept, 
        @QueryParam("type") Events type,
        @QueryParam("when") long when,
        @QueryParam("who") String who,
        @QueryParam("ref") @DefaultValue("-1") int ref
    ) {
        LOG.log(Level.FINE, "addEvent Accept: {0} type {1} when {2} who {3} ref {4}", new Object[] { accept, type, when, who, ref });
        if (when <= 0) {
            when = System.currentTimeMillis();
        }
        String whoName;
        Contact checkWho = Contacts.findById(contacts.allContacts(), who);
        int whoNum;
        if (checkWho != null) {
            whoNum = checkWho.getId();
            whoName = checkWho.getName();
        } else {
            whoName = "" + who;
            if (who == null) {
                whoNum = 0;
            } else {
                try {
                    whoNum = Integer.parseInt(who);
                } catch (NumberFormatException ex) {
                    whoNum = Math.abs(who.hashCode());
                    if (whoNum == 0) {
                        whoNum = Integer.MAX_VALUE;
                    }
                    contacts.pendingAliases(whoNum, who);
                }
            }
        }
        final Event newEvent = new Event().withId(++counter).
            withWhen(when).
            withRef(ref).
            withWho(whoNum).
            withType(type);
        LOG.log(Level.FINE, "  checkWho {0} event {1}", new Object[] { checkWho, newEvent });
        events.add(newEvent);
        storage.scheduleStore(settings.getName(), Event.class, events);
        Running changed = updateRunsAndReturnChanged();
        handleAwaiting(when, changed);
        if (MediaType.TEXT_PLAIN.equals(accept)) {
            return Response.ok(whoName).type(MediaType.TEXT_PLAIN_TYPE).build();
        }
        return Response.ok(newEvent).type(MediaType.APPLICATION_JSON_TYPE).build();
    }

    private Running updateRunsAndReturnChanged() {
        Running newRunning = Runs.compute(events, settings().getMinMillis(), settings().getMaxMillis());
        final List<Run> oldRuns = this.runs.getRuns();
        List<Run> newRuns = newRunning.getRuns();
        int at = newRuns.size() - 1;
        synchronized (this) {
            while (at >= 0) {
                Run newR = newRuns.get(at);
                int oldAt = at - newRuns.size() + oldRuns.size();
                if (oldAt < 0) {
                    break;
                }
                Run oldR = oldRuns.get(oldAt);
                if (!newR.equals(oldR)) {
                    break;
                }
                at--;
            }
            runs = newRunning;
        }
        Running justChanged = newRunning.clone();
        justChanged.withSettings(settings());
        justChanged.getRuns().clear();
        justChanged.getRuns().addAll(newRuns.subList(0, at + 1));
        return justChanged;
    }

    private void handleAwaiting(Long newest, Running changedRuns) {
        assert Thread.holdsLock(this);
        Iterator<Map.Entry<AsyncResponse, Request>> it;
        for (it = awaiting.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<AsyncResponse, Request> entry = it.next();
            AsyncResponse ar = entry.getKey();
            Request since = entry.getValue();
            if (newest == null) {
                it.remove();
                deliverAllEvents(since, ar, changedRuns);
            } else if (since.newerThan <= newest) {
                it.remove();
                allEvents(since, ar, changedRuns);
            }
        }
    }

    @Path("contacts")
    public ContactsResource getContacts() {
        return contacts;
    }
    
    @GET
    @Produces("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @Path("výsledky.xlsx")
    public byte[] asXlsx() throws IOException {
        XlsGenerator doc = XlsGenerator.create(runs, contacts.allContacts());
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            doc.write(os);
            return os.toByteArray();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("admin")
    public Settings config(
        @QueryParam("name") String name
    ) {
        LOG.log(Level.FINE, "admin/config {0}", name);
        Settings data = new Settings().withName(name);
        return config(data);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("admin")
    public Settings config(
        @QueryParam("name") String name,
        Settings data
    ) {
        LOG.log(Level.FINE, "admin/config {0} = {1}", new Object[] { name, data });
        if (name != null && !name.isEmpty()) {
            data.setName(name);
        }
        return config(data);
    }

    private Settings config(Settings data) {
        if (!isValidName(data.getName())) {
            throw new WebApplicationException("Unacceptable name: " + data.getName(), Response.Status.NOT_ACCEPTABLE);
        }
        updateSettings(data);
        return settings();
    }

    private List<String> listMeasurements() {
        List<String> measurements = new LinkedList<>(Arrays.asList(storage.files()));
        measurements.remove("people.json");
        ListIterator<String> it = measurements.listIterator();
        while (it.hasNext()) {
            final String file = it.next();
            if (!file.endsWith(".json")) {
                it.remove();
                continue;
            }
            String base = file.substring(0, file.length() - 5);
            if (!isValidName(base)) {
                it.remove();
            } else {
                it.set(base);
            }
        }
        return measurements;
    }

    static boolean isValidName(String name) {
        for (char ch : name.toCharArray()) {
            if (Character.isAlphabetic(ch)) {
                continue;
            }
            if (Character.isDigit(ch)) {
                continue;
            }
            if (ch == ' ') {
                continue;
            }
            return false;
        }
        return true;
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
