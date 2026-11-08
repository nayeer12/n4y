package com.n4y.client.core.event.events;

import com.n4y.client.core.event.Event;

/** Posted when the client is stopping; last chance to flush configs to disk. */
public record ClientShutdownEvent() implements Event {
}
