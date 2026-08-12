package fun.witt.favorite.service;

import java.util.List;
import java.util.Map;

public interface FavoriteService {

    boolean likeAction(String actionType, long videoID, long userID, long authorId);

    boolean isLiked(long videoID, long userID);

    Map<Long, Boolean> batchLikeState(List<Long> videoIDList, long userID);
}
