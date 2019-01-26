package org.apidesign.gate.timing.server;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import java.net.ServerSocket;
import java.net.URI;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apidesign.gate.timing.shared.Event;
import org.apidesign.gate.timing.shared.Events;
import org.apidesign.gate.timing.shared.Run;
import org.apidesign.gate.timing.shared.RunInfo;
import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;

public class TimingResourceTest {
    private final GenericType<List<Event>> eventType = new GenericType<List<Event>>() {};
    private final GenericType<List<Run>> runType = new GenericType<List<Run>>() {};
    private HttpServer server;
    private URI baseUri;

    public TimingResourceTest() {
    }

    @Before
    public void setUpMethod() throws Exception {
        int emptyPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            emptyPort = socket.getLocalPort();
        }

        System.setProperty("user.dir", "");

        URI serverURI = new URI("http://0.0.0.0:" + emptyPort + "/");
        server = Main.createServer(serverURI, null);
        baseUri = serverURI.resolve("timing/");
    }

    @After
    public void tearDownMethod() throws Exception {
        server.shutdownNow();
    }

    @Test
    public void testInitialEvents() {
        Client client = new Client();
        WebResource resource = client.resource(baseUri);
        List<Event> list = resource.get(new GenericType<List<Event>>() {});
        assertEquals("One element " + list, 1, list.size());
        assertEquals(Events.INITIALIZED, list.get(0).getType());
    }

    @Test
    public void testAddEvents() {
        Client client = new Client();
        long when = System.currentTimeMillis() + 300;
        WebResource add = client.resource(baseUri.resolve("add")).queryParam("type", "FINISH").queryParam("when", "" + when);
        Event addedEvent = add.get(Event.class);
        assertNotNull(addedEvent);
        assertEquals(when, addedEvent.getWhen());
        assertEquals(Events.FINISH, addedEvent.getType());

        WebResource resource = client.resource(baseUri);
        List<Event> list = resource.get(new GenericType<List<Event>>() {});
        assertEquals("Two elements " + list, 2, list.size());

        assertEquals("First one is the added event", addedEvent, list.get(0));
    }

    @Test
    public void updatingWhoDidAnEvent() {
        Client client = new Client();
        long when;
        {
            WebResource resource = client.resource(baseUri);
            List<Event> list = resource.get(new GenericType<List<Event>>() {});
            assertEquals("One element " + list, 1, list.size());
            assertEquals("First one is INITIALIZED", Events.INITIALIZED, list.get(0).getType());
            when = list.get(0).getWhen() + 100;
        }
        WebResource add = client.resource(baseUri.resolve("add")).queryParam("type", "FINISH").queryParam("when", "" + when);
        Event addedEvent = add.get(Event.class);
        assertNotNull(addedEvent);
        assertEquals(when, addedEvent.getWhen());
        assertEquals(Events.FINISH, addedEvent.getType());
        assertEquals("Assigned to nobody", 0, addedEvent.getWho());

        {
            WebResource resource = client.resource(baseUri);
            List<Event> list = resource.get(new GenericType<List<Event>>() {});
            assertEquals("Two elements " + list, 2, list.size());
            assertEquals("First one is the added event", addedEvent, list.get(0));
        }


        WebResource update = client.resource(baseUri.resolve("add")).
            queryParam("type", "ASSIGN").
            queryParam("when", "" + (when + 100)).
            queryParam("ref", "" + addedEvent.getId()).
            queryParam("who", "33");
        Event updatedEvent = update.get(Event.class);

        assertEquals("Same good ref", addedEvent.getId(), updatedEvent.getRef());
        assertEquals(33, updatedEvent.getWho());
        assertEquals("New event", addedEvent.getId() + 1, updatedEvent.getId());

        {
            WebResource resource = client.resource(baseUri);
            List<Event> list = resource.get(new GenericType<List<Event>>() {});
            assertEquals("Three elements " + list, 3, list.size());
            assertEquals("First one is the updated event", updatedEvent, list.get(0));
        }
    }

    @Test
    public void updatingWhoDidAnEventAndRef() {
        Client client = new Client();
        long when;
        {
            WebResource resource = client.resource(baseUri);
            List<Event> list = resource.get(new GenericType<List<Event>>() {});
            assertEquals("One element " + list, 1, list.size());
            assertEquals("First one is INITIALIZED", Events.INITIALIZED, list.get(0).getType());
            when = list.get(0).getWhen() + 300;
        }
        WebResource add = client.resource(baseUri.resolve("add")).queryParam("type", "FINISH").queryParam("when", "" + when);
        Event addedEvent = add.get(Event.class);
        assertNotNull(addedEvent);
        assertEquals(when, addedEvent.getWhen());
        assertEquals(Events.FINISH, addedEvent.getType());
        assertEquals("Assigned to nobody", 0, addedEvent.getWho());

        {
            WebResource resource = client.resource(baseUri);
            List<Event> list = resource.get(new GenericType<List<Event>>() {});
            assertEquals("Two elements " + list, 2, list.size());
            assertEquals("First one is the added event", addedEvent, list.get(0));
        }


        WebResource update = client.resource(baseUri.resolve("add")).
            queryParam("type", "ASSIGN").
            queryParam("when", "" + (when + 400)).
            queryParam("ref", "" + addedEvent.getId()).
            queryParam("who", "77");
        Event updatedEvent = update.get(Event.class);

        assertEquals("Same ref", addedEvent.getId(), updatedEvent.getRef());
        assertEquals(77, updatedEvent.getWho());

        {
            WebResource resource = client.resource(baseUri);
            List<Event> list = resource.get(new GenericType<List<Event>>() {});
            assertEquals("Three elements " + list, 3, list.size());
            assertEquals("First one is the updated event", updatedEvent, list.get(0));
        }
    }

    @Test
    public void testWaitForNewEvents() throws Exception {
        Client client = new Client();

        WebResource resource = client.resource(baseUri);
        List<Event> list = resource.get(eventType);
        assertEquals("One element " + list, 1, list.size());
        Event initialized = list.get(0);

        final long now = initialized.getWhen();
        Future<List<Event>> request0 = async(() -> {
            return client.resource(baseUri).queryParam("newerThan", "" + now).get(eventType);
        });

        Future<List<Event>> request300 = async(() -> {
            final long nowPlus300 = now + 300;
            return client.resource(baseUri).queryParam("newerThan", "" + nowPlus300).get(eventType);
        });

        try {
            List<Event> noNewer = request0.get(100, TimeUnit.MILLISECONDS);
            fail("We shouldn't get an answer: " + noNewer);
        } catch (TimeoutException timeoutException) {
            // OK
        }

        try {
            List<Event> noNewer = request300.get(100, TimeUnit.MILLISECONDS);
            fail("We shouldn't get an answer: " + noNewer);
        } catch (TimeoutException timeoutException) {
            // OK
        }

        long now100 = now + 100;
        WebResource add = client.resource(baseUri.resolve("add")).queryParam("type", "FINISH").queryParam("when", "" + now100);
        Event addedEvent = add.get(Event.class);
        assertNotNull(addedEvent);
        assertEquals(now100, addedEvent.getWhen());
        assertEquals(Events.FINISH, addedEvent.getType());

        List<Event> newEventAt100 = request0.get(1000, TimeUnit.MILLISECONDS);
        assertNotNull(newEventAt100);
        assertEquals(1, newEventAt100.size());
        assertEquals("It is the added event", addedEvent, newEventAt100.get(0));

        try {
            List<Event> noNewer = request300.get(100, TimeUnit.MILLISECONDS);
            fail("Still no answer for +300ms: " + noNewer);
        } catch (TimeoutException timeoutException) {
            // OK
        }

        long now500 = now + 500;
        Event ev500 = client.resource(baseUri.resolve("add")).queryParam("type", "START").queryParam("when", "" + now500).get(Event.class);

        assertEquals("+300ms event delivered", ev500, request300.get().get(0));
    }

    @Test
    public void testWaitForNewRecords() throws Exception {
        Client client = new Client();

        WebResource resource = client.resource(baseUri);
        List<Event> list = resource.get(eventType);
        assertEquals("One element " + list, 1, list.size());
        Event initialized = list.get(0);

        final long now = initialized.getWhen();
        Future<List<Run>> request0 = async(() -> {
            return loadRuns(client, now);
        });

        Future<List<Run>> request300 = async(() -> {
            return loadRuns(client, now + 300);
        });

        try {
            List<Run> noNewer = request0.get(100, TimeUnit.MILLISECONDS);
            fail("We shouldn't get an answer: " + noNewer);
        } catch (TimeoutException timeoutException) {
            // OK
        }

        try {
            List<Run> noNewer = request300.get(100, TimeUnit.MILLISECONDS);
            fail("We shouldn't get an answer: " + noNewer);
        } catch (TimeoutException timeoutException) {
            // OK
        }

        long now100 = now + 100;
        WebResource add = client.resource(baseUri.resolve("add")).queryParam("type", "START").queryParam("when", "" + now100);
        Event addedEvent = add.get(Event.class);
        assertNotNull(addedEvent);
        assertEquals(now100, addedEvent.getWhen());
        assertEquals(Events.START, addedEvent.getType());

        List<Run> newEventAt100 = request0.get(1000, TimeUnit.MILLISECONDS);
        assertNotNull(newEventAt100);
        assertEquals(1, newEventAt100.size());
        assertEquals("It is the added event", addedEvent, newEventAt100.get(0).getStart());

        try {
            List<Run> noNewer = request300.get(100, TimeUnit.MILLISECONDS);
            fail("Still no answer for +300ms: " + noNewer);
        } catch (TimeoutException timeoutException) {
            // OK
        }

        long now500 = now + 500;
        Event ev500 = client.resource(baseUri.resolve("add")).queryParam("type", "FINISH").queryParam("when", "" + now500).get(Event.class);

        assertEquals("+300ms event delivered", ev500, request300.get().get(0).getFinish());

        long now700 = now + 700;
        Event ev700 = client.resource(baseUri.resolve("add")).queryParam("type", "START").queryParam("when", "" + now700).get(Event.class);

        Future<List<Run>> request700 = async(() -> {
            return loadRuns(client, now + 700);
        });

        try {
            List<Run> noNewer = request700.get(100, TimeUnit.MILLISECONDS);
            fail("Still no answer for +500ms: " + noNewer);
        } catch (TimeoutException timeoutException) {
            // OK
        }

        List<Run> runsBeforeIgnoringEvent = loadRuns(client, 0);
        assertEquals("Two runs: " + runsBeforeIgnoringEvent, 2, runsBeforeIgnoringEvent.size());
        assertFinished(400, runsBeforeIgnoringEvent.get(1));
        assertFinished(-1, runsBeforeIgnoringEvent.get(0));

        long now1000 = now + 1000;
        Event evIgnore = client.resource(baseUri.resolve("add")).queryParam("type", "IGNORE").queryParam("when", "" + now1000).queryParam("ref", "" + ev500.getId()).get(Event.class);
        assertNotNull(evIgnore);

        List<Run> runsAfterIgnoringEvent = loadRuns(client, 0);
        assertEquals("Two: " + runsAfterIgnoringEvent, 2, runsAfterIgnoringEvent.size());

        List<Run> incrementalRuns = request700.get();
        assertEquals("Two incremental: " + incrementalRuns, 2, incrementalRuns.size());
    }



    @Test
    public void testSequenceOfStartsAndFinish() throws Exception {
        Client client = new Client();

        long now = System.currentTimeMillis() - 3600 * 1000;
        Event start1 = sendEvent(client, "START", now + 100);
        Event start2 = sendEvent(client, "START", now + 200);
        Event start3 = sendEvent(client, "START", now + 300);
        Event start4 = sendEvent(client, "START", now + 400);

        Event finish1 = sendEvent(client, "FINISH", now + 1000);
        Event finish2 = sendEvent(client, "FINISH", now + 2000);

        List<Run> runs1 = loadRuns(client, 0);
        assertEquals("Four: " + runs1, 4, runs1.size());

        assertFinished(900, runs1.get(3));
        assertFinished(1800, runs1.get(2));

        Future<List<Run>> request3000 = async(() -> {
            return loadRuns(client, now + 3000);
        });

        try {
            List<Run> noNewer = request3000.get(100, TimeUnit.MILLISECONDS);
            fail("Still no answer for +3s: " + noNewer);
        } catch (TimeoutException timeoutException) {
            // OK
        }

        Event ev5000 = client.resource(baseUri.resolve("add")).queryParam("type", "FINISH").queryParam("when", "" + (now + 5000)).get(Event.class);
        assertNotNull(ev5000);


        List<Run> incrementalRuns = request3000.get();
        assertEquals("Last two incrementally delivered: " + incrementalRuns, 2, incrementalRuns.size());

        assertFinished(-1, incrementalRuns.get(0), 4);
        assertFinished(4700, incrementalRuns.get(1), 3);
    }

    private List<Run> loadRuns(Client client, long newerThan) throws ClientHandlerException, UniformInterfaceException {
        WebResource resource = client.resource(baseUri).path("runs");
        if (newerThan > 0) {
            resource = resource.queryParam("newerThan", "" + newerThan);
        }
        return resource.get(RunInfo.class).getRuns();
    }

    private Event sendEvent(Client client, String type, final long at) {
        return client.resource(baseUri.resolve("add")).queryParam("type", type).queryParam("when", "" + at).get(Event.class);
    }

    private static void assertFinished(long time, Run run, int... id) {
        assertNotNull("Started", run.getStart());
        if (time == -1) {
            assertNull("Not finished", run.getFinish());
            return;
        }

        assertNotNull("Finished", run.getFinish());

        long took = run.getFinish().getWhen() - run.getStart().getWhen();

        assertEquals(time, took);

        if (id.length > 0) {
            assertEquals(id[0], run.getId());
        }
    }

    private ExecutorService EXEC;
    private <T> Future<T> async(Callable<T> r) {
        if (EXEC == null) {
            EXEC = Executors.newFixedThreadPool(3);
        }
        return EXEC.submit(r);
    }
}
