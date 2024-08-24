package org.focus.logmeet.service;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final AmazonS3 s3;

    @Value("${cloud.naver.credentials.accessKey}")
    private String accessKey;

    @Value("${cloud.naver.credentials.secretKey}")
    private String secretKey;

    public S3Service() {
        String endPoint = "https://kr.object.ncloudstorage.com";
        String regionName = "kr-standard";

        this.s3 = AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endPoint, regionName))
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)))
                .build();
    }

    public void uploadFile(String directory, String objectName, File file) {
        try {
            String bucketName = "logmeet";
            String fullObjectName = directory + "/" + objectName;
            s3.putObject(new PutObjectRequest(bucketName, fullObjectName, file));
            log.info("버킷에 객체 업로드 시도: bucketName={}, ObjectName={}", bucketName, fullObjectName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
