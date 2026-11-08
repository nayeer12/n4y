package com.n4y.client.core.event.events;

import com.n4y.client.core.event.Event;

import net.minecraft.client.Minecraft;

/**
 * Posted once per client tick (20 per second), after vanilla has ticked.
 * Module logic belongs here, never in render events.
 */
public record ClientTickEvent(Minecraft client) implements Event {
}
