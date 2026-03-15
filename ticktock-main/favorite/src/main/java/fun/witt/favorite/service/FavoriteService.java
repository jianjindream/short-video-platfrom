package fun.witt.favorite.service;

public interface FavoriteService {
    boolean likeAction(String actionType, long videoID, long userID);

}
