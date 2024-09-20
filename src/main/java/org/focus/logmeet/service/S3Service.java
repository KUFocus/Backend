package org.focus.logmeet.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.focus.logmeet.common.exception.BaseException;
import org.focus.logmeet.common.response.BaseExceptionResponseStatus;
import org.springframework.stereotype.Service;

import java.io.File;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final AmazonS3 s3;

    public void uploadFile(String directory, String objectName, File file) {
        try {
            String bucketName = "logmeet";
            String fullObjectName = directory + "/" + objectName;
            s3.putObject(new PutObjectRequest(bucketName, fullObjectName, file));
            log.info("버킷에 객체 업로드 성공: bucketName={}, ObjectName={}", bucketName, fullObjectName);
        } catch (Exception e) {
            log.error("S3에 파일 업로드 중 오류 발생: directory={}, objectName={}", directory, objectName, e);
            throw new BaseException(BaseExceptionResponseStatus.S3_FILE_UPLOAD_ERROR);
        }
    }
}
