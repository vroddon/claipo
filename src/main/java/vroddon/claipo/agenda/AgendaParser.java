package vroddon.claipo.agenda;

import java.io.BufferedReader;
import java.io.FileReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 *
 * @author victor
 */
public class AgendaParser {

    private static final DateTimeFormatter ICS_DT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    private static final DateTimeFormatter ICS_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    public static void main(String[] args) {
        List<ICSEvent> events = parseCalendar();
        System.out.println(formatEvents(events));
    }

    public static List<ICSEvent> parseCalendar() {
        LocalDateTime from = LocalDate.now().atStartOfDay();
        LocalDateTime to = LocalDate.now().plusMonths(1).atStartOfDay();

        List<ICSEvent> events = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader("./data/victor.ics"))) {
            String line;
            LocalDateTime start = null;
            LocalDateTime end = null;
            String summary = null;
            boolean inEvent = false;

            while ((line = br.readLine()) != null) {
                if (line.equals("BEGIN:VEVENT")) {
                    inEvent = true;
                    start = null;
                    end = null;
                    summary = null;
                } else if (line.equals("END:VEVENT")) {
                    if (inEvent && start != null && end != null && summary != null
                            && !start.isBefore(from) && start.isBefore(to)) {
                        events.add(new ICSEvent(start, end, summary));
                    }
                    inEvent = false;
                } else if (inEvent) {
                    if (line.startsWith("DTSTART")) {
                        start = parseDt(line.substring(line.indexOf(':') + 1));
                    } else if (line.startsWith("DTEND")) {
                        end = parseDt(line.substring(line.indexOf(':') + 1));
                    } else if (line.startsWith("SUMMARY:")) {
                        summary = line.substring(8);
                    }
                }
            }
        } catch (Exception e) {
            return new ArrayList<>();
        }

        events.sort((a, b) -> a.start.compareTo(b.start));
        return events;
    }

    private static LocalDateTime parseDt(String value) {
        value = value.trim();
        if (value.length() == 8) {
            return LocalDate.parse(value, ICS_DATE).atStartOfDay();
        }
        if (value.endsWith("Z")) {
            value = value.substring(0, value.length() - 1);
        }
        return LocalDateTime.parse(value, ICS_DT);
    }

    public static String formatEvents(List<ICSEvent> events) {
        Locale locale = new Locale("es", "ES");
        StringBuilder sb = new StringBuilder();

        for (ICSEvent e : events) {
            String dayName = e.start.getDayOfWeek().getDisplayName(TextStyle.FULL, locale);
            dayName = Character.toUpperCase(dayName.charAt(0)) + dayName.substring(1);
            String monthName = e.start.getMonth().getDisplayName(TextStyle.FULL, locale);
            int day = e.start.getDayOfMonth();
            String startTime = e.start.format(DateTimeFormatter.ofPattern("HH:mm"));
            String endTime = e.end.format(DateTimeFormatter.ofPattern("HH:mm"));

            sb.append(String.format("%s %d %s %s–%s %s%n",
                    dayName, day, monthName, startTime, endTime, e.summary));
        }

        return sb.toString();
    }
    

}
