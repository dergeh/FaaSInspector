package config;

public class BenchmarkConfig {
   private DataConfig data;
   private ExecutionConfig execution;
   private ResultConfig result;

    public DataConfig getData() {
        return data;
    }

    public void setData(DataConfig data) {
        this.data = data;
    }

    public ExecutionConfig getExecution() {
        return execution;
    }

    public void setExecution(ExecutionConfig execution) {
        this.execution = execution;
    }

    public ResultConfig getResult() {
        return result;
    }

    public void setResult(ResultConfig result) {
        this.result = result;
    }
}
