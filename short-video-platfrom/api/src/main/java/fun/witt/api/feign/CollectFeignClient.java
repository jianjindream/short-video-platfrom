package fun.witt.api.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "short-video-platfrom-favorite", path = "/douyin/feign/collect")
public interface CollectFeignClient {
    @GetMapping("/batch/state")
    Map<Long, Boolean> batchCollectState(@RequestParam("videoIDList") List<Long> videoIDList,
                                         @RequestParam("userID") long userID);

    @GetMapping("/user/list")
    List<Long> listUserCollectVideo(@RequestParam("userID") long userID);
}
