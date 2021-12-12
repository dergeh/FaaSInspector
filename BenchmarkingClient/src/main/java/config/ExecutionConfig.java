package config;

import java.util.Map;

public class ExecutionConfig {
    private String image;
    private String input;
    private boolean ownMetrics;
    private Map<String, String> environment;
    private Map<String,String> awsConfig;

    public Map<String, String> getAwsConfig() {
        return awsConfig;
    }

    public void setAwsConfig(Map<String, String> awsConfig) {
        this.awsConfig = awsConfig;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public Map<String, String> getEnvironment() {
        return environment;
    }

    public void setEnvironment(Map<String, String> environment) {
        this.environment = environment;
    }

    public boolean isOwnMetrics() {
        return ownMetrics;
    }

    public void setOwnMetrics(boolean ownMetrics) {
        this.ownMetrics = ownMetrics;
    }
}
