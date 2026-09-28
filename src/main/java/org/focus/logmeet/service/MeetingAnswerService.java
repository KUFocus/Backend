package org.focus.logmeet.service;

import lombok.RequiredArgsConstructor;
import org.focus.logmeet.common.exception.BaseException;
import org.focus.logmeet.controller.dto.meeting.MeetingAnswer;
import org.focus.logmeet.domain.Minutes;
import org.focus.logmeet.domain.Project;
import org.focus.logmeet.domain.User;
import org.focus.logmeet.repository.*;
import org.focus.logmeet.security.annotation.CurrentUser;
import org.focus.logmeet.security.aspect.CurrentUserHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashSet;
import java.util.Set;
import static org.focus.logmeet.common.response.BaseExceptionResponseStatus.*;
import static org.focus.logmeet.domain.enums.Status.ACTIVE;

@Service
@RequiredArgsConstructor
public class MeetingAnswerService {
    private final ProjectRepository projects;
    private final UserProjectRepository memberships;
    private final MinutesRepository minutes;
    private final MeetingAnswerClient client;

    @CurrentUser
    @Transactional(readOnly = true)
    public MeetingAnswer answer(Long projectId, Long minutesId, String question) {
        User user = CurrentUserHolder.get();
        if (user == null) throw new BaseException(USER_NOT_AUTHENTICATED);
        if (projectId == null || projectId <= 0 || minutesId != null && minutesId <= 0
                || question == null || question.isBlank() || question.codePointCount(0, question.length()) > 2000) {
            throw new BaseException(INVALID_INPUT_VALUE, "프로젝트와 회의록 ID는 양수여야 하며 질문은 공백이 아닌 2000자 이하의 텍스트여야 합니다.");
        }
        Project project = projects.findById(projectId).orElseThrow(() -> new BaseException(PROJECT_NOT_FOUND));
        if (memberships.findByUserAndProject(user, project).isEmpty()) throw new BaseException(USER_NOT_IN_PROJECT);
        if (minutesId != null) requireMeeting(projectId, minutesId);
        MeetingAnswer result = client.answer(projectId, minutesId, question);
        Set<Long> checked = new HashSet<>();
        for (MeetingAnswer.Claim claim : result.claims()) {
            for (MeetingAnswer.Citation citation : claim.citations()) {
                if (checked.add(citation.minutesId())) requireMeeting(projectId, citation.minutesId());
            }
        }
        return result;
    }

    private void requireMeeting(Long projectId, Long minutesId) {
        Minutes meeting = minutes.findById(minutesId).orElseThrow(() -> new BaseException(MINUTES_NOT_FOUND));
        if (meeting.getStatus() != ACTIVE || meeting.getProject() == null
                || !projectId.equals(meeting.getProject().getId())) {
            throw new BaseException(MINUTES_NOT_FOUND);
        }
    }
}
