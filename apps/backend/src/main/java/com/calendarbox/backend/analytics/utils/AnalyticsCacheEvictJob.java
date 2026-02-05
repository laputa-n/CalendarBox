package com.calendarbox.backend.analytics.utils;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AnalyticsCacheEvictJob {

    @CacheEvict(
            cacheNames = {
                    "analytics:peopleList",
                    "analytics:peopleSummary",
                    "analytics:placeList",
                    "analytics:placeSummary",
                    "analytics:dayHour",
                    "analytics:trend"
            },
            allEntries = true
    )
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void evictAt4am() {
        // body 비워도 됨
    }
}

