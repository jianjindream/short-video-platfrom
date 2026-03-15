package fun.witt.relation.feign;

import fun.witt.api.feign.RelationFeignClient;
import fun.witt.relation.service.RelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/feign/relation")
public class RelationFeignClientImpl implements RelationFeignClient {

    @Autowired
    private RelationService relationService;

    @Override
    public boolean followState(long userID, long loginUserID) {
        return relationService.followState(userID, loginUserID);
    }

}
