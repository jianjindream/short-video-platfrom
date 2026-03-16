package fun.witt.favorite.service.impl;

import fun.witt.constant.Constant;
import fun.witt.favorite.service.FavoriteService;
import fun.witt.mapper.FavoriteMapper;
import fun.witt.model.Favorite;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.entity.Example;

@Service
public class FavoriteServiceImpl implements FavoriteService {

    @Autowired
    private FavoriteMapper favoriteMapper;

    @Override
    public boolean likeAction(String actionType, long videoID, long userID) {
        // todo sync video favorite count; user video favorite count
        switch (actionType) {
            case Constant.FAVORITE_LIKE: {
                Favorite favorite = new Favorite();
                favorite.setUserId(userID);
                favorite.setVideoId(videoID);
                return favoriteMapper.insert(favorite) > 0;
            }
            case Constant.FAVORITE_UNLIKE: {
                Example example = new Example(Favorite.class);
                Example.Criteria criteria = example.createCriteria();
                criteria.andEqualTo("videoId", videoID);
                criteria.andEqualTo("userId", userID);
                return favoriteMapper.deleteByExample(example) > 0;
            }
            default:
                return false;
        }
    }

}
