package io.thoughtworksarts.riot.video;

import io.thoughtworksarts.riot.audio.RiotAudioPlayer;
import io.thoughtworksarts.riot.branching.BranchingLogic;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

@Slf4j
public class MediaControl extends BorderPane {

    public static final String DRIVER_NAME = "ASIO4ALL v2";

    private BranchingLogic branchingLogic;
    private RiotAudioPlayer audioPlayer;
    private MediaPlayer filmPlayer;

    public MediaControl(BranchingLogic branchingLogic, RiotAudioPlayer audioPlayer, Duration startTime, Boolean testing) throws Exception {
        this.branchingLogic = branchingLogic;
        //Video relate
        String filmPath = this.branchingLogic.getFilmPath();
        String pathToFilm = new File(String.valueOf(filmPath)).toURI().toURL().toString();
        setUpFilmPlayer(pathToFilm, startTime, testing);
        setUpPane();
        //Audio related
        this.audioPlayer = audioPlayer;
        this.audioPlayer.initialise(DRIVER_NAME, this.branchingLogic.getAudioPath());
    }

    private void setUpPane() {
        MediaView mediaView = new MediaView(filmPlayer);
        Pane pane = new Pane();
        mediaView.setOnMouseClicked(event -> handleClickDuringIntro());
        pane.getChildren().add(mediaView);
        pane.setStyle("-fx-background-color: black;");
        setCenter(pane);
    }

    private void setUpFilmPlayer(String pathToFilm, Duration startTime, Boolean testing) {
        Media media = new Media(pathToFilm);
        branchingLogic.recordMarkers(media.getMarkers());
        filmPlayer = new MediaPlayer(media);

        filmPlayer.setAutoPlay(false);
        filmPlayer.setOnMarker(arg -> {
            Duration duration = branchingLogic.branchOnMediaEvent(arg, testing);
            seek(duration);
        });

        filmPlayer.setOnReady(() -> {
                    filmPlayer.seek(startTime);
                    audioPlayer.seek(startTime.toSeconds());
                }
        );
    }

    private void handleClickDuringIntro() {
        Duration duration = branchingLogic.getProperIntroDuration(filmPlayer.getCurrentTime());
        if (duration != null) {
            seek(duration);
        } else {
            log.info("no clicking action outside of intro section");
        }
    }

    public void pause() {
        log.info("Pause");
        filmPlayer.pause();
    }

    public void play() {
        log.info("Play");
        filmPlayer.play();
        audioPlayer.resume();
    }

    public void seek(Duration duration) {
        filmPlayer.seek(duration);
        audioPlayer.seek(duration.toSeconds());
        audioPlayer.resume();  // this needs to be here because the audioPlayer stops after seeking sometimes

    }

    public void shutdown() {
        log.info("Shutting Down");
        filmPlayer.stop();
        audioPlayer.shutdown();
    }
}