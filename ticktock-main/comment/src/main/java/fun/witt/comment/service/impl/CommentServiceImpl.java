package fun.witt.comment.service.impl;

import fun.witt.api.feign.UserFeignClient;
import fun.witt.api.utils.ConvertUtil;
import fun.witt.api.vo.CommentExt;
import fun.witt.api.vo.CommentListVO;
import fun.witt.api.vo.CommentVO;
import fun.witt.api.vo.ResultVO;
import fun.witt.api.vo.UserExt;
import fun.witt.comment.service.CommentService;
import fun.witt.mapper.CommentMapper;
import fun.witt.mapper.VideoMapper;
import fun.witt.model.Comment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.entity.Example;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentServiceImpl implements CommentService {

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private VideoMapper videoMapper;

    @Autowired
    private UserFeignClient userFeignClient;

    @Override
    public CommentVO publish(long videoID, long loginUserID, String text) {
        Comment comment = new Comment();
        comment.setUserId(loginUserID);
        comment.setVideoId(videoID);
        comment.setCommentText(text);
        comment.setCreateTime(new Date());
        commentMapper.insert(comment);
        videoMapper.increaseCommentCount(videoID, 1);

        CommentExt commentExt = ConvertUtil.convertComment(comment);
        UserExt userExt = userFeignClient.getUserInfo(loginUserID, 0L);
        commentExt.setUser(userExt);
        CommentVO vo = new CommentVO();
        vo.setComment(commentExt);
        return vo;
    }

    @Override
    public ResultVO delete(long commentID, long loginUserID) {
        Comment comment = commentMapper.selectByPrimaryKey(commentID);
        if (comment == null || comment.getUserId() == null || comment.getUserId() != loginUserID) {
            return ResultVO.fail("fail");
        }
        if (commentMapper.deleteByPrimaryKey(commentID) > 0) {
            videoMapper.increaseCommentCount(comment.getVideoId(), -1);
            return ResultVO.ok();
        }
        return ResultVO.fail("fail");
    }

    @Override
    public CommentListVO list(long videoID, long loginUserID) {
        Example example = new Example(Comment.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("videoId", videoID);
        List<Comment> commentList = commentMapper.selectByExample(example);

        CommentListVO vo = new CommentListVO();
        if (commentList.isEmpty()) {
            return vo;
        }

        List<CommentExt> commentExtList = commentList.stream().map(comment -> {
            CommentExt commentExt = ConvertUtil.convertComment(comment);
            commentExt.setUser(userFeignClient.getUserInfo(comment.getUserId(), loginUserID));
            return commentExt;
        }).collect(Collectors.toList());
        vo.setCommentList(commentExtList);
        return vo;
    }
}
