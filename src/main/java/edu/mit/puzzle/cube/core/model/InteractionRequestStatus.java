package edu.mit.puzzle.cube.core.model;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

public enum InteractionRequestStatus {
    REQUESTED,
    ASSIGNED,
    ANSWERED,
    REJECTED;

    private static Set<InteractionRequestStatus> ASSIGNED_STATUSES =
            ImmutableSet.of(ASSIGNED, ANSWERED, REJECTED);
    private static Set<InteractionRequestStatus> TERMINAL_STATUSES = ImmutableSet.of(ANSWERED, REJECTED);

    public static InteractionRequestStatus getDefault() {
        return REQUESTED;
    }

    public boolean isAssigned() {
        return ASSIGNED_STATUSES.contains(this);
    }

    public boolean isTerminal() {
        return TERMINAL_STATUSES.contains(this);
    }
}
