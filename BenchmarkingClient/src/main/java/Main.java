
import com.fasterxml.jackson.databind.ObjectMapper;
import config.BenchmarkConfig;
import data.DataHandler;
import execution.ExecutionHandler;
import results.ResultHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Logger;

public class Main {
    private final static Logger log = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {

        DataHandler dataHandler = new DataHandler();
        ExecutionHandler executionHandler = new ExecutionHandler();
        ResultHandler resultHandler = new ResultHandler();

        long start = System.currentTimeMillis();

        BenchmarkConfig config = parseConfig(new File(args[0]));

        //check if data stage is defined
        if (config.getData() != null) {
            dataHandler.generate(config.getData().getScalingFactor(), config.getData().getImage());
            log.info("starting upload");
            try {
                dataHandler.upload(config.getData().getBucket());
            } catch (IOException e) {
                e.printStackTrace();
            }
            log.info("upload finished");
            //dataHandler.split(config.getData().getSplitSize());
        }

        //check if execution stage is defined
        if (config.getExecution()!=null){
           log.info("starting execution handler");
            executionHandler.execute(config.getExecution());
        }

        long finish = System.currentTimeMillis();

        if(config.getResult()!= null){
            resultHandler.getResults(start,finish,config.getResult(), config.getExecution().getInput());
        }


    }

    public static BenchmarkConfig parseConfig(File config) {
        ObjectMapper mapper = new ObjectMapper();
        BenchmarkConfig parsedConfig = null;
        try {
            parsedConfig = mapper.readValue(config, BenchmarkConfig.class);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return parsedConfig;

    }

}
