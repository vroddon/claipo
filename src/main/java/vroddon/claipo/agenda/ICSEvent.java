package vroddon.claipo.agenda;

import java.time.LocalDateTime;

public class ICSEvent {
    public LocalDateTime start;
    public LocalDateTime end;
    public String summary;

    public ICSEvent(LocalDateTime start, LocalDateTime end, String summary) {
        this.start = start;
        this.end = end;
        this.summary = summary;
    }
}
