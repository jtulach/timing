package org.apidesign.gate.timing.server.xsl;

import org.apidesign.gate.timing.server.XlsGenerator;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;
import net.java.html.BrwsrCtx;
import net.java.html.json.Models;
import org.apidesign.gate.timing.shared.Contact;
import org.apidesign.gate.timing.shared.Event;
import org.apidesign.gate.timing.shared.Events;
import org.apidesign.gate.timing.shared.Running;
import org.apidesign.gate.timing.shared.Runs;
import org.apidesign.gate.timing.shared.Time;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public class GenerateXlsTest {
    private List<Contact> contacts;
    private NavigableSet<Event> events;

    @Before
    public void loadContacts() throws IOException {
        InputStream is = getClass().getResourceAsStream("contacts.json");
        Assert.assertNotNull("contacts found", is);
        BrwsrCtx ctx = BrwsrCtx.findDefault(GenerateXlsTest.class);
        contacts = new ArrayList<>();
        Models.parse(ctx, Contact.class, is, contacts);
    }

    @Before
    public void loadEvents() throws IOException {
        InputStream is = getClass().getResourceAsStream("pizaar.json");
        Assert.assertNotNull("events found", is);
        BrwsrCtx ctx = BrwsrCtx.findDefault(GenerateXlsTest.class);
        events = new TreeSet<>(Events.COMPARATOR);
        Models.parse(ctx, Event.class, is, events);
    }

    @Test
    public void generateXls() throws Exception {
        Running running = Runs.compute(events, 23000, 42000);
        final XlsGenerator generator = XlsGenerator. create(running, contacts);
        String res = generator.toString();
        assertNotEquals(res, -1, res.indexOf("Hela"));
        assertNotEquals(res, -1, res.indexOf("Deny"));
        assertNotEquals(res, -1, res.indexOf("26:05"));
        assertNotEquals(res, -1, res.indexOf("--:--"));
        assertNotEquals(res, -1, res.indexOf("DNF"));
        assertNotEquals(res, -1, res.indexOf("40:47"));


        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            generator.write(os);
            assertTrue("4KB at least: " + os.size(), os.size() >= 4000);
        }
    }
}
