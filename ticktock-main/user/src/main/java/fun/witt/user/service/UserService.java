package fun.witt.user.service;

import fun.witt.api.vo.UserExt;
import fun.witt.model.User;

import java.util.List;

public interface UserService {

    User queryNameIsExist(String name);

    User createUser(String name, String password);

    UserExt queryUserByID(long userID, long loginUserID);

    List<UserExt> listUserByIDList(List<Long> userIDList, long loginUserID);

}
