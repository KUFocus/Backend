package org.focus.logmeet.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.focus.logmeet.common.exception.BaseException;
import org.focus.logmeet.controller.dto.search.MinutesSearchHistoryResult;
import org.focus.logmeet.controller.dto.search.MinutesSearchRequest;
import org.focus.logmeet.controller.dto.search.MinutesSearchResult;
import org.focus.logmeet.controller.dto.search.SearchHistoryResult;
import org.focus.logmeet.domain.Minutes;
import org.focus.logmeet.domain.MinutesSearchHistory;
import org.focus.logmeet.domain.User;
import org.focus.logmeet.domain.elasticsearch.MinutesDocument;
import org.focus.logmeet.repository.MinutesRepository;
import org.focus.logmeet.repository.MinutesSearchHistoryRepository;
import org.focus.logmeet.repository.MinutesSearchRepository;
import org.focus.logmeet.security.annotation.CurrentUser;
import org.focus.logmeet.security.aspect.CurrentUserHolder;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.focus.logmeet.common.response.BaseExceptionResponseStatus.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinutesSearchService {
    private final MinutesSearchRepository minutesSearchRepository;
    private final MinutesSearchHistoryRepository minutesSearchHistoryRepository;
    private final MinutesRepository minutesRepository;


    @CurrentUser
    public List<MinutesSearchResult> search(String query) {
        User currentUser = CurrentUserHolder.get();
        if (currentUser == null) {
            throw new BaseException(USER_NOT_AUTHENTICATED);
        }

        String sanitizedQuery = sanitizeQuery(query);

        List<Long> accessibleMinutesIds = minutesRepository.findAllByUserProjects_UserId(currentUser.getId())
                .stream()
                .map(Minutes::getId)
                .toList();

        if (accessibleMinutesIds.isEmpty()) {
            log.info("유저 {}는 접근 가능한 회의록이 없습니다.", currentUser.getId());
            return List.of();
        }

        List<MinutesDocument> searchResults = minutesSearchRepository.searchByQuery(
                sanitizedQuery, accessibleMinutesIds
        );

        List<Long> documentIds = searchResults.stream().map(MinutesDocument::getId).toList();
        List<Minutes> minutesList = minutesRepository.findAllById(documentIds);

        return minutesList.stream()
                .map(minutes -> MinutesSearchResult.builder()
                        .id(minutes.getId())
                        .title(minutes.getName())
                        .projectName(minutes.getProject().getName())
                        .contentSnippet(extractSnippet(minutes.getClearContent(), sanitizedQuery))
                        .build())
                .toList();
    }

    @CurrentUser
    public void saveSearchHistory(Long minutesId) {
        User currentUser = CurrentUserHolder.get();
        if (currentUser == null) {
            throw new BaseException(USER_NOT_AUTHENTICATED);
        }

        boolean alreadyExists = minutesSearchHistoryRepository.existsByUserIdAndMinutesId(currentUser.getId(), minutesId);
        if (alreadyExists) {
            log.info("기존 검색 기록 갱신: userId={}, minutesId={}", currentUser.getId(), minutesId);
            minutesSearchHistoryRepository.deleteByUserIdAndMinutesId(currentUser.getId(), minutesId);
        }

        List<MinutesSearchHistory> userHistories = minutesSearchHistoryRepository.findByUserIdOrderByCreatedAtAsc(currentUser.getId());
        if (userHistories.size() >= 10) {
            MinutesSearchHistory oldestHistory = userHistories.get(0);
            minutesSearchHistoryRepository.delete(oldestHistory);
            log.info("가장 오래된 검색 기록 삭제: historyId={}", oldestHistory.getId());
        }

        Minutes minutes = minutesRepository.findById(minutesId)
                .orElseThrow(() -> new BaseException(MINUTES_NOT_FOUND));

        MinutesSearchHistory searchHistory = MinutesSearchHistory.builder()
                .user(currentUser)
                .minutes(minutes)
                .build();

        minutesSearchHistoryRepository.save(searchHistory);
        log.info("검색 기록 저장 완료: userId={}, minutesId={}", currentUser.getId(), minutesId);
    }

    @CurrentUser
    public List<MinutesSearchHistoryResult> getUserSearchHistory() {
        User currentUser = CurrentUserHolder.get();
        if (currentUser == null) {
            throw new BaseException(USER_NOT_AUTHENTICATED);
        }

        List<MinutesSearchHistory> histories = minutesSearchHistoryRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId());

        return histories.stream()
                .map(history -> MinutesSearchHistoryResult.builder()
                        .searchDate(history.getCreatedAt())
                        .minutesId(history.getMinutes().getId())
                        .title(history.getMinutes().getName())
                        .projectName(history.getMinutes().getProject().getName())
                        .build())
                .toList();
    }

    @CurrentUser
    public void deleteSearchHistory(Long historyId) {
        User currentUser = CurrentUserHolder.get();
        if (currentUser == null) {
            throw new BaseException(USER_NOT_AUTHENTICATED);
        }

        MinutesSearchHistory history = minutesSearchHistoryRepository.findById(historyId)
                .orElseThrow(() -> new BaseException(SEARCH_HISTORY_NOT_FOUND));

        if (!history.getUser().getId().equals(currentUser.getId())) {
            throw new BaseException(NOT_USER_HISTORY);
        }

        minutesSearchHistoryRepository.delete(history);
        log.info("검색 기록 삭제 완료: historyId={}, userId={}", historyId, currentUser.getId());
    }



    private String extractSnippet(String content, String query) {
        if (content == null || query == null) {
            return content;
        }
        int matchIndex = content.toLowerCase().indexOf(query.toLowerCase());
        if (matchIndex == -1) {
            return content.length() > 100 ? content.substring(0, 100) + "..." : content;
        }

        int start = Math.max(0, matchIndex - 30);
        int end = Math.min(content.length(), matchIndex + query.length() + 30);

        return (start > 0 ? "..." : "") + content.substring(start, end) + (end < content.length() ? "..." : "");
    }

    @PostConstruct
    public void indexAllMinutes() {
        List<Minutes> allMinutes = minutesRepository.findAll();
        List<MinutesDocument> documents = allMinutes.stream()
                .map(minutes -> MinutesDocument.builder()
                        .id(minutes.getId())
                        .title(minutes.getName())
                        .content(minutes.getClearContent())
                        .build())
                .toList();

        minutesSearchRepository.saveAll(documents);
        log.info("Elasticsearch 인덱싱 완료: 총 " + documents.size() + "개 문서");
    }

    private String sanitizeQuery(String query) {
        if (query == null) {
            return "";
        }
        // 줄 바꿈 및 기타 특수 문자를 공백으로 대체
        return query.replaceAll("[\\n\\r\\\\]", " ").trim();
    }
}
