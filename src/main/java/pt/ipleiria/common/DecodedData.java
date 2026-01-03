package pt.ipleiria.common;

import java.util.ArrayList;
import java.util.List;

// DTO para o conjunto de dados
public class DecodedData {
    public long totalTimeMs;
    public List<KeystrokeEvent> events;

    public DecodedData() {
        this.totalTimeMs = 0;
        this.events = new ArrayList<>();
    }

    public DecodedData(long totalTimeMs, List<KeystrokeEvent> events) {
        this.totalTimeMs = totalTimeMs;
        this.events = events;
    }
}
