package com.digitaloffice.document;

import com.digitaloffice.contracts.DomainEvent;
import com.digitaloffice.contracts.DomainEventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringDomainEventPublisher implements DomainEventPublisher {
    private final ApplicationEventPublisher publisher;
    public SpringDomainEventPublisher(ApplicationEventPublisher publisher) { this.publisher = publisher; }
    @Override public void publish(DomainEvent event) { publisher.publishEvent(event); }
}
