package fun.witt.mapper;

import fun.witt.model.Favorite;
import fun.witt.utils.MyMapper;
import org.apache.ibatis.annotations.Param;

public interface FavoriteMapper extends MyMapper<Favorite> {
    int insertIgnore(@Param("userId") Long userId, @Param("videoId") Long videoId);

    int deleteByUserAndVideo(@Param("userId") Long userId, @Param("videoId") Long videoId);

    long countByUser(@Param("userId") Long userId);

    long countByVideo(@Param("videoId") Long videoId);

    long countReceivedByAuthor(@Param("authorId") Long authorId);
}
