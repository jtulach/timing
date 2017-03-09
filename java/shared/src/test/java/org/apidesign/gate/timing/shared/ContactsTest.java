package org.apidesign.gate.timing.shared;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

public class ContactsTest {

    public ContactsTest() {
    }

    @Test
    public void mustHaveFirstName() {
        Contact c = new Contact("0", null, null);
        assertNotNull(c.getValidate());
    }

    @Test
    public void okName() {
        Contact c = new Contact("0", "first", null);
        assertNull(c.getValidate());
    }

}
