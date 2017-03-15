package org.apidesign.gate.timing.server;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.util.Enumeration;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

/** Starts REST server based on Jersey.
 */
final class Main implements ContainerResponseFilter {
    public static void main(String... args) throws Exception {
        int port = 8080;
        if (args.length == 1) {
            port = Integer.parseInt(args[0]);
        }
        URI u = new URI("http://0.0.0.0:" + port + "/");
        HttpServer server = createServer(u);
        System.err.println("Server running on following IP addresses:");
        dumpIPs();
        System.err.println("Server running, press Ctrl-C to stop it.");
        try {
            synchronized (Main.class) {
                Main.class.wait();
            }
        } finally {
            server.shutdownNow();
        }
    }

    static HttpServer createServer(URI u) {
        ResourceConfig rc = new ResourceConfig(
            TimingResource.class, ContactsResource.class, Main.class
        );
        final Storage storage = new Storage();
        rc.registerInstances(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(storage).to(Storage.class);
            }
        });
        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(u, rc);
        return server;
    }

    @Override
    public void filter(
        ContainerRequestContext requestContext, 
        ContainerResponseContext r
    ) throws IOException {
        r.getHeaders().add("Access-Control-Allow-Origin", "*");
        r.getHeaders().add("Access-Control-Allow-Credentials", "true");
        r.getHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        r.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT");
    }    
    
    private static void dumpIPs() throws Exception {
        Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
        while (en.hasMoreElements()) {
            NetworkInterface n = en.nextElement();
            if (n.isUp()) {
                for (InterfaceAddress i : n.getInterfaceAddresses()) {
                    if (i.getAddress() instanceof Inet4Address) {
                        System.err.println(n.getName() + ": " + i.getAddress());
                    }
                }
            }
        }
    }
}
