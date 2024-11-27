package org.focus.logmeet.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.focus.logmeet.common.response.BaseResponse;
import org.focus.logmeet.controller.dto.search.MinutesSearchHistoryResult;
import org.focus.logmeet.controller.dto.search.MinutesSearchRequest;
import org.focus.logmeet.controller.dto.search.MinutesSearchResult;
import org.focus.logmeet.service.MinutesSearchService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.focus.logmeet.common.response.BaseExceptionResponseStatus.SUCCESS;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/search")
public class MinutesSearchController {
    private final MinutesSearchService minutesSearchService;

    @Operation(summary = "회의록 검색", description = "검색어로 회의록을 검색합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "검색 결과 반환")
    })
    @PostMapping
    public BaseResponse<List<MinutesSearchResult>> searchMinutes(
            @RequestBody MinutesSearchRequest request) {
        log.info("회의록 검색 요청: query={}", request.getQuery());
        List<MinutesSearchResult> results = minutesSearchService.search(request.getQuery());
        return new BaseResponse<>(results);
    }

    @Operation(summary = "검색 기록 조회", description = "유저의 검색 기록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "검색 기록 반환")
    })
    @GetMapping("/history")
    public BaseResponse<List<MinutesSearchHistoryResult>> getUserSearchHistory() {
        List<MinutesSearchHistoryResult> history = minutesSearchService.getUserSearchHistory();
        return new BaseResponse<>(history);
    }

    @Operation(summary = "검색 기록 저장", description = "검색 결과 클릭 시 기록을 저장합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "검색 기록 저장 성공")
    })
    @PostMapping("/{minutesId}/history")
    public BaseResponse<Void> saveSearchHistory(
            @PathVariable Long minutesId) {
        log.info("검색 기록 저장 요청: minutesId={}", minutesId);
        minutesSearchService.saveSearchHistory(minutesId);
        return new BaseResponse<>(SUCCESS);
    }
}
