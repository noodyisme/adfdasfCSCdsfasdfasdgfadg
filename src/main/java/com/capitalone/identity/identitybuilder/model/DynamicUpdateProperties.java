package com.capitalone.identity.identitybuilder.model;

import org.springframework.lang.Nullable;

import java.time.Duration;
import java.time.LocalTime;

public interface DynamicUpdateProperties {
    @Nullable
    LocalTime getTimeOfDayUTC();

    @Nullable
    Duration getInterval();

}
