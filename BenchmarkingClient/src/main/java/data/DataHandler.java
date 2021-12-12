package data;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.core.DockerClientBuilder;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DataHandler {
    static final String data_path = "./resources";
    private final static Logger log = Logger.getLogger(DataHandler.class.getName());

    public void generate(int size, String image) {
        try {
            Files.createDirectories(Paths.get(data_path));
        } catch (IOException e) {
            e.printStackTrace();
        }
        String absoluteDataPath = new File(DataHandler.data_path).getAbsolutePath();
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();
        CreateContainerResponse container
                = dockerClient.createContainerCmd(image)
                .withCmd("-s " + size)
                .withName("dbgen")
                .withBinds(Bind.parse(absoluteDataPath + ":/data")).exec(); // local link muss noch vom system geholt werden
        log.info("starting data generation container");
        dockerClient.startContainerCmd(container.getId()).exec();
        boolean running = true;
        while (running) {
            List<Container> containers = dockerClient.listContainersCmd().withShowAll(true).exec();
            for (Container c : containers) {

                if (c.getId().equals(container.getId()) && c.getState().equals("exited")) {
                    running = false;
                    log.info("generation finished");
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }


    }

    public void upload(String bucket) throws IOException {

        Region region = Region.EU_CENTRAL_1;
        S3Client s3 = S3Client.builder()
                .region(region)
                .build();
        List<Path> files = null;
        long limit = 50000000;
        // find all files to upload
        files = Files.walk(Paths.get(DataHandler.data_path))
                .filter(Files::isRegularFile)
                .collect(Collectors.toList());

        //upload the files to the S3 bucket using the same folder structure as in generation
        for (Path p : files) {
            File f = new File(p.toUri());
            log.info("starting upload of " + p);
            String folder = f.getParent().split("/")[f.getParent().split("/").length - 1];
            Stream<String> lines = Files.lines(p);
            MultipartUploadHelper multipartUploadHelper = new MultipartUploadHelper(s3, bucket, "input/" + folder + "/" + f.getName());
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

    public void split(int size) {
        final String splitDir = "./splits";
        try {
            Files.createDirectories(Paths.get(splitDir));
            List<Path> files = Files.walk(Paths.get(DataHandler.data_path))
                    .filter(Files::isRegularFile)
                    .collect(Collectors.toList());

            for (Path p : files) splitFile(new File(p.toUri()), size, splitDir);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static List<File> splitFile(File file, int sizeOfFileInMB, String outDir) throws IOException {
        int counter = 1;
        List<File> files = new ArrayList<File>();
        int sizeOfChunk = 1024 * 1024 * sizeOfFileInMB;
        String eof = System.lineSeparator();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String name = file.getName();
            String line = br.readLine();
            while (line != null) {
                File newFile = new File(outDir + "/" + file.getParent(), name + "."
                        + String.format("%02d", counter++));
                try (OutputStream out = new BufferedOutputStream(new FileOutputStream(newFile))) {
                    int fileSize = 0;
                    while (line != null) {
                        byte[] bytes = (line + eof).getBytes(Charset.defaultCharset());
                        if (fileSize + bytes.length > sizeOfChunk)
                            break;
                        out.write(bytes);
                        fileSize += bytes.length;
                        line = br.readLine();
                    }
                }
                files.add(newFile);
            }
        }
        return files;
    }

}
