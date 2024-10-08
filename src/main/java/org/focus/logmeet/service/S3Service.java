package org.focus.logmeet.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.focus.logmeet.common.exception.BaseException;
import org.focus.logmeet.common.response.BaseExceptionResponseStatus;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final AmazonS3 s3;

    public void uploadFile(String directory, String objectName, File file, String contentType) {
        try {
            String bucketName = "logmeet";
            String fullObjectName = directory + "/" + objectName;

            // 파일 메타데이터 생성 및 Content-Type 설정
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(contentType);
            metadata.setContentLength(file.length());

            // 파일을 S3에 업로드
            try (FileInputStream inputStream = new FileInputStream(file)) {
                PutObjectRequest request = new PutObjectRequest(bucketName, fullObjectName, inputStream, metadata)
                        .withCannedAcl(CannedAccessControlList.PublicRead);  // 파일을 전체 공개로 설정

                s3.putObject(request);
            }

            log.info("버킷에 객체 업로드 성공: bucketName={}, ObjectName={}", bucketName, fullObjectName);
        } catch (IOException e) {
            log.error("파일을 읽는 중 오류 발생: {}", e.getMessage());
            throw new BaseException(BaseExceptionResponseStatus.S3_FILE_UPLOAD_ERROR);
        } catch (Exception e) {
            log.error("S3에 파일 업로드 중 오류 발생: directory={}, objectName={}", directory, objectName, e);
            throw new BaseException(BaseExceptionResponseStatus.S3_FILE_UPLOAD_ERROR);
        }
    }
}
