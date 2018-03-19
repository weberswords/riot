package io.thoughtworksarts.riot.functional;

import io.thoughtworksarts.riot.audio.AudioPlayer;
import io.thoughtworksarts.riot.audio.JavaSoundAudioPlayer;
import io.thoughtworksarts.riot.audio.RiotAudioPlayer;
import io.thoughtworksarts.riot.branching.BranchingLogic;
import io.thoughtworksarts.riot.branching.JsonTranslator;
import io.thoughtworksarts.riot.branching.model.ConfigRoot;
import io.thoughtworksarts.riot.branching.model.Level;
import io.thoughtworksarts.riot.facialrecognition.Emotion;
import io.thoughtworksarts.riot.facialrecognition.FacialEmotionRecognitionAPI;
import io.thoughtworksarts.riot.utilities.OSChecker;
import io.thoughtworksarts.riot.video.MediaControl;
import io.thoughtworksarts.riot.video.MoviePlayer;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;


import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@Slf4j
public class FearJourney extends Application {

    public static final String PATH_TO_CONFIG = "src/main/resources/config.json";


    @Mock FacialEmotionRecognitionAPI facialEmotionRecognitionAPI;
    private MediaControl mediaControl;
    private static JsonTranslator jsonTranslator;
    private static MediaControl mediaControlSpy;

    @Test
    public static void main(String... args) throws Exception {
        log.info("Starting Riot...");
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        initMocks(this);
        when(facialEmotionRecognitionAPI.getDominantEmotion()).thenReturn(Emotion.FEAR);

        jsonTranslator = new JsonTranslator();
        ConfigRoot root = jsonTranslator.populateModelsFromJson(PATH_TO_CONFIG);
        Level[] levels = root.getLevels();

        //Play the movie
        RiotAudioPlayer audioPlayer = OSChecker.isWindows() ? new AudioPlayer() : new JavaSoundAudioPlayer();
        BranchingLogic branchingLogic = new BranchingLogic(facialEmotionRecognitionAPI, jsonTranslator);
        mediaControl = new MediaControl(branchingLogic, audioPlayer, jsonTranslator.convertToDuration("03:47.000"), true);

        mediaControlSpy = Mockito.spy(mediaControl);
        MoviePlayer moviePlayer = new MoviePlayer(primaryStage, mediaControl);
        moviePlayer.initialise();
        mediaControl.play();
//        verifySeek(levels[0].getBranch().get("fear").getStart());
    }


    @Override
    public void stop() throws Exception {
        super.stop();
        mediaControl.shutdown();
    }

    public void verifySeek(String time){
        verify(mediaControlSpy, timeout(10000)).seek(jsonTranslator.convertToDuration(time));
}

}
