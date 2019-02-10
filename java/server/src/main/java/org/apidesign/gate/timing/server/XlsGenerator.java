package org.apidesign.gate.timing.server;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apidesign.gate.timing.shared.Contact;
import org.apidesign.gate.timing.shared.Run;
import org.apidesign.gate.timing.shared.Running;
import org.apidesign.gate.timing.shared.Time;

public final class XlsGenerator {
    private final Running stats;
    private final List<Row> rows;

    private XlsGenerator(Running running, List<Row> names) {
        this.stats = running;
        this.rows = names;
    }

    public List<Row> getRows() {
        return Collections.unmodifiableList(rows);
    }

    public static XlsGenerator create(Running stats, List<Contact> contacts) {
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
        return new XlsGenerator(stats, rows);
    }

    private static String findName(int id, List<Contact> contacts) {
        for (Contact c : contacts) {
            if (c.getId() == id) {
                return c.getName();
            }
        }
        return String.valueOf(id);
    }

    public void write(OutputStream os) throws IOException {
        try (Workbook doc = new XSSFWorkbook()) {
            short timeFormat = doc.createDataFormat().getFormat("0.00");
            CellStyle timeStyle = doc.createCellStyle();
            timeStyle.setDataFormat(timeFormat);

            CellStyle dnfStyle = doc.createCellStyle();
            dnfStyle.setAlignment(HorizontalAlignment.RIGHT);

            String docName = stats.getSettings().getName();
            if (docName == null) {
                docName = "Výsledky";
            }

            Sheet sheet = doc.createSheet(docName);

            {
                org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
                Cell name = headerRow.createCell(0);
                name.setCellValue("Jméno");
                Cell min = headerRow.createCell(1);
                min.setCellValue("Nejlepší");
                Cell avg = headerRow.createCell(2);
                avg.setCellValue("Průměr");
            }

            for (int i = 0; i < rows.size(); i++) {
                Row row = rows.get(i);

                org.apache.poi.ss.usermodel.Row resultRow = sheet.createRow(1 + i);

                Cell name = resultRow.createCell(0);
                name.setCellValue(row.getName());

                Cell min = resultRow.createCell(1);
                min.setCellValue(Time.toDouble(row.getMin()));
                min.setCellStyle(timeStyle);

                Cell avg = resultRow.createCell(2);
                avg.setCellValue(Time.toDouble(row.getAvg()));
                avg.setCellStyle(timeStyle);

                for (int j = 0; j < row.getTimes().size(); j++) {
                    Long time = row.getTimes().get(j);
                    if (time == null) {
                        continue;
                    }
                    Cell cell = resultRow.createCell(3 + j);
                    if (time == 0) {
                        cell.setCellValue("DNF");
                        cell.setCellStyle(dnfStyle);
                    } else {
                        cell.setCellValue(Time.toDouble(time));
                        cell.setCellStyle(timeStyle);
                    }
                }
            }
            doc.write(os);
        }
    }

    public static final class Row {
        private final String name;
        private final List<Long> times;
        private final long average;
        private final long minimum;

        Row(String name, List<Long> times, long average, long minimum) {
            this.name = name;
            this.times = times;
            this.average = average;
            this.minimum = minimum;
        }

        public String getName() {
            return name;
        }

        public List<Long> getTimes() {
            return Collections.unmodifiableList(times);
        }

        public long getAvg() {
            return average;
        }

        public long getMin() {
            return minimum;
        }
    }

    @Override
    public String toString() {
        StringWriter w = new StringWriter();
        PrintWriter pw = new PrintWriter(w);
        pw.println("Jméno               Nejlepší\tPrůměr\tČas v kolech");
        for (XlsGenerator.Row r : rows) {
            String name20 = (r.getName() + "                   ").substring(0, 20);
            pw.print(name20 + Time.toString(r.getMin()) + "\t" + Time.toString(r.getAvg()));
            for (Long time : r.getTimes()) {
                pw.print("\t");
                pw.print(Time.toString(time));
            }
            pw.println();
        }
        return w.toString();
    }
}
