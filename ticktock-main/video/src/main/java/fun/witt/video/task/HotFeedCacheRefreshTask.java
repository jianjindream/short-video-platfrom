package fun.witt.video.task;

import fun.witt.video.service.FeedCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HotFeedCacheRefreshTask {

    @Autowired
    private FeedCacheService feedCacheService;

    @Scheduled(fixedDelayString = "${feed.hot-cache-refresh-ms:60000}", initialDelay = 10000)
    public void refresh() {
        if (feedCacheService.rebuildHotFeedCache()) {
            log.debug("hot feed cache refreshed");
        }
    }
}
