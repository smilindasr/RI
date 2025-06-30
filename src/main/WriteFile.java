package main;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class WriteFile {
    public static void write(String filename, String content) throws IOException {
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write(content);
        }
    }

    /**
     * Writes a list of training examples to a JSONL file.
     * Each line in the file will be a JSON object representing one training example.
     *
     * @param filename The name of the file to write to
     * @param examples The list of training examples to write
     * @param append Whether to append to the file (true) or overwrite it (false)
     * @throws IOException If there is an error writing to the file
     */
    public static <ActionType> void writeTrainingExamplesToJSONL(
            String filename,
            List<TrainingExample<ActionType>> examples,
            boolean append) throws IOException {

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename, append))) {
            for (TrainingExample<ActionType> example : examples) {
                // Convert the example to JSON format
                String jsonLine = convertTrainingExampleToJSON(example);
                writer.write(jsonLine);
                writer.newLine();
            }
        }
    }

    private static <ActionType> String convertTrainingExampleToJSON(TrainingExample<ActionType> example) {
        // Convert board to string representation
        String boardStr = Arrays.stream(example.board)
                .map(row -> Arrays.toString(row))
                .collect(Collectors.joining(", ", "[", "]"));

        // Convert policy to string representation
        String policyStr = example.policy.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", ", "[", "]"));

        // Build JSON string
        return String.format(
                "{\"board\": %s, \"policy\": %s, \"value\": %.6f, \"current_player\": %d}",
                boardStr,
                policyStr,
                example.value,
                example.current_player
        );
    }
}
