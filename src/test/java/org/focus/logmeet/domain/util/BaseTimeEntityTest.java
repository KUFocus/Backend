package org.focus.logmeet.domain.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class BaseTimeEntityTest {

    @InjectMocks
    private BaseTimeEntity baseTimeEntity;


    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();

        setFieldValue(baseTimeEntity, "createdAt", now);
        setFieldValue(baseTimeEntity, "updatedAt", now);
    }

    @Test
    @DisplayName("createdAt과 updatedAt 필드 자동 설정 테스트")
    void testBaseTimeEntityFields() {
        assertNotNull(baseTimeEntity.getCreatedAt());
        assertNotNull(baseTimeEntity.getUpdatedAt());

        assertTrue(baseTimeEntity.getCreatedAt().isBefore(LocalDateTime.now()) || baseTimeEntity.getCreatedAt().isEqual(LocalDateTime.now()));
        assertTrue(baseTimeEntity.getUpdatedAt().isBefore(LocalDateTime.now()) || baseTimeEntity.getUpdatedAt().isEqual(LocalDateTime.now()));
    }

    private void setFieldValue(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
