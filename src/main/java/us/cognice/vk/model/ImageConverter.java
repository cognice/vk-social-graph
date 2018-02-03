package us.cognice.vk.model;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ImageConverter {

    public static Image decode(String base64) throws IOException {
        BASE64Decoder decoder = new BASE64Decoder();
        ByteArrayInputStream rocketInputStream = new ByteArrayInputStream(decoder.decodeBuffer(base64));
        return new Image(rocketInputStream);
    }

    public static String encode(Image image) throws IOException {
        BufferedImage bImage = SwingFXUtils.fromFXImage(image, null);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(bImage, "png", outputStream);
        BASE64Encoder encoder = new BASE64Encoder();
        return encoder.encode(outputStream.toByteArray());
    }

}
