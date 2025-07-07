package main;

import tictac.TicTacToeState;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainAlphaZero {
    public static void main(String[] args) throws IOException {
        MCTSAlpha<Integer> mcts = new MCTSAlpha<>();
        GameState<Integer> currentState = new TicTacToeState();
        List<TrainingExample<Integer>> trainingExamples = new ArrayList<>();
        /*for(int i=0; i< 3 ; i++) {
            if(i< 1) {
                trainingExamples = mcts.selfPlayGame(currentState, 200, 200, 500, new NeuralNetwork<>());
            } else if(i < 2) {
                trainingExamples.addAll(mcts.selfPlayGame(currentState, 8, 200, 500, new NeuralNetwork<>()));
            } else {
               trainingExamples.addAll(mcts.selfPlayGame(currentState, 200, 8, 500, new NeuralNetwork<>()));
            }

        }*/
        NeuralNetwork nn = new NeuralNetwork<>();
        for(int i=0; i< 1; i++) {
            List<TrainingExample<Integer>> trainingExamples1 = mcts.selfPlayGame(currentState, 100, 100, 1, nn);
            //WriteFile.writeTrainingExamplesToJSONL(i+"training_dataRI.jsonl", trainingExamples, false);
            //nn.train(trainingExamples1);
            //nn.load_model();
        }
        //WriteFile.writeTrainingExamplesToJSONL("training_data.jsonl", trainingExamples, false);
    }
}
