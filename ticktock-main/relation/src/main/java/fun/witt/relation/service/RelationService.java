package fun.witt.relation.service;

import fun.witt.api.vo.ResultVO;

public interface RelationService {
    ResultVO followAction(String actionType, long userID, long loginUserID);

    boolean followState(long userID, long loginUserID);

    /**
     * 关注列表
     */
    ResultVO followList(long userID, long loginUserID);

    /**
     * 粉丝列表
     */
    ResultVO followerList(long userID, long loginUserID);
}
