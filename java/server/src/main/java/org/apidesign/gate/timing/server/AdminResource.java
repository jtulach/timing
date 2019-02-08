package org.apidesign.gate.timing.server;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apidesign.gate.timing.shared.Settings;

@Singleton
public class AdminResource {
    @Inject
    private Storage storage;
    private TimingResource main;

    void register(TimingResource tr) {
        assert main == null;
        main = tr;
    }


    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Settings config(
        @QueryParam("name") String name,
        Settings data
    ) {
        if (name != null && !name.isEmpty()) {
            data.setName(name);
        }
        return config(data);
    }

    private Settings config(Settings data) {
        if (!isValidName(data.getName())) {
            throw new WebApplicationException("Unacceptable name: " + data.getName(), Response.Status.NOT_ACCEPTABLE);
        }
        main.updateSettings(data);

        List<String> measurements = listMeasurements();
        final Settings setup = main.settings();
        setup.getMeasurements().clear();
        setup.getMeasurements().addAll(measurements);
        return setup;
    }

    private List<String> listMeasurements() {
        List<String> measurements = new LinkedList<>(Arrays.asList(storage.files()));
        measurements.remove("people.json");
        ListIterator<String> it = measurements.listIterator();
        while (it.hasNext()) {
            final String file = it.next();
            if (!file.endsWith(".json")) {
                it.remove();
                continue;
            }
            String base = file.substring(0, file.length() - 5);
            if (!isValidName(base)) {
                it.remove();
            } else {
                it.set(base);
            }
        }
        return measurements;
    }

    static boolean isValidName(String name) {
        for (char ch : name.toCharArray()) {
            if (Character.isAlphabetic(ch)) {
                continue;
            }
            if (Character.isDigit(ch)) {
                continue;
            }
            if (ch == ' ') {
                continue;
            }
            return false;
        }
        return true;
    }
}
