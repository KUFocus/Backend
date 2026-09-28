package org.focus.logmeet.service;

// 삭제된 엔티티를 다시 조회하지 않도록 삭제 전 식별자를 전달한다.
public record MeetingIndexDeleteRequested(Long projectId, Long minutesId) {}
