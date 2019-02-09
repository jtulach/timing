package org.apidesign.gate.timing.server.xsl;

import java.io.IOException;
import java.io.InputStream;
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
    public void generateXls() {
        Running running = Runs.compute(events, 23000, 42000);
        List<XlsGenerator.Row> rows = XlsGenerator. create(running, contacts);
        System.err.println("Jméno               Nejlepší\tPrůměr\tČas v kolech");
        for (XlsGenerator.Row r : rows) {
            String name20 = (r.name + "                   ").substring(0, 20);
            System.err.print(name20 + Time.toString(r.minimum) + "\t" + Time.toString(r.average));
            for (Long time : r.times) {
                System.err.print("\t");
                System.err.print(Time.toString(time));
            }
            System.err.println();
        }
    }
}
