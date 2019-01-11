package org.apidesign.gate.timing;

import java.util.Timer;
import java.util.TimerTask;
import net.java.html.json.Model;
import net.java.html.json.ModelOperation;
import net.java.html.json.Property;

@Model(className="Current", instance = true, properties = {
    @Property(name = "millis", type = long.class)
})
final class CurrentModel {
    private static final Timer MILLIS = new Timer("Millis");
    private PingMillis task;
    
    @ModelOperation
    void start(Current model) {
        if (task == null) {
            task = new PingMillis(model);
            MILLIS.scheduleAtFixedRate(task, 10, 10);
        }
    }
    
    @ModelOperation
    void stop(Current model) {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
    
    @ModelOperation
    void updateTime(Current model) {
        model.setMillis(System.currentTimeMillis());
    }
    
    private static final class PingMillis extends TimerTask {
        private final Current model;

        PingMillis(Current model) {
            this.model = model;
        }
        
        @Override
        public void run() {
            this.model.updateTime();
        }
        
    }
}
