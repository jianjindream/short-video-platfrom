package fun.witt.comment.service;

import fun.witt.api.vo.CommentListVO;
import fun.witt.api.vo.CommentVO;
import fun.witt.api.vo.ResultVO;

public interface CommentService {

    CommentVO publish(long videoID, long loginUserID, String text);

    ResultVO delete(long commentID, long loginUserID);

    CommentListVO list(long videoID, long loginUserID);
}
