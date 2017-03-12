package org.apidesign.gate.timing.server;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.GenericType;
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
import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;

public class TimingResourceTest {

    private final GenericType<List<Event>> eventType = new GenericType<List<Event>>() {};
    private HttpServer server;
    private URI baseUri;

    public TimingResourceTest() {
    }

    @Before
    public void setUpMethod() throws Exception {
        ServerSocket socket = new ServerSocket(0);
        int emptyPort = socket.getLocalPort();
        socket.close();

        URI serverURI = new URI("http://0.0.0.0:" + emptyPort + "/");
        server = Main.createServer(serverURI);
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
        assertEquals("INITIALIZED", list.get(0).getType());
    }

    @Test
    public void testAddEvents() {
        Client client = new Client();
        long when = System.currentTimeMillis() + 300;
        WebResource add = client.resource(baseUri.resolve("add")).queryParam("type", "FINISH").queryParam("when", "" + when);
        Event addedEvent = add.get(Event.class);
        assertNotNull(addedEvent);
        assertEquals(when, addedEvent.getWhen());
        assertEquals("FINISH", addedEvent.getType());

        WebResource resource = client.resource(baseUri);
        List<Event> list = resource.get(new GenericType<List<Event>>() {});
        assertEquals("Two elements " + list, 2, list.size());

        assertEquals("First one is the added event", addedEvent, list.get(0));
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
        assertEquals("FINISH", addedEvent.getType());

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

    private ExecutorService EXEC;
    private <T> Future<T> async(Callable<T> r) {
        if (EXEC == null) {
            EXEC = Executors.newFixedThreadPool(3);
        }
        return EXEC.submit(r);
    }
}
