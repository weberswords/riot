package io.thoughtworksarts.riot.facialrecognition;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.thoughtworksarts.riot.utilities.JSONReader;
import org.deeplearning4j.nn.modelimport.keras.InvalidKerasConfigurationException;
import org.deeplearning4j.nn.modelimport.keras.KerasModelImport;
import org.deeplearning4j.nn.modelimport.keras.UnsupportedKerasConfigurationException;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.io.IOException;
import java.util.List;

public class DeepLearningProcessor {

    private String _h5File;
    private String _jsonFile;
    private MultiLayerNetwork model;

    public DeepLearningProcessor(String configPath){
        String jsonConfig = JSONReader.readFile(configPath);
        ObjectMapper objectMapper = new ObjectMapper();
        FERNeuralNetConfigRoot configRoot;
        try {
            configRoot = objectMapper.readValue(jsonConfig, FERNeuralNetConfigRoot.class);
            _jsonFile = configRoot.getModelFile();
            _h5File = configRoot.getWeightsFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        initModel();
    }

    public DeepLearningProcessor(String jsonFile, String h5File) {
        _jsonFile = jsonFile;
        _h5File = h5File;
        initModel();
    }

    private void initModel() {
        try {
            model =  KerasModelImport.importKerasSequentialModelAndWeights(_jsonFile, _h5File);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidKerasConfigurationException e) {
            e.printStackTrace();
        } catch (UnsupportedKerasConfigurationException e) {
            e.printStackTrace();
        }
    }

    public float[] getEmotionPrediction(INDArray imageData) {
        INDArray prediction = model.output(imageData);
        return prediction.data().asFloat();
    }

    public List<String> getModelLayerNames() {
        return model.getLayerNames();
    }
}
