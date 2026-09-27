package org.focus.logmeet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.focus.logmeet.common.exception.BaseException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class MeetingIndexListener {
    private final MeetingIndexClient client;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void index(MeetingIndexRequested event) {
        try {
            client.index(event.projectId(), event.minutesId(), event.text());
        } catch (BaseException e) {
            // 원문 저장은 이미 완료됐으므로 색인 실패를 저장 실패로 반환하지 않는다.
            log.error("회의록은 저장됐지만 색인에 실패했습니다. 프로젝트 ID={}, 회의록 ID={}",
                    event.projectId(), event.minutesId(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void delete(MeetingIndexDeleteRequested event) {
        try {
            client.delete(event.projectId(), event.minutesId());
        } catch (BaseException e) {
            log.error("회의록은 삭제됐지만 색인 삭제에 실패했습니다. 프로젝트 ID={}, 회의록 ID={}",
                    event.projectId(), event.minutesId(), e);
        }
    }
}
