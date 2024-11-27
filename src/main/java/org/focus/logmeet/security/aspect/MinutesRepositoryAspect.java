package org.focus.logmeet.security.aspect;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.focus.logmeet.domain.Minutes;
import org.focus.logmeet.domain.elasticsearch.MinutesDocument;
import org.focus.logmeet.repository.MinutesSearchRepository;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class MinutesRepositoryAspect {
    private final MinutesSearchRepository minutesSearchRepository;

    @AfterReturning(value = "execution(* org.focus.logmeet.repository.MinutesRepository.save(..))", returning = "minutes")
    public void afterSave(Minutes minutes) {
        log.info("Elasticsearch에 인덱싱 시작: {}", minutes.getId());
        MinutesDocument document = MinutesDocument.builder()
                .id(minutes.getId())
                .title(minutes.getName())
                .content(minutes.getClearContent())
                .build();
        minutesSearchRepository.save(document);
        log.info("Elasticsearch에 인덱싱 완료: {}", minutes.getId());
    }

    @AfterReturning("execution(* org.focus.logmeet.repository.MinutesRepository.delete(..)) && args(minutes)")
    public void afterDelete(Minutes minutes) {
        log.info("Elasticsearch에서 삭제 시작: {}", minutes.getId());
        minutesSearchRepository.deleteById(minutes.getId());
        log.info("Elasticsearch에서 삭제 완료: {}", minutes.getId());
    }
}
