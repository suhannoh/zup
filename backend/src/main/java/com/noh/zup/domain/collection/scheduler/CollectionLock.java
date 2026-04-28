package com.noh.zup.domain.collection.scheduler;

public record CollectionLock(
        String key,
        String token
) {
}
