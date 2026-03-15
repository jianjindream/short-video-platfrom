package fun.witt.relation.service.impl;

import fun.witt.api.feign.UserFeignClient;
import fun.witt.api.vo.ResultVO;
import fun.witt.api.vo.UserExt;
import fun.witt.api.vo.UserListVO;
import fun.witt.constant.Constant;
import fun.witt.mapper.RelationMapper;
import fun.witt.model.Relation;
import fun.witt.relation.service.RelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.entity.Example;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RelationServiceImpl implements RelationService {

    @Autowired
    private RelationMapper relationMapper;

    @Autowired
    private UserFeignClient userFeignClient;


    @Override
    public ResultVO followAction(String actionType, long userID, long loginUserID) {
        // todo sync follow count
        switch (actionType) {
            case Constant.RELATION_FOLLOW -> {
                Relation relation = new Relation();
                relation.setFollowId(userID);
                relation.setFollowerId(loginUserID);
                if (relationMapper.insert(relation) > 0) {
                    return ResultVO.ok();
                }
            }
            case Constant.RELATION_UNFOLLOW -> {
                Example example = new Example(Relation.class);
                Example.Criteria criteria = example.createCriteria();
                criteria.andEqualTo("followId", userID);
                criteria.andEqualTo("followerId", loginUserID);
                if (relationMapper.deleteByExample(example) > 0) {
                    return ResultVO.ok();
                }
            }
        }
        return ResultVO.fail("");
    }

    @Override
    public boolean followState(long userID, long loginUserID) {
        Example example = new Example(Relation.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("followId", userID);
        criteria.andEqualTo("followerId", loginUserID);
        Relation relation = relationMapper.selectOneByExample(example);
        return relation != null;
    }

    /**
     * 关注列表
     */
    @Override
    public ResultVO followList(long userID, long loginUserID) {
        Example example = new Example(Relation.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("followerId", userID);
        List<Relation> relationList = relationMapper.selectByExample(example);

        UserListVO vo = new UserListVO();
        List<Long> followUserIDList = relationList.stream().map(Relation::getFollowId).toList();
        if (followUserIDList.isEmpty()) {
            return vo;
        }

        Map<Long, Boolean> followStateDict = relationList.stream()
                .collect(Collectors.toMap(Relation::getFollowId,
                        relation -> userID == loginUserID || relation.getFollowId() == loginUserID));

        List<UserExt> userExtList = userFeignClient.batchUserInfo(followUserIDList, 0);
        userExtList = userExtList.stream()
                .peek(userExt -> userExt.setFollow(followStateDict.getOrDefault(userExt.getId(), false)))
                .toList();
        vo.setUserList(userExtList);
        return vo;
    }

    /**
     * 粉丝列表
     */
    @Override
    public ResultVO followerList(long userID, long loginUserID) {
        Example example = new Example(Relation.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("followId", userID);
        List<Relation> relationList = relationMapper.selectByExample(example);

        UserListVO vo = new UserListVO();
        List<Long> followerUserIDList = relationList.stream().map(Relation::getFollowerId).toList();
        if (followerUserIDList.isEmpty()) {
            return vo;
        }

        Map<Long, Boolean> followerStateDict = relationList.stream()
                .collect(Collectors.toMap(Relation::getFollowerId
                        , relation -> relation.getFollowerId() == loginUserID));

        List<UserExt> userExtList = userFeignClient.batchUserInfo(followerUserIDList, 0);
        userExtList = userExtList.stream()
                .peek(userExt -> userExt.setFollow(followerStateDict.getOrDefault(userExt.getId(), false)))
                .toList();
        vo.setUserList(userExtList);
        return null;
    }

}