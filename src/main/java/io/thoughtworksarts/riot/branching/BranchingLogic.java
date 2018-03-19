package io.thoughtworksarts.riot.branching;

import io.thoughtworksarts.riot.branching.model.*;
import io.thoughtworksarts.riot.facialrecognition.FacialEmotionRecognitionAPI;
import javafx.application.Platform;
import javafx.scene.media.MediaMarkerEvent;
import javafx.util.Duration;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class BranchingLogic {

    public static final String PATH_TO_CONFIG = "src/main/resources/config.json";

    private JsonTranslator translator;
    private FacialEmotionRecognitionAPI facialRecognition;
    private Level[] levels;
    private Intro[] intros;
    private Credits[] credits;
    @Getter
    private String filmPath;
    @Getter
    private String audioPath;

    public BranchingLogic(FacialEmotionRecognitionAPI facialRecognition, JsonTranslator translator) throws Exception {
        this.facialRecognition = facialRecognition;
        this.translator = translator;
        initialise();
    }

    private void initialise() throws Exception {
        ConfigRoot root = translator.populateModelsFromJson(PATH_TO_CONFIG);
        levels = root.getLevels();
        filmPath = root.getMedia().getVideo();
        audioPath = root.getMedia().getAudio();
        intros = root.getIntros();
        credits = root.getCredits();
    }

    public Duration branchOnMediaEvent(MediaMarkerEvent arg, Boolean testing) {
        String key = arg.getMarker().getKey();
        String[] split = key.split(":");
        String category = split[0];
        int index = Integer.parseInt(split[1]);
        Map<String, EmotionBranch> branches = levels[index - 1].getBranch();

        if (category.equals("level")) {
            log.info("Level Marker: " + key);
            String value = facialRecognition.getDominantEmotion().name();
            EmotionBranch emotionBranch = branches.get(value.toLowerCase());
            return translator.convertToDuration(emotionBranch.getStart());
        } else if (category.equals("emotion")) {
            log.info("Emotion Marker: " + key);
            String emotionType = split[2];
            EmotionBranch emotionBranch = branches.get(emotionType);
            int outcomeNumber = emotionBranch.getOutcome();
            if (outcomeNumber > 0) {
                Level nextLevel = levels[outcomeNumber - 1];
                return translator.convertToDuration(nextLevel.getStart());
            } else {
                log.info("Credits: ");
                return translator.convertToDuration(credits[0].getStart());
            }
        } else if (category.equals("intro")) {
            log.info("Intro slide: " + key);
            return translator.convertToDuration("00:00.000");
        } else if (category.equals("credit")) {
            if (split[1].equals("2")) {
                log.info("Shutting down webcam: ");
                facialRecognition.endImageCapture();
                log.info("Exiting application: ");
                Platform.exit();
            }
        }

        if(testing){
            if(category.equals("nearLevel1Start")){
                String endLevel1 = levels[0].getEnd();
                return subtractTimeFromDuration("00:03.000", endLevel1);

            }
        }

        double currentTime = arg.getMarker().getValue().toMillis() + 1;
        return new Duration(currentTime);
    }


    public void addMarker(Map<String, Duration> markers, String nameForMarker, String index, String time) {
        String markerNameWithColon = nameForMarker + ":" + index;
        markers.put(markerNameWithColon, translator.convertToDuration(time));
    }

    public void addDurationMarker(Map<String, Duration> markers, String nameForMarker, String index, Duration time) {
        String markerNameWithColon = nameForMarker + ":" + index;
        markers.put(markerNameWithColon, time);
    }

    public void recordMarkers(Map<String, Duration> markers) {
        addMarker(markers, "intro", "3", intros[2].getEnd());
        System.out.println("here" + credits[1]);
        addMarker(markers, "credit", "2", credits[1].getEnd());

        for (Level level : levels) {
            addMarker(markers, "level", String.valueOf(level.getLevel()), level.getEnd());
            Map<String, EmotionBranch> branch = level.getBranch();
            branch.forEach((branchKey, emotionBranch) -> addMarker(markers, "emotion:" + level.getLevel(),
                    branchKey, emotionBranch.getEnd()));
        }

        //Testing Markers
        String startLevel1 = levels[0].getStart();
        Duration nearStartOfLevel = addTimeToDuration("00:03.000", startLevel1);
        addDurationMarker(markers, "nearLevel1Start", "1", nearStartOfLevel);

        String startFearScene = levels[0].getBranch().get("fear").getStart();
        Duration nearStartOfFearScene = addTimeToDuration("00:03.00", startFearScene);
        addDurationMarker(markers, "nearFearStart", "1", nearStartOfFearScene);

        String startAngerScene = levels[0].getBranch().get("anger").getStart();
        Duration nearStartOfAngerScene = addTimeToDuration("00:03.00", startAngerScene);
        addDurationMarker(markers, "nearAngerStart", "1", nearStartOfAngerScene);
    }

    //Strings must be in 00:00.000
    public Duration subtractTimeFromDuration(String seconds, String time){
        Duration timeInDuration = translator.convertToDuration(time);
        Duration secondsInDuration = translator.convertToDuration(seconds);
        return timeInDuration.subtract(secondsInDuration);
    }

    //Strings must be in 00:00.000
    public Duration addTimeToDuration(String seconds, String time){
        Duration timeInDuration = translator.convertToDuration(time);
        Duration secondsInDuration = translator.convertToDuration(seconds);
        return timeInDuration.add(secondsInDuration);
    }

    public Duration getProperIntroDuration(Duration currentTime) {
        Duration beginningOfIntro = translator.convertToDuration(intros[0].getStart());
        Duration secondIntroStart = translator.convertToDuration(intros[1].getStart());
        Duration thirdIntroStart = translator.convertToDuration(intros[2].getStart());
        Duration endOfIntro = translator.convertToDuration(intros[2].getEnd());

        Duration[] durations = new Duration[]{secondIntroStart, thirdIntroStart, endOfIntro};

        for (Duration duration : durations) {
            if (currentTime.lessThan(duration) && currentTime.greaterThan(beginningOfIntro)) {
                return duration;
            }
        }
        return null;
    }
}