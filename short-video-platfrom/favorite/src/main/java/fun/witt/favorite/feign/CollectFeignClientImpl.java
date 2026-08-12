package fun.witt.favorite.feign;

import fun.witt.api.feign.CollectFeignClient;
import fun.witt.favorite.service.CollectService;
import fun.witt.mapper.CollectMapper;
import fun.witt.model.Collect;
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
@RequestMapping("/feign/collect")
public class CollectFeignClientImpl implements CollectFeignClient {

    @Autowired
    private CollectService collectService;

    @Autowired
    private CollectMapper collectMapper;

    @Override
    public Map<Long, Boolean> batchCollectState(@RequestParam("videoIDList") List<Long> videoIDList,
                                                @RequestParam("userID") long userID) {
        return collectService.batchCollectState(videoIDList, userID);
    }

    @Override
    public List<Long> listUserCollectVideo(@RequestParam("userID") long userID) {
        Example example = new Example(Collect.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("userId", userID);
        List<Collect> collectList = collectMapper.selectByExample(example);
        if (collectList.isEmpty()) {
            return new ArrayList<>();
        }
        return collectList.stream()
                .map(Collect::getVideoId)
                .collect(Collectors.toList());
    }
}
