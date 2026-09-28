package org.focus.logmeet.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.focus.logmeet.common.response.BaseResponse;
import org.focus.logmeet.controller.dto.meeting.MeetingAnswer;
import org.focus.logmeet.service.MeetingAnswerService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/projects")
public class MeetingAnswerController {
    private final MeetingAnswerService service;

    @Operation(summary = "회의록에 질문", description = "프로젝트 회의록의 근거를 바탕으로 답변합니다. minutesId를 지정하면 해당 회의록에서만 검색합니다.")
    @PostMapping("/{projectId}/questions")
    public BaseResponse<MeetingAnswer> answer(@PathVariable Long projectId, @Valid @RequestBody Question request) {
        return new BaseResponse<>(service.answer(projectId, request.minutesId(), request.question()));
    }

    public record Question(@NotBlank(message = "질문은 공백일 수 없습니다.") String question,
                           @Positive(message = "회의록 ID는 양수여야 합니다.") Long minutesId) {}
}
