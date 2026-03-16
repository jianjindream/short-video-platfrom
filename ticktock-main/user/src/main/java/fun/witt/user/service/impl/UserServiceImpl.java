package fun.witt.user.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import fun.witt.api.feign.RelationFeignClient;
import fun.witt.api.utils.ConvertUtil;
import fun.witt.api.vo.UserExt;
import fun.witt.mapper.UserMapper;
import fun.witt.model.User;
import fun.witt.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.entity.Example;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RelationFeignClient feignClient;

    @Override
    public User queryNameIsExist(String name) {
        Example userExample = new Example(User.class);
        Example.Criteria criteria = userExample.createCriteria();
        criteria.andEqualTo("userName", name);
        return userMapper.selectOneByExample(userExample);
    }

    @Override
    public User createUser(String name, String password) {
        // fixme salt
        String hex = DigestUtil.sha1Hex(password);
        User user = new User();
        user.setUserName(name);
        user.setPassword(hex);
        user.setFollowCount(0L);
        user.setFollowerCount(0L);
        user.setTotalFavorited(0L);
        user.setFavoriteCount(0L);
        user.setSignature("default sign");
        user.setAvatar("https://tse1-mm.cn.bing.net/th/id/R-C.d83ded12079fa9e407e9928b8f300802?rik=Gzu6EnSylX9f1Q&riu=http%3a%2f%2fwww.webcarpenter.com%2fpictures%2fGo-gopher-programming-language.jpg&ehk=giVQvdvQiENrabreHFM8x%2fyOU70l%2fy6FOa6RS3viJ24%3d&risl=&pid=ImgRaw&r=0");
        user.setBackgroundImage("https://tse2-mm.cn.bing.net/th/id/OIP-C.sDoybxmH4DIpvO33-wQEPgHaEq?pid=ImgDet&rs=1");
        userMapper.insert(user);
        return user;
    }

    @Override
    public UserExt queryUserByID(long userID, long loginUserID) {
        User user = userMapper.selectByPrimaryKey(userID);
        UserExt userExt = ConvertUtil.convertUser(user);
        if (loginUserID > 0) {
            userExt.setFollow(feignClient.followState(user.getId(), loginUserID));
        }
        return userExt;
    }

    @Override
    public List<UserExt> listUserByIDList(List<Long> userIDList, long loginUserID) {
        Example example = new Example(User.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andIn("id", userIDList);
        List<User> userList = userMapper.selectByExample(example);
        if (userList.isEmpty()) {
            return new ArrayList<>();
        }

        return userList.stream().map(user -> {
            UserExt userExt = ConvertUtil.convertUser(user);
            if (loginUserID > 0) {
                userExt.setFollow(feignClient.followState(user.getId(), loginUserID));
            }
            return userExt;
        }).collect(Collectors.toList());
    }
}
