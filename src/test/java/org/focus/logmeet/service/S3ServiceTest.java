package org.focus.logmeet.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.focus.logmeet.common.exception.BaseException;
import org.focus.logmeet.common.response.BaseExceptionResponseStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

    @Mock
    private AmazonS3 s3;

    @InjectMocks
    private S3Service s3Service;

    private File mockFile;

    @BeforeEach
    void setUp() {
        mockFile = mock(File.class);
    }

    @Test
    @DisplayName("파일 업로드 성공")
    void uploadFile_Success() throws IOException {
        // given
        String directory = "minutes_voice";
        String objectName = "test.mp3";
        String contentType = "audio/mpeg";
        when(mockFile.length()).thenReturn(100L);
        FileInputStream mockInputStream = mock(FileInputStream.class);

        S3Service spyS3Service = spy(s3Service);
        doReturn(mockInputStream).when(spyS3Service).createFileInputStream(mockFile);

        // when
        spyS3Service.uploadFile(directory, objectName, mockFile, contentType);

        // then
        verify(s3).putObject(any(PutObjectRequest.class));
    }

    @Test
    @DisplayName("파일을 읽는 중 IOException 발생 시 예외 발생")
    void uploadFile_FileReadError_ThrowsException() throws IOException {
        // given
        String directory = "minutes_voice";
        String objectName = "test.mp3";
        String contentType = "audio/mpeg";

        S3Service spyS3Service = spy(s3Service);
        doThrow(new IOException("File read error")).when(spyS3Service).createFileInputStream(mockFile);

        // when & then
        BaseException exception = assertThrows(BaseException.class,
                () -> spyS3Service.uploadFile(directory, objectName, mockFile, contentType));
        assertEquals(BaseExceptionResponseStatus.S3_FILE_UPLOAD_ERROR, exception.getStatus());
    }

    @Test
    @DisplayName("S3에 파일 업로드 중 예외 발생")
    void uploadFile_S3Error_ThrowsException() throws IOException {
        // given
        String directory = "minutes_voice";
        String objectName = "test.mp3";
        String contentType = "audio/mpeg";
        when(mockFile.length()).thenReturn(100L);
        FileInputStream mockInputStream = mock(FileInputStream.class);

        S3Service spyS3Service = spy(s3Service);
        doReturn(mockInputStream).when(spyS3Service).createFileInputStream(mockFile);

        // S3 업로드 중 예외 발생하도록 모킹
        doThrow(new RuntimeException("S3 upload error")).when(s3).putObject(any(PutObjectRequest.class));

        // when & then
        BaseException exception = assertThrows(BaseException.class,
                () -> spyS3Service.uploadFile(directory, objectName, mockFile, contentType));
        assertEquals(BaseExceptionResponseStatus.S3_FILE_UPLOAD_ERROR, exception.getStatus());
    }

    @Test
    @DisplayName("파일 스트림 생성 성공")
    void createFileInputStream_Success() throws IOException {
        // Create a temporary file for testing
        File tempFile = Files.createTempFile("test", ".txt").toFile();
        tempFile.deleteOnExit();

        // when
        FileInputStream result = s3Service.createFileInputStream(tempFile);

        // then
        assertEquals(FileInputStream.class, result.getClass());
        result.close();
    }

    @Test
    @DisplayName("파일 스트림 생성 시 IOException 발생 시 예외 발생")
    void createFileInputStream_ThrowsIOException() {
        // given
        File nonExistentFile = new File("non-existent-file.txt");

        // when & then
        IOException exception = assertThrows(IOException.class, () -> s3Service.createFileInputStream(nonExistentFile));
        assertEquals("non-existent-file.txt (No such file or directory)", exception.getMessage());
    }
}
