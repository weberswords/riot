package io.thoughtworksarts.riot.branching;

import io.thoughtworksarts.riot.branching.model.*;
import io.thoughtworksarts.riot.facialrecognition.FacialEmotionRecognitionAPI;
import javafx.application.Platform;
import javafx.scene.media.MediaMarkerEvent;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

@Slf4j
public class BranchingLogic {

    private JsonTranslator translator;
    private FacialEmotionRecognitionAPI facialRecognition;
    private Level[] levels;
    private Intro[] intros;
    private Credits[] credits;
    private boolean visitedIntro = false;

    public BranchingLogic(FacialEmotionRecognitionAPI facialRecognition, JsonTranslator translator,ConfigRoot configRoot) {
        this.facialRecognition = facialRecognition;
        this.translator = translator;
        this.levels = configRoot.getLevels();
        this.intros = configRoot.getIntros();
        this.credits = configRoot.getCredits();
    }

    public Duration branchOnMediaEvent(MediaMarkerEvent arg) {
        String key = arg.getMarker().getKey();
        String[] split = key.split(":");
        String category = split[0];
        int index = Integer.parseInt(split[1]);
        Map<String, EmotionBranch> branches = levels[index - 1].getBranch();

        switch (category) {
            case "level":
                log.info("Level Marker: " + key);
                String value = facialRecognition.getDominantEmotion(branches.keySet()).name();
                EmotionBranch emotionBranch = branches.get(value.toLowerCase());
                return translator.convertToDuration(emotionBranch.getStart());
            case "emotion":
                log.info("Emotion Marker: " + key);
                String emotionType = split[2];
                emotionBranch = branches.get(emotionType);
                int outcomeNumber = emotionBranch.getOutcome();
                if (outcomeNumber > 0) {
                    Level nextLevel = levels[outcomeNumber - 1];
                    return translator.convertToDuration(nextLevel.getStart());
                } else {
                    log.info("Credits: ");
                    return translator.convertToDuration(credits[0].getStart());
                }
            case "intro":
                log.info("Intro slide: " + key);
                return translator.convertToDuration("00:00.000");
            case "credit":
                if (split[1].equals("2")) {
                    log.info("Shutting down webcam: ");
                    facialRecognition.endImageCapture();
                    log.info("Exiting application: ");
                    Platform.exit();
                }
        }

        double currentTime = arg.getMarker().getValue().toMillis() + 1;
        return new Duration(currentTime);
    }

    private void addMarker(Map<String, Duration> markers, String nameForMarker, String index, String time) {
        String markerNameWithColon = nameForMarker + ":" + index;
        markers.put(markerNameWithColon, translator.convertToDuration(time));
    }

    public void recordMarkers(Map<String, Duration> markers) {
        addMarker(markers, "intro", "3", intros[2].getEnd());
        addMarker(markers, "credit", "2", credits[1].getEnd());

        for (Level level : levels) {
            addMarker(markers, "level", String.valueOf(level.getLevel()), level.getEnd());
            Map<String, EmotionBranch> branch = level.getBranch();
            branch.forEach((branchKey, emotionBranch) -> addMarker(markers, "emotion:" + level.getLevel(),
                    branchKey, emotionBranch.getEnd()));
        }
    }

    public Duration getClickSeekTime(Duration currentTime) {
        Duration beginningOfIntroSlides = translator.convertToDuration(intros[0].getStart());
        Duration secondIntroStart = translator.convertToDuration(intros[1].getStart());
        Duration thirdIntroStart = translator.convertToDuration(intros[2].getStart());
        Duration endOfIntro = translator.convertToDuration(intros[2].getEnd());

        ArrayList<Duration> durations = new ArrayList<>();

        for (Level level : levels) {
            durations.add(translator.convertToDuration(level.getStart()));
        }

        durations.add(beginningOfIntroSlides);
        durations.add(secondIntroStart);
        durations.add(thirdIntroStart);
        durations.add(endOfIntro);

        Collections.sort(durations);  // needs to be ordered because intro slides are at end of film

        if (isDuringEmoScene(currentTime)) {
            return null;
        }

        for (Duration duration : durations) {
            isIntroVisited(currentTime, thirdIntroStart, beginningOfIntroSlides);

            if (currentTime.lessThan(duration)) {
                if (duration.equals(beginningOfIntroSlides) && visitedIntro) {
                    return translator.convertToDuration(credits[0].getStart());
                }
                return duration;
            }
        }
        return null;
    }

    private void isIntroVisited(Duration currentTime, Duration thirdIntroStart, Duration beginning) {
        if (currentTime.greaterThan(thirdIntroStart) || currentTime.lessThan(beginning)) {
            visitedIntro = true;
        }
    }

    private boolean isDuringEmoScene(Duration currentTime) {
        ArrayList<ArrayList<Duration>> emoTimes = new ArrayList<>();

        addEmoTimesToArray(emoTimes);

        addLastLevelEmoToIntroTime(emoTimes);

        for (ArrayList<Duration> times : emoTimes) {
            if (currentTime.greaterThan(times.get(0)) && currentTime.lessThan(times.get(1))) {
                return true;
            }
        }

        return false;
    }

    private void addLastLevelEmoToIntroTime(ArrayList<ArrayList<Duration>> emoTimes) {
        /*
        NOTE: because the Intro slides are directly after the end of the non-emotion measuring part of the last level,
        the end time for this has to be the start of the first Intro slide instead of the first Credit slide
        */
        ArrayList<Duration> currTimes = new ArrayList<>();
        currTimes.add(translator.convertToDuration(levels[levels.length - 1].getEnd()));
        currTimes.add(translator.convertToDuration(intros[0].getStart()));
        emoTimes.add(currTimes);
    }

    private void addEmoTimesToArray(ArrayList<ArrayList<Duration>> emoTimes) {
        for(int i = 0; i < levels.length - 1; i++) {
            ArrayList<Duration> currTimes = new ArrayList<>();
            currTimes.add(translator.convertToDuration(levels[i].getEnd()));
            currTimes.add(translator.convertToDuration(levels[i + 1].getStart()));
            emoTimes.add(currTimes);
        }
    }
}
