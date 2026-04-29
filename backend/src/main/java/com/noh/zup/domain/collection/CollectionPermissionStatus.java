package com.noh.zup.domain.collection;

public enum CollectionPermissionStatus {
    ALLOWED_TO_COLLECT,
    MANUAL_REVIEW_ONLY,
    BLOCKED_BY_ROBOTS,
    BLOCKED_BY_TERMS,
    LOGIN_REQUIRED,
    UNKNOWN_NEEDS_REVIEW
}
