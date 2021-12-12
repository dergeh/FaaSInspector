package config;

public class ResultConfig {
    private boolean rawResults;
    private String metricGeneration;

    public boolean isRawResults() {
        return rawResults;
    }

    public void setRawResults(boolean rawResults) {
        this.rawResults = rawResults;
    }

    public String getMetricGeneration() {
        return metricGeneration;
    }

    public void setMetricGeneration(String metricGeneration) {
        this.metricGeneration = metricGeneration;
    }
}
