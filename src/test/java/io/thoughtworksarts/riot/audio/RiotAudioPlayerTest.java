package io.thoughtworksarts.riot.audio;

import io.thoughtworksarts.riot.utilities.OSChecker;
import org.junit.Ignore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class RiotAudioPlayerTest {

    private RiotAudioPlayer audioPlayer;

    @Ignore
    void shouldPlayGivenFile() throws Exception {
        boolean isWindows = OSChecker.isWindows();
        audioPlayer = isWindows ? new AudioPlayer() : new JavaSoundAudioPlayer();
        String wavFile = this.getClass().getClassLoader().getResource("audio/audio.wav").getFile();
        audioPlayer.initialise("ASIO4ALL v2", wavFile);
        playAudio();
        audioPlayer.shutdown();
    }

    private void playAudio() throws InterruptedException {
        audioPlayer.resume();
        Thread.sleep(3000);
        audioPlayer.pause();
        Thread.sleep(3000);
        audioPlayer.seek(120);
        audioPlayer.resume();
        Thread.sleep(3000);
    }
}
