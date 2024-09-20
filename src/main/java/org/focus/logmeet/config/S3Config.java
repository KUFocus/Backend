package org.focus.logmeet.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class S3Config {

    @Value("${cloud.naver.credentials.accessKey}")
    private String accessKey;

    @Value("${cloud.naver.credentials.secretKey}")
    private String secretKey;

    @Bean
    public AmazonS3 amazonS3() {
        String endPoint = "https://kr.object.ncloudstorage.com";
        String regionName = "kr-standard";

        try {
            return AmazonS3ClientBuilder.standard()
                    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endPoint, regionName))
                    .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)))
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("S3 클라이언트 생성 중 오류가 발생했습니다.", e);
        }
    }
}
