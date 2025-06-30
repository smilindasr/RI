package main;

import java.util.*;

public class MCTS<ActionType extends Comparable> {
    private final double explorationWeight;

    public MCTS() {
        this(Math.sqrt(2));
    }

    public MCTS(double explorationWeight) {
        this.explorationWeight = explorationWeight;
    }

    public Node<ActionType> run(GameState<ActionType> initialState, int iterations) {
        Node<ActionType> root = new Node<>(initialState, null);

        for (int i = 0; i < iterations; i++) {
            if(root.fullyExplored) {
                System.out.println("Exploration done after " + i + " iterations");
                break;
            }

            eachStep(initialState, root);
        }

        return root;
    }


    public void eachStep(GameState<ActionType> initialState, Node<ActionType> node) {
        // Selection
        while (!node.state.isTerminal()) {
            if(node.isFullyExplored()){
                return;
            }
            if (node.isFullyExpanded()) {
                ActionType action = null;

                if(initialState.getLegalActions().size() <=3) {
                    action = node.selectChildUCT(explorationWeight, true);
                } else {
                    action = node.selectChildUCT(explorationWeight, false);
                }

                // Safeguard against null action
                if (action == null) {
                    throw new IllegalStateException("null action");
                }

                // Safeguard against missing child
                Node<ActionType> child = node.children.get(action);
                if (child == null) {
                    throw new IllegalStateException("Child node not found for action: " + action);
                }

                node = child;
            } else {
                // If not fully expanded, break to expansion phase
                break;
            }
        }

        // Expansion
        if (!node.state.isTerminal()) {
            node = node.expand();
            if (node == null) {
                return;
            }
        }

        // Simulation
        double reward = simulate(initialState, node.state);

        backpropagate(node, reward);
    }

    double simulate(GameState<ActionType> initialState, GameState<ActionType> state) {
        GameState<ActionType> currentState = state;
        Random random = new Random();
        int depth = 0;

        int maxDepth = Math.max(state.getMaximumPlays(), 1);

        while (!currentState.isTerminal()) {
            List<ActionType> actions = currentState.getLegalActions();
            // Otherwise choose random move
            currentState = currentState.takeAction(actions.get(random.nextInt(actions.size())));
            depth++;
        }

        double baseReward = currentState.getReward(1 - state.getCurrentPlayer());
        if(initialState.getLegalActions().size() <=5) {
           // System.out.println("Base reward: " + baseReward);
           // currentState.printWinner();
           // System.out.println("node current state: \n" + currentState + "\n\n");
        }
        //return baseReward;
        return calculateReward(baseReward, depth, maxDepth);
    }

    public double calculateReward(double baseReward, int depth, int maxDepth) {
        // Normalize depth between 0 (fastest) and 1 (slowest)
        double normalizedDepth = Math.min(1.0, Math.max(0.0, depth / (double) maxDepth));

        // Win: Exponential decay from 1.0 to 0.2
        if (baseReward == 1.0) {
            return 0.8 * Math.exp(-1.6 * normalizedDepth) + 0.2;
        }
        // Draw: Neutral value
        else if (baseReward == 0.0) {
            return 0.0;
        }
        // Loss: Linear penalty from -1.0 to -0.2
        else {
            return -1.0 + 0.8 * normalizedDepth;
        }
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

    public ActionType getBestAction(Node<ActionType> rootNode) {
        // Print node statistics in grid layout
        System.out.println("\nNode Statistics (Visit Count / Total Value / Average Value / Prior Probability):");
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
            // Print total values
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
