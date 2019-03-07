package org.apidesign.gate.timing.shared;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;
import net.java.html.json.ComputedProperty;
import net.java.html.json.Model;
import net.java.html.json.ModelOperation;
import net.java.html.json.Property;

@Model(className = "Running", builder = "with", properties = {
    @Property(name = "timestamp", type = long.class),
    @Property(name = "starting", type = int.class),
    @Property(name = "settings", type = Settings.class),
    @Property(name = "identities", type = String.class, array = true),
    @Property(name = "runs", type = Run.class, array = true),
})
public final class Runs {

    @Model(className = "Run", builder = "with", properties = {
        @Property(name = "start", type = Event.class),
        @Property(name = "finish", type = Event.class),
        @Property(name = "ignore", type = boolean.class),
        @Property(name = "who", type = int.class),
        @Property(name = "id", type = int.class),
    })
    static final class SingleRun {
        @ModelOperation
        static void empty(Run model) {
            model.withStart(null).withFinish(null).withIgnore(false);
        }

        @ComputedProperty
        static boolean dnf(Event start, Event finish) {
            return start != null && start.equals(finish);
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
    }

    private static int findFollower(List<Run> runs, int who) {
        int id = -1;
        boolean nextOne = false;
        for (Run r : runs) {
            if (nextOne) {
                if (r.getWho() >= 0) {
                    id = r.getWho();
                }
                nextOne = false;
            }
            if (r.getWho() == who) {
                nextOne = true;
            }
        }
        return id;
    }

    public static Running compute(NavigableSet<Event> set) {
        return compute(set, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    public static Running compute(NavigableSet<Event> set, long min, long max) {
        LinkedHashMap<Integer, Run> run = new LinkedHashMap<>();
        LinkedList<Run> running = new LinkedList<>();
        TreeSet<String> identities = new TreeSet<>();

        long newestEvent = 0;
        int nextOnStart = -1;
        Iterator<Event> it = set.descendingIterator();
        while (it.hasNext()) {
            Event ev = it.next();
            if (ev.getWhen() > newestEvent) {
                newestEvent = ev.getWhen();
            }
            for (;;) {
                Run r = running.peekFirst();
                if (r == null) {
                    break;
                }
                long sinceR = newestEvent - r.getWhen();
                if (sinceR > max) {
                    running.remove();
                    r.setFinish(r.getStart());
                } else {
                    break;
                }
            }

            switch (ev.getType()) {
                case START: {
                    Run r = new Run();
                    r.empty();
                    r.withStart(ev);
                    if (nextOnStart >= 0) {
                        r.setWho(nextOnStart);
                        nextOnStart = findFollower(running, nextOnStart);
                    }
                    Run prev = run.put(ev.getId(), r);
                    assert prev == null;
                    running.add(r);
                    break;
                }
                case FINISH: {
                    if (running.isEmpty()) {
                        continue;
                    }
                    Run r = running.peekFirst();
                    long took = ev.getWhen() - r.getStart().getWhen();
                    if (took < min || took > max) {
                        continue;
                    }
                    r.withFinish(ev);
                    Run prev = run.put(ev.getId(), r);
                    running.removeFirst();
                    assert prev == null;
                    break;
                }
                case IGNORE: {
                    Run r = run.get(ev.getRef());
                    if (r != null) {
                        if (r.getFinish() != null && ev.getRef() == r.getFinish().getId()) {
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
                        } else {
                            running.remove(r);
                            Iterator<Map.Entry<Integer, Run>> removeIt = run.entrySet().iterator();
                            while (removeIt.hasNext()) {
                                if (removeIt.next().getValue() == r) {
                                    removeIt.remove();
                                }
                            }
                        }
                    }
                    break;
                }
                case ASSIGN: {
                    if (ev.getRef() == -1) {
                        nextOnStart = ev.getWho();
                    }
                    Run r = run.get(ev.getRef());
                    if (r != null) {
                        r.setWho(ev.getWho());
                    }
                    identities.add("" + ev.getWho());
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
        return new Running().
            withRuns(res.toArray(new Run[res.size()])).
            withIdentities(identities.toArray(new String[identities.size()])).
            withStarting(nextOnStart).
            withTimestamp(newestEvent);
    }
}
