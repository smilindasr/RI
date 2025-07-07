package main;

import java.util.*;
import java.util.concurrent.*;

public class MCTSAlpha<ActionType extends Comparable> {
    private final double explorationWeight;  // Used for standard UCT
    private final double c_puct = 2;      // Reduced from 4.0 to balance exploration/exploitation

    public MCTSAlpha() {
        this(1.0);  // Default exploration weight for standard UCT
    }

    public MCTSAlpha(double explorationWeight) {
        this.explorationWeight = explorationWeight;
    }

    List<TrainingExample<ActionType>> trainingExamples = new ArrayList<>();

    public List<TrainingExample<ActionType>> selfPlayGame(GameState<ActionType> initialState, int player1Iterations, int player2Iterations, int fullIterations, NeuralNetwork<ActionType> nn) {
        // Create thread pool and synchronization objects
        int numThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Future<List<TrainingExample<ActionType>>>> futures = new ArrayList<>();

        // Submit each full game iteration as a separate task
        for (int i = 0; i < fullIterations; i++) {
            int finalI = i;
            futures.add(executor.submit(() -> playOneGame(initialState, player1Iterations, player2Iterations, nn, finalI + 1)));
        }

        // Collect results from all games
        for (Future<List<TrainingExample<ActionType>>> future : futures) {
            try {
                trainingExamples.addAll(future.get());
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("Error collecting game results: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Shutdown executor and train on all collected examples
        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Executor shutdown interrupted: " + e.getMessage());
        }

        return trainingExamples;
    }

    private List<TrainingExample<ActionType>> playOneGame(GameState<ActionType> initialState, int player1Iterations, int player2Iterations, NeuralNetwork<ActionType> nn, int gameNumber) {
        List<TrainingExample<ActionType>> gameExamples = new ArrayList<>();
        GameState<ActionType> state = initialState;
        System.out.println("\n=== Starting New Self-Play Game " + gameNumber + " ===");
        MCTS<ActionType> mcts = new MCTS<>();
        double temperature = 1.0; // Start with high temperature for exploration
        int moveCount = 0;

        while (!state.isTerminal()) {
            Node<ActionType> rootNode = new Node<>(state, null);
            int currentIterations = state.getCurrentPlayer() == 0 ? player1Iterations : player2Iterations;

            System.out.println("Player " + state.getCurrentPlayer() + " thinking with " + currentIterations + " iterations...");

            // Run MCTS simulations
            for (int i = 0; i < currentIterations; i++) {
                //mcts.eachStep(state, rootNode);
                eachStepAlphaZero(state, rootNode, nn, moveCount);
            }

            // Adjust temperature based on move number
            if (moveCount > 15) {
                temperature = 0.1; // Lower temperature in late game
            }

            // Get improved policy from MCTS visit counts
            int currentPlayer = state.getCurrentPlayer() == 0 ? 1 : -1;
            Map<ActionType, Double> visitDistribution = rootNode.getVisitDistribution(temperature);

            TrainingExample<ActionType> example = new TrainingExample<>(
                    state.convertBoard(),
                    rootNode.getVisitDistributionSorted(temperature),
                    0,
                    currentPlayer);
            gameExamples.add(example);

            // Sample action based on visit distribution and temperature
            // ActionType action = sampleFromDistribution(visitDistribution, state);
            ActionType action = getBestAction(rootNode);
            state.printCurrentPlayer();
            state = state.takeAction(action);

            System.out.println("\nGame " + gameNumber + " - Board state after move " + action + ":");
            System.out.println(state);

            moveCount++;
        }

        // Game is finished, assign final result
        double gameResult = state.getReward(state.getCurrentPlayer());

        System.out.println("\n=== Game " + gameNumber + " Finished ===");
        System.out.println("Final board state:");
        System.out.println(state);
        state.printWinner();
        System.out.println("Game result value: " + gameResult);

        // Update values with temporal difference learning
        double discountFactor = .9;
        double lambda = 0.8; // TD(λ) parameter
        double sign = -1;
        for (int i = gameExamples.size() - 1; i >= 0; i--) {
            TrainingExample<ActionType> ex = gameExamples.get(i);
            int depthFromEnd = gameExamples.size() - i;

            // Compute TD target using λ-returns
            double tdTarget = gameResult;
            if (i < gameExamples.size() - 1) {
                double nextValue = gameExamples.get(i + 1).value;
                tdTarget = (1 - lambda) * (-nextValue) + lambda * (gameResult * Math.pow(discountFactor, depthFromEnd - 1));
            }

            ex.value = tdTarget * sign;
            sign = -sign;
        }

        return gameExamples;
    }


    private void eachStepAlphaZero(GameState<ActionType> initialState, Node<ActionType> node, NeuralNetwork<ActionType> nn, int moveCount) {
        Node<ActionType> selectedNode = node;

        // Selection - use PUCT for nodes with children
        while (!selectedNode.state.isTerminal() && !selectedNode.isFullyExplored()) {

            if (!selectedNode.isFullyExpanded()) {
                break;
            }

            ActionType action = selectedNode.selectChildPUCT(c_puct);
            if (action == null) {
                throw new IllegalStateException("null action");
            }

            selectedNode = selectedNode.children.get(action);
            if (selectedNode == null) {
                throw new IllegalStateException("Child node not found");
            }
        }

        double value;

        // Expansion and Evaluation
        if (!selectedNode.state.isTerminal()) {

            // Get neural network evaluation
            int currentPlayer = selectedNode.state.getCurrentPlayer() == 0 ? 1 : -1;
            TrainingExample<ActionType> inputExample = new TrainingExample<>(
                    selectedNode.state.convertBoard(), null, 0, currentPlayer);
            NeuralNetworkOutput<ActionType> nnOutput = nn.predict(inputExample);

            if (!selectedNode.hasChildren()) {
                selectedNode.expandWithPriors(nnOutput.policyHead);
            }

            value = nnOutput.valueHead * -1;

        } else {
            value = selectedNode.state.getReward(1- selectedNode.state.getCurrentPlayer());
        }

        backpropagate(selectedNode, value);
    }

    private void backpropagate(Node<ActionType> node, double reward) {
        while (node != null) {
            node.visitCount += 1;
            node.totalValue += reward;

            // Invert reward for parent's perspective
            reward = -reward; // Flip sign for opponent's perspective

            // Move to parent node
            node = node.parent;
        }
    }

    public ActionType sampleFromDistribution(Map<ActionType, Double> probs, GameState<ActionType> state) {
        List<ActionType> legalActions = state.getLegalActions();
        if (legalActions.isEmpty()) {
            throw new IllegalStateException("No legal actions available");
        }

        // Filter and renormalize probabilities to only include legal actions
        Map<ActionType, Double> legalProbs = new HashMap<>();
        double sum = 0.0;
        for (ActionType action : legalActions) {
            Double prob = probs.getOrDefault(action, 0.0);
            legalProbs.put(action, prob);
            sum += prob;
        }

        // Normalize if sum is not 0, otherwise use uniform distribution
        if (sum == 0.0) {
            double uniformProb = 1.0 / legalActions.size();
            legalProbs.replaceAll((k, v) -> uniformProb);
        } else {
            double finalSum = sum;
            legalProbs.replaceAll((k, v) -> v / finalSum);
        }

        Random random = new Random();
        double r = random.nextDouble();
        double cumSum = 0.0;
        for (Map.Entry<ActionType, Double> entry : legalProbs.entrySet()) {
            cumSum += entry.getValue();
            if (cumSum > r) return entry.getKey();
        }
        return legalActions.get(0); // Floating point precision safety
    }

    public void writeTrainingExamplesToCSV(List<TrainingExample<ActionType>> examples) {
        String filename = "training_samples_" + System.currentTimeMillis() + ".csv";
        try {
            StringBuilder csv = new StringBuilder();
            csv.append("game_position,current_player,value,board_state,policy_probabilities\n");

            for (int i = 0; i < examples.size(); i++) {
                TrainingExample<ActionType> example = examples.get(i);

                StringBuilder boardStr = new StringBuilder();
                for (int[] row : example.board) {
                    for (int cell : row) {
                        boardStr.append(cell).append(",");
                    }
                }

                StringBuilder policyStr = new StringBuilder();
                for (Double prob : example.policy) {
                    policyStr.append(String.format("%.4f", prob)).append(",");
                }

                csv.append(i).append(",")
                   .append(example.current_player).append(",")
                   .append(String.format("%.4f", example.value)).append(",")
                   .append("\"").append(boardStr).append("\"").append(",")
                   .append("\"").append(policyStr).append("\"")
                   .append("\n");
            }

            WriteFile.write(filename, csv.toString());
            System.out.println("\nTraining samples written to: " + filename);
        } catch (Exception e) {
            System.err.println("Error writing training samples to CSV: " + e.getMessage());
        }
    }

    public ActionType getBestAction(Node<ActionType> rootNode) {
        // Print node statistics in grid layout
        System.out.println("\nNode Statistics (Visit Count / Total Value / Average Value):");
        for (int row = 0; row < 3; row++) {
            // Print visit counts
            for (int col = 0; col < 3; col++) {
                int pos = row * 3 + col;
                Node<ActionType> child = rootNode.children.get(pos);
                String stats = child != null ?
                        String.format("%4d", child.visitCount) :
                        "   -";
                System.out.print(stats + " |");
            }
            System.out.println("\n-----------------");
            // Print average values
            for (int col = 0; col < 3; col++) {
                int pos = row * 3 + col;
                Node<ActionType> child = rootNode.children.get(pos);
                String stats = child != null && child.visitCount > 0 ?
                        String.format("%4.2f", child.totalValue / child.visitCount) :
                        "   -";
                System.out.print(stats + " |");
            }
            System.out.println("\n-----------------");
            // Print average values
            for (int col = 0; col < 3; col++) {
                int pos = row * 3 + col;
                Node<ActionType> child = rootNode.children.get(pos);
                String stats = child != null && child.visitCount > 0 ?
                        String.format("%4.2f", child.totalValue) :
                        "   -";
                System.out.print(stats + " |");
            }
            System.out.println("\n-----------------");
            // Print prior probabilities
            for (int col = 0; col < 3; col++) {
                int pos = row * 3 + col;
                Double priorProb = rootNode.priorProbabilities.getOrDefault(pos, 0.0);
                String stats = priorProb != null ?
                        String.format("%4.2f", priorProb) :
                        "   -";
                System.out.print(stats + " |");
            }
            System.out.println("\n=================");
        }

        // Select action with highest visit count
        ActionType bestAction = null;
        Node<ActionType> bestNode = null;
        int maxVisits = -1;
        double bestValue = Double.NEGATIVE_INFINITY;

        for (Map.Entry<ActionType, Node<ActionType>> entry : rootNode.children.entrySet()) {
            int visits = entry.getValue().visitCount;
            double avgValue = visits > 0 ? entry.getValue().totalValue / visits : Double.NEGATIVE_INFINITY;

            // System.out.println(String.format("\nAction %s: %d visits, avg value: %.3f",
            //  entry.getKey(), visits, avgValue));

            if (visits > maxVisits || (visits == maxVisits && avgValue > bestValue)) {
                maxVisits = visits;
                bestAction = entry.getKey();
                bestNode = entry.getValue();
                bestValue = avgValue;
            }
        }

        if (bestNode != null) {
            System.out.println(String.format("\nSelected move %s:", bestAction));
            System.out.println(String.format("Visit count: %d", maxVisits));
            System.out.println(String.format("Total value: %.3f", bestNode.totalValue));
            System.out.println(String.format("Average value: %.3f", bestNode.totalValue / maxVisits));
        }

        return bestAction;
    }
}
