package org.focus.logmeet.service;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final AmazonS3 s3;

    public String generatePreSignedUrl(String directory, String fileName, String contentType) {
        String fullObjectName = directory + "/" + fileName;

        Date expiration = new Date(System.currentTimeMillis() + 3600000);

        String bucketName = "logmeet";
        GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucketName, fullObjectName)
                .withMethod(HttpMethod.PUT)
                .withExpiration(expiration)
                .withContentType(contentType);

        generatePresignedUrlRequest.addRequestParameter("x-amz-acl", CannedAccessControlList.PublicRead.toString());

        URL presignedUrl = s3.generatePresignedUrl(generatePresignedUrlRequest);
        return presignedUrl.toString();
    }
}

