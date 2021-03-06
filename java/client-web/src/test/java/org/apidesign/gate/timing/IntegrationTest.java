package org.apidesign.gate.timing;

import net.java.html.junit.BrowserRunner;
import net.java.html.junit.HTMLContent;
import org.apidesign.gate.timing.shared.Contact;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests for behavior of your application in real systems. The {@link BrowserRunner}
 * selects all possible presenters from your <code>pom.xml</code> and
 * runs the tests inside of them. By default there are two:
 * <ul>
 *   <li>JavaFX WebView presenter - verifies behavior in HotSpot JVM</li>
 *   <li>Bck2Brwsr presenter - runs the test in a pluginless browser</li>
 * </ul>
 *
 * See your <code>pom.xml</code> dependency section for details.
 */
@RunWith(BrowserRunner.class)
@HTMLContent(
    "<h3>Test in JavaFX WebView and pluginless Browser</h3>\n" +
    "<span data-bind='text: message'></span>\n" +
    "<ul data-bind='foreach: contacts'>\n" +
    "  <li>\n" +
    "    <span data-bind='text: name'></span>\n" +
    "  </li>\n" +
    "</ul>\n" +
    "\n"
)
public class IntegrationTest {
    @Test public void testUIModelUI() {
        UI model = new UI();
        model.cancel();
        model.applyBindings();
        model.getContacts().clear();
        model.getContacts().add(new Contact().withId(1).withName("Toni"));
        model.getContacts().add(new Contact().withId(2).withName("Joe"));
        model.getContacts().add(new Contact().withId(3).withName("Duke"));

        assertEquals("Joe", model.getContacts().get(1).getName());
    }
}
