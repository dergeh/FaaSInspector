package results;

import java.util.List;
import java.util.Map;

public class ResultLogStream {
   private Map<String,List<EventMessage>> events;

    public ResultLogStream(Map<String, List<EventMessage>> events) {
        this.events = events;
    }

    public Map<String, List<EventMessage>> getEvents() {
        return events;
    }

    public void setEvents(Map<String, List<EventMessage>> events) {
        this.events = events;
    }
}
