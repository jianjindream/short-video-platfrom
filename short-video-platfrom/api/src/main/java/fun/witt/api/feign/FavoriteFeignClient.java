package fun.witt.api.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "ticktock-favorite", path = "/douyin/feign/favorite")
public interface FavoriteFeignClient {
    @GetMapping("/batch/state")
    Map<Long, Boolean> batchFavoriteState(@RequestParam("videoIDList") List<Long> videoIDList, @RequestParam("userID") long userID);

    @GetMapping("/user/list")
    List<Long> listUserFavoriteVideo(@RequestParam("userID") long userID);
}
