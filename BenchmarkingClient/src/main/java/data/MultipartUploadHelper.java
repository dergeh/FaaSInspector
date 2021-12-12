package data;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static java.util.Objects.isNull;

public class MultipartUploadHelper {
    private final static Logger log = Logger.getLogger(MultipartUploadHelper.class.getName());
    private final S3Client s3Client;
    private final String bucket;
    private final String destinationKey;
    private String abortRuleId;
    private String uploadId;
    private final List<CompletedPart> parts = new ArrayList<>();

    public MultipartUploadHelper(S3Client s3Client, String bucket, String destinationKey) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.destinationKey = destinationKey;
    }

    public MultipartUploadHelper start() {
        isValidStart();
        CreateMultipartUploadResponse multipartUpload = s3Client
                .createMultipartUpload(CreateMultipartUploadRequest.builder()
                        .bucket(bucket)
                        .key(destinationKey)
                        .build());
        abortRuleId = multipartUpload.abortRuleId();
        uploadId = multipartUpload.uploadId();
        return this;
    }

    private Boolean isValidStart() {
        if (isNull(abortRuleId) && isNull(uploadId) && parts.isEmpty()) {
            return true;
        }
        throw new RuntimeException("Invalid Multipart Upload Start");
    }

    public void partUpload(long limit, ByteArrayOutputStream byteArrayOutputStream) {
        if (byteArrayOutputStream.size() > limit) {
            partUpload(byteArrayOutputStream);
        }
    }

    private void partUpload(ByteArrayOutputStream byteArrayOutputStream) {
        Integer partNumber = parts.size() + 1;
        UploadPartResponse uploadPartResponse = s3Client.uploadPart(UploadPartRequest.builder()
                .bucket(bucket)
                .key(destinationKey)
                .uploadId(uploadId)
                .partNumber(partNumber)
                .build(), RequestBody.fromBytes(byteArrayOutputStream.toByteArray()));
        parts.add(CompletedPart.builder()
                .partNumber(partNumber)
                .eTag(uploadPartResponse.eTag())
                .build());
        byteArrayOutputStream.reset();
    }

    public void complete(ByteArrayOutputStream byteArrayOutputStream) {
        partUpload(byteArrayOutputStream);
        s3Client.completeMultipartUpload(CompleteMultipartUploadRequest.builder()
                .uploadId(uploadId)
                .bucket(bucket)
                .key(destinationKey)
                .multipartUpload(CompletedMultipartUpload.builder()
                        .parts(parts).build())
                .build());
        log.info("Multipart Upload complete with " + parts.size() + " parts");
    }
}