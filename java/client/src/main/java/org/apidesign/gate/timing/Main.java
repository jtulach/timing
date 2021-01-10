package org.apidesign.gate.timing;

import net.java.html.boot.BrowserBuilder;

public final class Main {
    private Main() {
    }

    public static void main(String... args) throws Exception {
        String page = args.length > 0 ? args[0] : "start.html";
        BrowserBuilder.newBrowser().
            loadPage("pages/" + page).
            loadClass(Main.class).
            invoke("onPageLoad").
            showAndWait();
        System.exit(0);
    }

    /**
     * Called when the page is ready.
     */
    public static void onPageLoad() throws Exception {
        UIModel.onPageLoad();
    }

}
