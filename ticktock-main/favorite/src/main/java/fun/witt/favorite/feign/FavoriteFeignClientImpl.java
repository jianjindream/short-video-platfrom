package fun.witt.favorite.feign;

import com.google.common.collect.Maps;
import fun.witt.api.feign.FavoriteFeignClient;
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
    private FavoriteMapper favoriteMapper;

    public Map<Long, Boolean> batchFavoriteState(@RequestParam("videoIDList") List<Long> videoIDList,
                                                 @RequestParam("userID") long userID) {
        Example example = new Example(Favorite.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("userId", userID);
        Example.Criteria criteria1 = example.createCriteria();
        criteria1.andIn("videoId", videoIDList);
        List<Favorite> favoriteList = favoriteMapper.selectByExample(example);

        Map<Long, Boolean> result = Maps.newHashMap();
        if (favoriteList.isEmpty()) {
            return result;
        }

        return favoriteList.stream().collect(Collectors.toMap(Favorite::getVideoId, favorite -> true));
    }

    @Override
    public List<Long> listUserFavoriteVideo(long userID) {
        Example example = new Example(Favorite.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("userId", userID);
        List<Favorite> favoriteList = favoriteMapper.selectByExample(example);
        if (favoriteList.isEmpty()) {
            return new ArrayList<>();
        }
        return favoriteList.stream()
                .map(Favorite::getVideoId)
                .toList();
    }
}
