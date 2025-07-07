package main;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import java.util.HashMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NeuralNetwork<ActionType> {
    private static final String BASE_URL = "http://localhost:5000";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CloseableHttpClient httpClient = HttpClients.createDefault();

    public NeuralNetworkOutput<ActionType> predict(TrainingExample<ActionType> input) {
        try {
            HttpPost request = new HttpPost(BASE_URL + "/predict");
            String jsonData = objectMapper.writeValueAsString(input);
            request.setEntity(new StringEntity(jsonData, ContentType.APPLICATION_JSON));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() != 200) {
                    throw new IOException("Prediction request failed with status: " + response.getCode());
                }

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()))) {
                    String output = reader.lines().collect(Collectors.joining("\n"));
                    return deserializeOutput(output);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to make prediction request", e);
        }
    }

    public void train(List<TrainingExample<ActionType>> examples) {
        try {
            HttpPost request = new HttpPost(BASE_URL + "/train");
            String jsonData = objectMapper.writeValueAsString(examples);
            request.setEntity(new StringEntity(jsonData, ContentType.APPLICATION_JSON));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() != 200) {
                    throw new IOException("Prediction request failed with status: " + response.getCode());
                }

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()))) {
                    String output = reader.lines().collect(Collectors.joining("\n"));
                    System.out.println(output);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to make prediction request", e);
        }
    }

    public void load_model() {
        try {
            HttpPost request = new HttpPost(BASE_URL + "/load-model");

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() != 200) {
                    throw new IOException("Prediction request failed with status: " + response.getCode());
                }

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()))) {
                    String output = reader.lines().collect(Collectors.joining("\n"));
                    System.out.println(output);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to make prediction request", e);
        }
    }

    public void analyse_train_data(List<TrainingExample<ActionType>> examples) {
        String serializedExamples = serializeExamples(examples);
        ProcessBuilder processBuilder = new ProcessBuilder("python", "/Users/milindas/workspace/PythonProject1/analyze_training_data.py", serializedExamples);
        processBuilder.redirectErrorStream(true);
        Process process = null;
        StringBuilder result = new StringBuilder();
        try {
            process = processBuilder.start();
            InputStream inputStream = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;

            while ((line = reader.readLine()) != null) {
                System.out.println(line); // or handle the output as needed
                result.append(line);
            }
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void analyze_model_complexity(List<TrainingExample<ActionType>> examples) {
        String serializedExamples = serializeExamples(examples);
        ProcessBuilder processBuilder = new ProcessBuilder("python", "/Users/milindas/workspace/PythonProject1/analyze_model_complexity.py", serializedExamples);
        processBuilder.redirectErrorStream(true);
        Process process = null;
        StringBuilder result = new StringBuilder();
        try {
            process = processBuilder.start();
            InputStream inputStream = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;

            while ((line = reader.readLine()) != null) {
                System.out.println(line); // or handle the output as needed
                result.append(line);
            }
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private <T> String serializeExamples(T examples) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(examples);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * Deserializes the JSON output from the neural network into a NeuralNetworkOutput object.
     * Expected format:
     * {
     * "policy": [p1, p2, ..., pn],
     * "value": v
     * }
     * where p1...pn are policy probabilities and v is the value prediction.
     *
     * @param output The string output from the neural network
     * @return NeuralNetworkOutput object containing policy and value
     */
    @SuppressWarnings("unchecked")
    private NeuralNetworkOutput<ActionType> deserializeOutput(String output) {
        try {
            Map<String, Object> jsonMap = objectMapper.readValue(output, Map.class);
            NeuralNetworkOutput<ActionType> result = new NeuralNetworkOutput<>();

            // Parse policy array
            List<Double> policyList = (List<Double>) jsonMap.get("policy");
            Map<ActionType, Double> policyMap = new HashMap<>();
            for (int i = 0; i < policyList.size(); i++) {
                policyMap.put((ActionType) Integer.valueOf(i), policyList.get(i));
            }
            result.policyHead = policyMap;

            // Parse value
            result.valueHead = ((Number) jsonMap.get("value")).doubleValue();

            return result;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse JSON output: " + output, e);
        }
    }
}
