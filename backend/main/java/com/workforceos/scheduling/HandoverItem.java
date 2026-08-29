package com.workforceos.scheduling;

public record HandoverItem(
        HandoverItemType type,
        String content) {
}
