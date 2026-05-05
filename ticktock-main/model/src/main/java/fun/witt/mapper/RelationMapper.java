package fun.witt.mapper;

import fun.witt.model.Relation;
import fun.witt.utils.MyMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RelationMapper extends MyMapper<Relation> {
    int insertIgnore(@Param("followerId") Long followerId, @Param("followId") Long followId);

    int deleteByUsers(@Param("followerId") Long followerId, @Param("followId") Long followId);

    int existsByUsers(@Param("followerId") Long followerId, @Param("followId") Long followId);

    List<Relation> listFollowingRows(@Param("followerId") Long followerId, @Param("limit") int limit);

    List<Relation> listFollowerRows(@Param("followId") Long followId, @Param("limit") int limit);

    long countFollowingActive(@Param("followerId") Long followerId);

    long countFollowerActive(@Param("followId") Long followId);
}
