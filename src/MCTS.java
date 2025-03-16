import java.util.*;

public class MCTS<ActionType> {
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

    private void eachStep(GameState<ActionType> initialState, Node<ActionType> node) {
        // Selection
        while (!node.state.isTerminal()) {
            if(node.isFullyExplored()){
                return;
            }
            if (node.isFullyExpanded()) {
                ActionType action = null;

                if(initialState.getLegalActions().size() <=3) {
                    action = node.selectChild(explorationWeight, true);
                } else {
                    action = node.selectChild(explorationWeight, false);
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
                // If not fully expanded, beak to expansion phase
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
        double reward = simulate(node.state);

        // Backpropagation
        backpropagate(node, reward);
    }

    double simulate(GameState<ActionType> state) {
        GameState<ActionType> currentState = state;
        Random random = new Random();
        int depth = 0;
        char[] board = currentState.getBoard();
        int initialEmptyCells = 0;
        for (char c : board) {
            if (c == ' ') {
                initialEmptyCells++;
            }
        }
        int maxDepth = Math.max(initialEmptyCells, 1);

        while (!currentState.isTerminal()) {
            List<ActionType> actions = currentState.getLegalActions();
            // Otherwise choose random move
            currentState = currentState.takeAction(actions.get(random.nextInt(actions.size())));
            depth++;
        }
        double baseReward = currentState.getReward(state.getCurrentPlayer());
        //return baseReward;
        return calculateDepthWeightedReward(baseReward, depth, maxDepth);
    }

    private double calculateDepthWeightedReward(double baseReward, int depth, int maxDepth) {
        // Ensure depth is within valid range
        depth = Math.max(0, Math.min(depth, maxDepth));

        // Calculate normalized depth (0 = immediate, 1 = max depth)
        double normalizedDepth = (double) depth / maxDepth;

        // Weight rewards based on depth
        if (baseReward == 1.0) { // Win
            // Immediate wins are better (reward decreases with depth)
            return 1.0 - 0.5 * normalizedDepth;
        } else if (baseReward == 0.0) { // Loss
            // Later losses are less bad (reward increases with depth)
            return 0.5 * normalizedDepth;
        } else { // Draw
            // Neutral reward for draws
            return 0.5;
        }
    }

    private void backpropagate(Node<ActionType> node, double reward) {
        // Add small epsilon to prevent division by zero
        double epsilon = 1e-8;

        while (node != null) {
            // Update visit count
            node.visitCount += 1;

            // Update total value
            node.totalValue += reward;

            // Invert reward for parent's perspective
            reward = 1 - reward;

            // Add epsilon to prevent NaN from 0/0
            reward = Math.max(0.0, Math.min(1.0, reward)) + epsilon;

            // Move to parent node
            node = node.parent;
        }

    }

    public static <ActionType> ActionType getBestAction(Node<ActionType> rootNode) {
        ActionType bestAction = null;
        Node<ActionType> bestValue = null;
        double maxValue = -1;

        for (Map.Entry<ActionType, Node<ActionType>> entry : rootNode.children.entrySet()) {
            System.out.println("\nAction Value: " + entry.getValue().totalValue);
            System.out.println("\nAction Visits: " + entry.getValue().visitCount);
            System.out.println("Action key: " + entry.getKey());
            if (entry.getValue().totalValue > maxValue) {
                maxValue = entry.getValue().totalValue;
                bestAction = entry.getKey();
                bestValue = entry.getValue();
            }
        }
        System.out.println("Best value : " + bestValue.totalValue);
        System.out.println("BAction Visit count : " + bestValue.visitCount);
        System.out.println("Best key : " + bestAction);
        return bestAction;
    }

    // Example usage (requires game-specific implementation)

}