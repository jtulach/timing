package org.apidesign.gate.timing.server.xsl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import org.apidesign.gate.timing.shared.Contact;
import org.apidesign.gate.timing.shared.Run;
import org.apidesign.gate.timing.shared.Running;

public final class XlsGenerator {
    private final List<Row> names;

    private XlsGenerator(List<Row> names) {
        this.names = names;
    }

    public static List<Row> create(Running stats, List<Contact> contacts) {
        Map<Integer, List<Long>> idToTimes = new HashMap<>();
        int round = 0;
        ListIterator<Run> it = stats.getRuns().listIterator(stats.getRuns().size() - 1);
        while (it.hasPrevious()) {
            Run r = it.previous();
            if (r.isIgnore()) {
                continue;
            }

            final int whoId = r.getWho();
            List<Long> times = idToTimes.get(whoId);
            if (times == null) {
                times = new ArrayList<>();
                idToTimes.put(whoId, times);
            }

            while (times.size() <= round) {
                times.add(null);
            }

            if (r.getFinish() == null || r.getStart() == null) {
                continue;
            }
            long took = r.getFinish().getWhen() - r.getStart().getWhen();
            if (took < stats.getSettings().getMinMillis()) {
                continue;
            }
            if (took > stats.getSettings().getMaxMillis()) {
                continue;
            }

            if (times.get(round) != null) {
                times.add(null);
                round++;
            }

            times.set(round, took);
        }

        Map<Integer, Long> average = new HashMap<>();
        Map<Integer, Long> minimum = new HashMap<>();
        for (Map.Entry<Integer, List<Long>> entry : idToTimes.entrySet()) {
            long sum = 0;
            int cnt = 0;
            long min = Long.MAX_VALUE;
            for (Long v : entry.getValue()) {
                if (v == null || v == 0L) {
                    continue;
                }
                sum += v;
                if (v < min) {
                    min = v;
                }
                cnt++;
            }
            if (cnt == 0) {
                continue;
            }
            average.put(entry.getKey(), sum / cnt);
            minimum.put(entry.getKey(), min);
        }

        List<Map.Entry<Integer, Long>> sortedAverages = new ArrayList<>(average.entrySet());
        Collections.sort(sortedAverages, (e1, e2) -> {
            return (int) Math.signum(e1.getValue() - e2.getValue());
        });

        List<Row> rows = new ArrayList<>();
        for (Map.Entry<Integer, Long> entry : sortedAverages) {
            int id = entry.getKey();
            String c = findName(id, contacts);
            rows.add(new Row(c, idToTimes.get(id), average.get(id), minimum.get(id)));
        }
        return rows;
    }

    private static String findName(int id, List<Contact> contacts) {
        for (Contact c : contacts) {
            if (c.getId() == id) {
                return c.getName();
            }
        }
        return String.valueOf(id);
    }

    public static final class Row {
        final String name;
        final List<Long> times;
        final long average;
        final long minimum;

        Row(String name, List<Long> times, long average, long minimum) {
            this.name = name;
            this.times = times;
            this.average = average;
            this.minimum = minimum;
        }
    }
}
