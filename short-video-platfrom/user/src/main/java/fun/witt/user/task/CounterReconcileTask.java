package fun.witt.user.task;

import fun.witt.common.service.CounterService;
import fun.witt.constant.Constant;
import fun.witt.mapper.FavoriteMapper;
import fun.witt.mapper.RelationMapper;
import fun.witt.mapper.UserMapper;
import fun.witt.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

@Slf4j
@Component
public class CounterReconcileTask {

    private static final int BATCH_SIZE = 50;
    private long lastReconcileUserId = 0;

    @Autowired
    private CounterService counterService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RelationMapper relationMapper;

    @Autowired
    private FavoriteMapper favoriteMapper;

    @Scheduled(fixedDelay = 600000)
    public void reconcile() {
        Example userExample = new Example(User.class);
        userExample.createCriteria().andGreaterThan("id", lastReconcileUserId);
        userExample.setOrderByClause("id ASC");

        List<User> users = userMapper.selectByExample(userExample);
        if (users.isEmpty()) {
            lastReconcileUserId = 0;
            return;
        }

        int fixed = 0;
        for (User user : users) {
            if (fixed >= BATCH_SIZE) {
                break;
            }

            long userId = user.getId();
            long[] counters = counterService.getUserCounters(userId);
            long redisFollow = counters[0];
            long redisFollower = counters[1];
            long redisFavorited = counters[2];
            long redisFavorite = counters.length > 3 ? counters[3] : 0;

            long dbFollow = relationMapper.countFollowingActive(userId);
            long dbFollower = relationMapper.countFollowerActive(userId);
            long dbFavorited = favoriteMapper.countReceivedByAuthor(userId);
            long dbFavorite = favoriteMapper.countByUser(userId);

            boolean needFix = false;
            if (redisFollow != dbFollow) {
                counterService.setCounter(userId, Constant.OFFSET_FOLLOW_COUNT, dbFollow);
                needFix = true;
            }
            if (redisFollower != dbFollower) {
                counterService.setCounter(userId, Constant.OFFSET_FOLLOWER_COUNT, dbFollower);
                needFix = true;
            }
            if (redisFavorited != dbFavorited) {
                counterService.setCounter(userId, Constant.OFFSET_FAVORITED_COUNT, dbFavorited);
                needFix = true;
            }
            if (redisFavorite != dbFavorite) {
                counterService.setCounter(userId, Constant.OFFSET_FAVORITE_COUNT, dbFavorite);
                needFix = true;
            }

            if (needFix) {
                fixed++;
                log.info("counter reconciled userId={}: follow({} -> {}), follower({} -> {}), favorited({} -> {}), favorite({} -> {})",
                        userId, redisFollow, dbFollow, redisFollower, dbFollower, redisFavorited, dbFavorited,
                        redisFavorite, dbFavorite);
            }

            lastReconcileUserId = userId;
        }

        if (fixed > 0) {
            log.info("counter reconcile fixed {} users", fixed);
        }
    }
}
