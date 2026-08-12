package fun.witt.favorite.feign;

import fun.witt.api.feign.FavoriteFeignClient;
import fun.witt.favorite.service.FavoriteService;
import fun.witt.mapper.FavoriteMapper;
import fun.witt.model.Favorite;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tk.mybatis.mapper.entity.Example;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/feign/favorite")
public class FavoriteFeignClientImpl implements FavoriteFeignClient {

    @Autowired
    private FavoriteService favoriteService;

    @Autowired
    private FavoriteMapper favoriteMapper;

    /**
     * 批量查询点赞状态：优先走 Redis Bitmap，极速响应
     */
    public Map<Long, Boolean> batchFavoriteState(@RequestParam("videoIDList") List<Long> videoIDList,
                                                 @RequestParam("userID") long userID) {
        return favoriteService.batchLikeState(videoIDList, userID);
    }

    @Override
    public List<Long> listUserFavoriteVideo(@RequestParam("userID") long userID) {
        Example example = new Example(Favorite.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("userId", userID);
        List<Favorite> favoriteList = favoriteMapper.selectByExample(example);
        if (favoriteList.isEmpty()) {
            return new ArrayList<>();
        }
        return favoriteList.stream()
                .map(Favorite::getVideoId)
                .collect(Collectors.toList());
    }
}
