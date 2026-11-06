package com.digitaloffice.contracts;

/**
 * Abstraction for publishing domain events.
 *
 * <p>The default in-process implementation delegates to Spring's {@code ApplicationEventPublisher}.
 * A future Kafka-based implementation can replace it without any changes to domain logic.
 *
 * @see DomainEvent
 */
public interface DomainEventPublisher {

    /**
     * Publishes a domain event to all registered listeners.
     *
     * @param event the event to publish; must not be {@code null}
     */
    void publish(DomainEvent event);
}
