package fun.witt.mapper;

import fun.witt.model.Collect;
import fun.witt.utils.MyMapper;
import org.apache.ibatis.annotations.Param;

public interface CollectMapper extends MyMapper<Collect> {
    int insertIgnore(@Param("userId") Long userId, @Param("videoId") Long videoId);

    int deleteByUserAndVideo(@Param("userId") Long userId, @Param("videoId") Long videoId);

    long countByUser(@Param("userId") Long userId);

    long countByVideo(@Param("videoId") Long videoId);
}
