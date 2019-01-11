package org.apidesign.gate.timing;

import java.util.Timer;
import java.util.TimerTask;
import net.java.html.json.ComputedProperty;
import net.java.html.json.Model;
import net.java.html.json.ModelOperation;
import net.java.html.json.Property;

@Model(className = "StopWatch", instance = true, properties = {
    @Property(name = "start", type = long.class),
    @Property(name = "current", type = long.class),
    @Property(name = "stop", type = long.class),
    @Property(name = "who", type = Avatar.class)
})
final class StopWatchModel {
    private static final Timer TIMER = new Timer("StopWatch", true);
    private StopWatch model;

    @ModelOperation
    void start(StopWatch model, long now) {
        this.model = model;
        model.setStart(now);
        TIMER.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                model.updateCurrent();
            }
        }, 500, 10);
    }
    
    @ModelOperation
    void stop(StopWatch model, long now) {
        model.setStop(now);
    }
    
    @ComputedProperty
    static String seconds(long start, long stop, long current) {
        long actual = stop;
        if (stop > 0) {
        } else {
            actual = current;
        }
        if (actual < start) {
            return "--";
        }
        long time = (actual - start) / 1000L;
        String digits = Long.toString(time);
        if (digits.length() < 2) {
            digits = "0" + digits;
        }
        return digits;
    }
    
    @ComputedProperty
    static String hundreds(long start, long stop, long current) {
        long actual = stop;
        if (stop > 0) {
        } else {
            actual = current;
        }
        if (actual < start) {
            return "--";
        }
        long time = actual - start;
        String digits = Long.toString(time % 100);
        if (digits.length() < 2) {
            digits = "0" + digits;
        }
        return digits;
    }

    @ModelOperation
    public void updateCurrent() {
        long now = System.currentTimeMillis();
        model.setCurrent(now);
    }
}
