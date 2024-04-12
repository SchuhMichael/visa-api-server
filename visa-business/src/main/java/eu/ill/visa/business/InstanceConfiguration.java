package eu.ill.visa.business;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "business.instance")
public interface InstanceConfiguration {

    Integer userMaxLifetimeDurationHours();
    Integer staffMaxLifetimeDurationHours();
    Integer userMaxInactivityDurationHours();
    Integer staffMaxInactivityDurationHours();
    Integer defaultUserInstanceQuota();
    Integer activityRetentionPeriodDays();

}
