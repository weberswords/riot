package io.thoughtworksarts.riot.video;

import io.thoughtworksarts.riot.branching.BranchingLogic;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.MalformedURLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;

@Slf4j
public class MediaControl extends BorderPane {

    private BranchingLogic branchingLogic;
    private MediaPlayer filmPlayer;
    private MediaPlayer audioPlayer;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    public MediaControl(BranchingLogic branchingLogic, Duration startTime) throws Exception {
        this.branchingLogic = branchingLogic;
        //Video relate
        setUpFilmPlayer(startTime);
        setUpPane();
        //Audio related
        setUpAudioPlayer();
    }

    private void setUpAudioPlayer() throws MalformedURLException {
        String pathToAudio = getPathToMedia(branchingLogic.getAudioPath());
        Media audioMedia = new Media(pathToAudio);
        audioPlayer = new MediaPlayer(audioMedia);
        audioPlayer.setAutoPlay(false);
    }

    private void setUpPane() {
        MediaView mediaView = new MediaView(filmPlayer);
        Pane pane = new Pane();
        mediaView.setOnMouseClicked(event -> handleClickDuringIntro());
        pane.getChildren().add(mediaView);
        pane.setStyle("-fx-background-color: black;");
        setCenter(pane);
    }

    private void setUpFilmPlayer(Duration startTime) throws MalformedURLException {
        String pathToFilm = getPathToMedia(branchingLogic.getFilmPath());
        Media media = new Media(pathToFilm);
        branchingLogic.recordMarkers(media.getMarkers());
        filmPlayer = new MediaPlayer(media);
        filmPlayer.setAutoPlay(false);

        filmPlayer.setOnMarker(arg -> {
            Duration duration = branchingLogic.branchOnMediaEvent(arg);
            seek(duration);
        });
        filmPlayer.setOnReady(() -> {
                    filmPlayer.seek(startTime);
                    audioPlayer.seek(startTime);
                }
        );

        filmPlayer.setOnPlaying(()->{
            executor.submit(()->{
                while(filmPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                    log.info("FilmPlayer time in seconds : "+filmPlayer.getCurrentTime().toSeconds());
                    log.info("AudioPlayer time in seconds : "+audioPlayer.getCurrentTime().toSeconds());
                    sleepUninterruptibly(3, TimeUnit.SECONDS);
                }
            });
        });
    }

    private String getPathToMedia(String filmPath) throws MalformedURLException {
        return new File(String.valueOf(filmPath)).toURI().toURL().toString();
    }

    private void handleClickDuringIntro() {
        Duration duration = branchingLogic.getProperIntroDuration(filmPlayer.getCurrentTime());
        if (duration != null) {
            seek(duration);
        }
    }

    public void pause() {
        log.info("Pause");
        filmPlayer.pause();
    }

    public void play() {
        log.info("Play");
        filmPlayer.play();
        audioPlayer.play();
    }

    public void seek(Duration duration) {
        filmPlayer.seek(duration);
        audioPlayer.seek(duration);
    }

    public void shutdown() {
        log.info("Shutting Down");
        filmPlayer.stop();
        audioPlayer.stop();
    }
}