package org.focus.logmeet.service;

import lombok.RequiredArgsConstructor;
import org.focus.logmeet.common.exception.BaseException;
import org.focus.logmeet.domain.Minutes;
import org.focus.logmeet.domain.User;
import org.focus.logmeet.repository.MinutesRepository;
import org.focus.logmeet.repository.UserProjectRepository;
import org.focus.logmeet.security.annotation.CurrentUser;
import org.focus.logmeet.security.aspect.CurrentUserHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.focus.logmeet.common.response.BaseExceptionResponseStatus.*;
import static org.focus.logmeet.domain.enums.MinutesType.*;
import static org.focus.logmeet.domain.enums.Status.ACTIVE;

@Service
@RequiredArgsConstructor
public class MeetingIndexService {
    private final MinutesRepository minutesRepository;
    private final UserProjectRepository userProjectRepository;
    private final MeetingIndexClient client;

    @CurrentUser
    @Transactional(readOnly = true)
    public MeetingIndexClient.IndexResult index(Long minutesId) {
        User user = CurrentUserHolder.get();
        if (user == null) {
            throw new BaseException(USER_NOT_AUTHENTICATED);
        }
        Minutes minutes = minutesRepository.findById(minutesId)
                .orElseThrow(() -> new BaseException(MINUTES_NOT_FOUND));
        if (minutes.getStatus() != ACTIVE || minutes.getProject() == null) {
            throw new BaseException(INVALID_INPUT_VALUE, "프로젝트에 확정 저장된 회의록만 색인할 수 있습니다.");
        }
        if (userProjectRepository.findByUserAndProject(user, minutes.getProject()).isEmpty()) {
            throw new BaseException(USER_NOT_IN_PROJECT);
        }
        String text;
        if (minutes.getType() == MANUAL) {
            text = minutes.getContent();
        } else if (minutes.getType() == VOICE || minutes.getType() == PICTURE) {
            text = minutes.getClearContent();
        } else {
            throw new BaseException(MINUTES_TYPE_NOT_FOUND);
        }
        if (text == null || text.isBlank() || text.codePointCount(0, text.length()) > 100000) {
            throw new BaseException(INVALID_INPUT_VALUE, "색인할 회의 원문은 공백이 아닌 100000자 이하의 텍스트여야 합니다.");
        }
        return client.index(minutes.getProject().getId(), minutes.getId(), text);
    }
}
