package org.apidesign.gate.timing.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.java.html.BrwsrCtx;
import net.java.html.json.Models;

final class Storage {
    private static Logger LOG = Logger.getLogger(Storage.class.getName());
    private final File dir;
    private final Executor storage;

    Storage() {
        dir = new File(System.getProperty("user.dir"));
        dir.mkdirs();
        storage = Executors.newFixedThreadPool(1);
    }

    public <T> void scheduleStore(String prefix, Class<T> type, Collection<T> data) {
        ArrayList<T> copy = new ArrayList<T>(data);
        storage.execute(() -> {
            try {
                storeData(prefix, type, copy);
            } catch (IOException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        });
    }

    private <T> void storeData(String prefix, Class<T> type, List<T> data) throws IOException {
        File existing = new File(dir, prefix + ".json");
        File created = new File(dir, prefix + ".new");
        FileOutputStream os = new FileOutputStream(created);
        os.write('[');
        boolean sep = false;
        for (T t : data) {
            if (sep) {
                os.write(',');
            }
            os.write('\n');
            os.write(' ');
            os.write(' ');
            os.write(t.toString().getBytes("UTF-8"));
            sep = true;
        }
        os.write('\n');
        os.write(']');
        os.write('\n');
        os.getFD().sync();
        os.close();
        existing.delete();
        created.renameTo(existing);
    }

    <T> void readInto(String prefix, Class<T> type, Collection<T> collectTo) throws IOException {
        File readFrom = new File(dir, prefix + ".json");
        if (!readFrom.exists()) {
            readFrom = new File(dir, prefix + ".new");
            if (!readFrom.exists()) {
                return;
            }
        }

        try (FileInputStream is = new FileInputStream(readFrom)) {
            BrwsrCtx def = BrwsrCtx.findDefault(Storage.class);
            Models.parse(def, type, is, collectTo);
        }
    }
}
