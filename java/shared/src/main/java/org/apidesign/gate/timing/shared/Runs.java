package org.apidesign.gate.timing.shared;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.NavigableSet;
import net.java.html.json.ComputedProperty;
import net.java.html.json.Model;
import net.java.html.json.ModelOperation;
import net.java.html.json.Property;

@Model(className = "Run", builder = "with", properties = {
    @Property(name = "start", type = Event.class),
    @Property(name = "finish", type = Event.class),
    @Property(name = "ignore", type = boolean.class),
    @Property(name = "who", type = int.class),
    @Property(name = "id", type = int.class),
})
public final class Runs {
    @ModelOperation
    static void empty(Run model) {
        model.withStart(null).withFinish(null).withIgnore(false);
    }
    
    @ComputedProperty
    static long when(Event start, Event finish) {
        long at = 0L;
        if (start != null && start.getWhen() > at) {
            at = start.getWhen();
        }
        if (finish != null && finish.getWhen() > at) {
            at = finish.getWhen();
        }
        return at;
    }

    public static List<Run> compute(NavigableSet<Event> arr) {
        return compute(arr, false);
    }
    static List<Run> compute(NavigableSet<Event> set, boolean onlyValid) {
        LinkedHashMap<Integer, Run> run = new LinkedHashMap<>();
        LinkedList<Run> running = new LinkedList<>();
        
        Iterator<Event> it = set.descendingIterator();
        while (it.hasNext()) {
            Event ev = it.next();
            switch (ev.getType()) {
                case "START": {
                    Run r = new Run();
                    r.empty();
                    r.withStart(ev);
                    Run prev = run.put(ev.getId(), r);
                    assert prev == null;
                    running.add(r);
                    break;
                }
                case "FINISH": {
                    if (running.isEmpty()) {
                        continue;
                    }
                    Run r = running.removeFirst();
                    r.withFinish(ev);
                    Run prev = run.put(ev.getId(), r);
                    assert prev == null;
                    break;
                }
                case "IGNORE": {
                    Run r = run.get(ev.getRef());
                    if (r != null) {
                        r.setFinish(null);
                        ListIterator<Run> search = running.listIterator();
                        while (search.hasNext()) {
                            Run inThere = search.next();
                            if (inThere.getWhen() > r.getWhen()) {
                                search.previous();
                                search.add(r);
                                r = null;
                                break;
                            }
                        }
                        if (r != null) {
                            running.add(r);
                        }
                    }
                    break;
                }
                case "ASSIGN": {
                    Run r = run.get(ev.getRef());
                    if (r != null) {
                        r.setWho(ev.getWho());
                    }
                    break;
                }
            }            
        }
        LinkedList<Run> res = new LinkedList<>();
        int cnt = 0;
        Iterator<Run> runsIt = run.values().iterator();
        while (runsIt.hasNext()) {
            Run r = runsIt.next();
            if (r.getId() != 0) {
                continue;
            }
            r.setId(++cnt);
            res.addFirst(r);
        }
        return res;
    }
}
