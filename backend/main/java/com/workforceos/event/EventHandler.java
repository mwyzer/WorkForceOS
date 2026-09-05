package com.workforceos.event;

public interface EventHandler {

    boolean supports(DomainEvent event);

    void onEvent(DomainEvent event);
}