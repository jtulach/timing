package org.apidesign.gate.timing.js;

import java.io.Closeable;
import java.io.IOException;
import net.java.html.js.JavaScriptBody;
import net.java.html.js.JavaScriptResource;

// copied from 
// https://dukescript.com/best/practices/2015/11/23/dynamic-templates.html
// example

@JavaScriptResource("templates.js")
public class TemplateRegistration {
    /**
     * Use this method to register a knockout template defined in an external
     * HTMl file. The following code will register a template defined in file
     * <i>"template1.html"</i> under id <i>"template1"</i>:
     * <br><br><br>
     * {@code Closeable template = TemplateRegistration.register("template1", "template1.html");}
     * <br><br>
     * The template can be unregistered by calling:
     * <br><br> {@code template.close();}
     * <br><br>
     * The call will return a {@link java.io.Closeable}. To make sure the
     * Template is not accidentialy unregistered, it can only be unregistered
     * via the original {@link java.io.Closeable}. Later calls to register under
     * the same id will return a no-op {@link java.io.Closeable}.
     * <br><br>
     * The intended use is to register Templates in a modular System (e.g.
     * OSGi). Like this each module can register it's templates upon activation
     * and deregister them when unloaded/uninstalled.
     * <br><br>
     * Registered templates are loaded lazily when first used in a template
     * binding, so the memory impact of loading a template is minimal. Closed
     * Templates are eligible for GarbageCollection to further optimize memory
     * usage.
     *
     * @param id id under which this template can be found.
     * @param template the src (relative to loaded page).
     * @return A closeable that can be called
     */
    public static Closeable register(String id, String template) throws IllegalStateException{
        final Object obj = registerTemplate_impl(id, template);
        if (obj == null) throw new IllegalStateException("Cannot register a template with id "+id +" and template "+template+" (trying to register twice?)");
        return new Closeable() {
            @Override
            public void close() throws IOException {
                unRegisterTemplate(obj);
            }
        };
    }

    @JavaScriptBody(args = {"id", "template"}, body = "var my_template = document.getElementById(id);\n"
            + "if ( my_template ) {\n"
            + "   return null;\n"
            + "}\n"
//            + "console.log('Registering a template for id '+id+ ' with src='+template);\n"
            + "my_template = document.createElement('script');\n"
            + "my_template.setAttribute('id', id);\n"
            + "my_template.type ='text/html';\n"
            + "document.body.appendChild(my_template);\n"
            + "my_template.src = template;\n"
            + "return document.getElementById(id);")
    private static native Object registerTemplate_impl(String id, String template);

    @JavaScriptBody(args = {"template"}, body
            =  "if ( template ) {\n"
            + "    if (document.contains(template)){"
                    + "    document.body.removeChild(template);\n"
                    + "}\n"
                    + "else{\n"
                    + "    console.warn('Doing nothing. This template has already been removed.');"
                    + "}\n"
            + "}\n"
    )
    private static native void unRegisterTemplate(Object template);

}