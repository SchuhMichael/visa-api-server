package eu.ill.visa.cloud.domain;

public enum CloudInstanceState {
    UNKNOWN,
    BUILDING,
    BUILD,
    STARTING,
    ACTIVE,
    STOPPING,
    STOPPED,
    REBOOTING,
    UNAVAILABLE,
    ERROR,
    DELETED,
}
