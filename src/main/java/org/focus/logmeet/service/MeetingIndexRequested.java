package org.focus.logmeet.service;

// 엔티티 대신 저장 시점의 식별자와 원문을 전달한다.
public record MeetingIndexRequested(Long projectId, Long minutesId, String text) {}
