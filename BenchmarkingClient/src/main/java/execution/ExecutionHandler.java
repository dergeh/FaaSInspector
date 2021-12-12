package execution;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.core.DockerClientBuilder;
import config.ExecutionConfig;
import data.DataHandler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class ExecutionHandler {
    public static final String result_path = "./results";
    private final static Logger log = Logger.getLogger(ExecutionHandler.class.getName());

    public void execute(ExecutionConfig config) {
        Map<String, String> env = config.getEnvironment();
        Map<String, String> awsConfig = config.getAwsConfig();
        String image = config.getImage();
        try {
            Files.createDirectories(Paths.get(result_path));
        } catch (IOException e) {
            e.printStackTrace();
        }
        String absoluteResultPath = new File(ExecutionHandler.result_path).getAbsolutePath();

        //check if prefix for env is present and delete entry if so
        String prefix = env.getOrDefault("prefix", null);
        if (prefix != null) env.remove("prefix");

        List<String> parsedEnv = new ArrayList<>();

        parseEnv(env, prefix, parsedEnv);
        parseEnv(awsConfig, null, parsedEnv);
        log.info("env parsed");
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();

        //build docker container from image

        CreateContainerCmd containerCmd = dockerClient.createContainerCmd(image)
                .withCmd("")//das muss noch definiert werden
                .withName("serverless-benchmark")
                .withEnv(parsedEnv);

        if (config.isOwnMetrics()) {
            containerCmd = containerCmd.withBinds(Bind.parse(absoluteResultPath + ":/corral-tpc-h/runs"));
        }


        CreateContainerResponse container = containerCmd.exec();

        //run container
        log.info("starting execution container");
        dockerClient.startContainerCmd(container.getId()).exec();

        //wait for container to finish running
        boolean running = true;
        while (running) {
            List<Container> containers = dockerClient.listContainersCmd().withShowAll(true).exec();
            for (Container c : containers) {

                if (c.getId().equals(container.getId()) && c.getState().equals("exited")) {
                    running = false;
                    log.info("execution finished");
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    private void parseEnv(Map<String, String> env, String prefix, List<String> parsedEnv) {
        //parse Map to Env variables
        for (String k : env.keySet()) {
            StringBuilder envBuilder = new StringBuilder();
            if (prefix != null) envBuilder.append(prefix);
            envBuilder.append(k.toUpperCase()).append("=").append(env.get(k));
            parsedEnv.add(envBuilder.toString());
        }
    }
}
