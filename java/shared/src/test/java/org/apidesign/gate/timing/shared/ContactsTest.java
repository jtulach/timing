package org.apidesign.gate.timing.shared;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

public class ContactsTest {

    public ContactsTest() {
    }

    @Test
    public void mustHaveFirstName() {
        Contact c = new Contact().withId(0);
        assertNotNull(c.getValidate());
    }

    @Test
    public void okName() {
        Contact c = new Contact().withId(0).withName("first");
        assertNull(c.getValidate());
    }

    @Test
    public void unlinkTest() {
        Contact c = new Contact().withAliases("a", "b", "c");
        Contacts.unalias(c, "b");
        assertEquals(2, c.getAliases().size());
        assertEquals("a", c.getAliases().get(0));
        assertEquals("c", c.getAliases().get(1));
    }

}
