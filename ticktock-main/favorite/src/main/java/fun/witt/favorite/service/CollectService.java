package fun.witt.favorite.service;

import java.util.List;
import java.util.Map;

public interface CollectService {

    boolean collectAction(String actionType, long videoId, long userId);

    boolean isCollected(long videoId, long userId);

    Map<Long, Boolean> batchCollectState(List<Long> videoIdList, long userId);
}
