package org.apidesign.gate.timing.server;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import java.net.ServerSocket;
import java.net.URI;
import java.util.List;
import org.apidesign.gate.timing.shared.Event;
import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

public class TimingResourceTest {

    private HttpServer server;
    private URI baseUri;

    public TimingResourceTest() {
    }

    @Before
    public void setUpMethod() throws Exception {
        ServerSocket socket = new ServerSocket(0);
        int emptyPort = socket.getLocalPort();
        socket.close();

        URI serverURI = new URI("http://0.0.0.0:" + emptyPort);
        server = Main.createServer(serverURI);
        baseUri = serverURI.resolve("/timing");
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
        assertEquals("STARTED", list.get(0).getType());
    }

}
