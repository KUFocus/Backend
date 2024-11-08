package org.focus.logmeet.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

    @InjectMocks
    private S3Service s3Service;

    @Mock
    private AmazonS3 s3;

    @Test
    @DisplayName("Pre-signed URL 생성 성공 테스트")
    void generatePreSignedUrl_Success() {
        // given
        String directory = "minutes_voice";
        String fileName = "sample.mp3";
        String contentType = "audio/mpeg";
        String expectedUrl = "https://mock-s3-url.com/presigned-url";

        // 모의 URL 객체 생성
        URL mockUrl = mock(URL.class);
        when(mockUrl.toString()).thenReturn(expectedUrl);

        // 모킹 설정
        when(s3.generatePresignedUrl(any(GeneratePresignedUrlRequest.class))).thenReturn(mockUrl);

        // when
        String resultUrl = s3Service.generatePreSignedUrl(directory, fileName, contentType);

        // then
        assertNotNull(resultUrl);
        assertEquals(expectedUrl, resultUrl);
        verify(s3, times(1)).generatePresignedUrl(any(GeneratePresignedUrlRequest.class));
    }

    @Test
    @DisplayName("Pre-signed URL 생성 시 예외 발생 테스트")
    void generatePreSignedUrl_ThrowsException() {
        // given
        String directory = "minutes_voice";
        String fileName = "sample.mp3";
        String contentType = "audio/mpeg";

        // S3 클라이언트에서 예외 발생 모킹
        when(s3.generatePresignedUrl(any(GeneratePresignedUrlRequest.class)))
                .thenThrow(new RuntimeException("AWS S3 Error"));

        // when & then
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                s3Service.generatePreSignedUrl(directory, fileName, contentType));
        assertEquals("AWS S3 Error", exception.getMessage());
    }
}
