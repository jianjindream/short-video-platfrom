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

@Slf4j
public class FfmpegUtils {
    public static final String format = "jpg";

    public static byte[] videoFrame(MultipartFile file) {
        try (FFmpegFrameGrabber ff = new FFmpegFrameGrabber(file.getInputStream())) {
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
            ByteArrayOutputStream imageOut = new ByteArrayOutputStream();
            ImageIO.write(frameToBufferedImage(frame), format, imageOut);
            return imageOut.toByteArray();
        } catch (IOException e) {
            log.error("video cut frame fail", new RuntimeException(e));
        }
        return null;
    }

    /**
     * 帧转为流
     */
    private static RenderedImage frameToBufferedImage(Frame frame) {
        BufferedImage bufferedImage;
        try (Java2DFrameConverter converter = new Java2DFrameConverter()) {
            bufferedImage = converter.getBufferedImage(frame);
        }
        return bufferedImage;
    }
}

