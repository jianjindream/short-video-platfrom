package fun.witt.mapper;

import fun.witt.model.Follower;
import fun.witt.utils.MyMapper;
import org.apache.ibatis.annotations.Param;

public interface FollowerMapper extends MyMapper<Follower> {

    int insertIgnore(@Param("fromUserId") Long fromUserId, @Param("toUserId") Long toUserId);

    int deleteByUsers(@Param("fromUserId") Long fromUserId, @Param("toUserId") Long toUserId);
}
