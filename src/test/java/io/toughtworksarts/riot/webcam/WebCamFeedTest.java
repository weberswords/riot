package io.toughtworksarts.riot.webcam;

import com.github.sarxos.webcam.Webcam;
import io.thoughtworksarts.riot.WebcamFeed;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class WebCamFeedTest {

    @Test
    public void shouldGetHighestResolutionAvailable() {

        // given
        WebcamFeed webcamFeed = new WebcamFeed();

        // when
        Dimension highestResolutionAvailable = webcamFeed.getHighestResolutionAvailable();
        double actual = highestResolutionAvailable.getHeight() * highestResolutionAvailable.getWidth();

        // then
        boolean isHighestResolution = Arrays.stream(Webcam.getDefault().getViewSizes())
                .map(this::getResolution)
                .allMatch(area -> area <= actual);

        assertThat(isHighestResolution).isTrue();
    }

    @Test
    public void shouldCreatePictureFileWithProperResolution() throws Exception {
        // given
        WebcamFeed webcamFeed = new WebcamFeed();


        // when
        File file = webcamFeed.takePicture();

        // then
        log.info("File created: '{}'", file.getCanonicalPath());

        assertThat(file).exists();

        BufferedImage bufferedImage = ImageIO.read(file);
        Dimension highestResolutionAvailable = webcamFeed.getHighestResolutionAvailable();

        int actualResolution = this.getResolution(bufferedImage);
        int expectedResolution = this.getResolution(highestResolutionAvailable);

        assertThat(actualResolution).isEqualTo(expectedResolution);
    }

    private int getResolution(Dimension dimension) {
        Double area = dimension.getWidth() * dimension.getHeight();
        return area.intValue();
    }

    private int getResolution(BufferedImage bufferedImage) {
        return bufferedImage.getWidth() * bufferedImage.getHeight();
    }


}