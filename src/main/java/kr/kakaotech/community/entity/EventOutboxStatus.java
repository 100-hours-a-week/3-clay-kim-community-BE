package kr.kakaotech.community.entity;

public enum EventOutboxStatus {
    PENDING,
    PROCESSING,
    PROCESSED,
    FAILED
}
