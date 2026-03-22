package fun.witt.video.utils;

import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
public class FfmpegUtils {
    public static final String format = "jpg";

    public static byte[] videoFrame(MultipartFile file) {
        try (FFmpegFrameGrabber ff = new FFmpegFrameGrabber(file.getInputStream())) {
            return grabFrame(ff);
        } catch (IOException e) {
            log.error("video cut frame fail", new RuntimeException(e));
        }
        return null;
    }

    /**
     * 从视频 URL（如 MinIO 预签名地址）提取封面帧，
     * FFmpeg 通过 HTTP 协议直接读取，无需下载完整文件到本地
     */
    public static byte[] videoFrameFromUrl(String videoUrl) {
        try (FFmpegFrameGrabber ff = new FFmpegFrameGrabber(videoUrl)) {
            return grabFrame(ff);
        } catch (IOException e) {
            log.error("video cut frame from url fail", new RuntimeException(e));
        }
        return null;
    }

    /**
     * 从输入流中提取封面帧
     */
    public static byte[] videoFrame(InputStream inputStream) {
        try (FFmpegFrameGrabber ff = new FFmpegFrameGrabber(inputStream)) {
            return grabFrame(ff);
        } catch (IOException e) {
            log.error("video cut frame from stream fail", new RuntimeException(e));
        }
        return null;
    }

    private static byte[] grabFrame(FFmpegFrameGrabber ff) throws IOException {
        ff.start();
        int ftp = ff.getLengthInFrames();
        int flag = 0;
        Frame frame = null;
        while (flag <= ftp) {
            frame = ff.grabImage();
            if ((flag > 12) && (frame != null)) {
                break;
            }
            flag++;
        }
        ff.stop();
        if (frame == null) {
            return null;
        }
        ByteArrayOutputStream imageOut = new ByteArrayOutputStream();
        ImageIO.write(frameToBufferedImage(frame), format, imageOut);
        return imageOut.toByteArray();
    }

    private static RenderedImage frameToBufferedImage(Frame frame) {
        BufferedImage bufferedImage;
        try (Java2DFrameConverter converter = new Java2DFrameConverter()) {
            bufferedImage = converter.getBufferedImage(frame);
        }
        return bufferedImage;
    }
}

