package results;

import config.ResultConfig;
import data.DataHandler;
import data.MultipartUploadHelper;
import execution.ExecutionHandler;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.FilterLogEventsRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.FilteredLogEvent;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ResultHandler {
    private final static Logger log = Logger.getLogger(ResultHandler.class.getName());


    public void getResults(long start, long finish, ResultConfig config, String experiment) {
        if (config.getMetricGeneration().equals("cloudwatch")) {
            ResultLogStream cloudWatchResults = getCloudWatch(start, finish);
            writeCsv(cloudWatchResults, experiment);
        }
        try {
            uploadToS3(experiment);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void uploadToS3(String bucket) throws IOException {

        Region region = Region.EU_CENTRAL_1;
        S3Client s3 = S3Client.builder()
                .region(region)
                .build();
        List<Path> files = null;
        long limit = 50000000;
        // find all files to upload
        files = Files.walk(Paths.get(ExecutionHandler.result_path))
                .filter(Files::isRegularFile)
                .collect(Collectors.toList());

        //upload the files to the S3 bucket using the same folder structure as in generation
        for (Path p : files) {
            File f = new File(p.toUri());
            log.info("starting upload of " + p);
            Stream<String> lines = Files.lines(p);
            MultipartUploadHelper multipartUploadHelper = new MultipartUploadHelper(s3, bucket, "results/" + f.getName());
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            multipartUploadHelper.start();
            lines.forEach(sampleData -> {
                try {
                    stream.write(sampleData.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                multipartUploadHelper.partUpload(limit, stream);
            });
            multipartUploadHelper.complete(stream);
        }
    }

    private void writeCsv(ResultLogStream cloudWatchResults, String experiment) {
        try {
            Files.createDirectories(Paths.get(ExecutionHandler.result_path));
        } catch (IOException e) {
            e.printStackTrace();
        }
        Map<String, List<EventMessage>> eventMap = cloudWatchResults.getEvents();
        Set<String> eventKeys = eventMap.keySet();
        List<String[]> dataLines = new ArrayList<>();
        for (String key : eventKeys) {
            List<EventMessage> messages = eventMap.get(key);
            for (EventMessage m : messages) dataLines.add(new String[]{key, m.toString()});
        }
        File csvOutputFile = new File(ExecutionHandler.result_path + "/" + experiment);
        try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
            dataLines.stream()
                    .map(this::convertToCSV)
                    .forEach(pw::println);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    public String convertToCSV(String[] data) {
        return Stream.of(data)
                .map(this::escapeSpecialCharacters)
                .collect(Collectors.joining(","));
    }

    public String escapeSpecialCharacters(String data) {
        String escapedData = data.replaceAll("\\R", " ");
        if (data.contains(",") || data.contains("\"") || data.contains("'")) {
            data = data.replace("\"", "\"\"");
            escapedData = "\"" + data + "\"";
        }
        return escapedData;
    }

    private ResultLogStream getCloudWatch(long start, long finish) {
        Region region = Region.EU_CENTRAL_1;
        CloudWatchLogsClient cloudWatchLogsClient = CloudWatchLogsClient.builder()
                .region(region)
                .build();
        FilterLogEventsRequest filterLogEventsRequest = FilterLogEventsRequest.builder()
                .logGroupName("/aws/lambda/corral_function")//need generification
                .startTime(start)
                .endTime(finish)
                .build();
        int logLimit = cloudWatchLogsClient.filterLogEvents(filterLogEventsRequest).events().size();
        ResultLogStream resultLogStream = new ResultLogStream(new HashMap<>());
        for (int c = 0; c < logLimit; c++) {
            FilteredLogEvent event = cloudWatchLogsClient.filterLogEvents(filterLogEventsRequest).events().get(c);
            if (event.message().startsWith("REPORT")) {
                List<EventMessage> eventMessages = resultLogStream.getEvents().getOrDefault(event.logStreamName(), new ArrayList<>());
                String[] s = event.message().split(" ");
                eventMessages.add(new EventMessage(s[2], Integer.parseInt(s[6]), Integer.parseInt(s[9]), Integer.parseInt(s[13])));
                resultLogStream.getEvents().putIfAbsent(event.logStreamName(), eventMessages);
            }
            ;
        }
        return resultLogStream;

    }

    private void getLocal() {

    }
}

