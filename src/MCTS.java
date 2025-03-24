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
        double reward = simulate(initialState, node.state);

        // Backpropagation
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

        double baseReward = currentState.getReward(state.getCurrentPlayer());
        if(initialState.getLegalActions().size() <=5) {
            System.out.println("Base reward: " + baseReward);
            currentState.printWinner();
            System.out.println("node current state: \n" + currentState + "\n\n");
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

    public static <ActionType> ActionType getBestAction(Node<ActionType> rootNode) {
        ActionType bestAction = null;
        Node<ActionType> bestValue = null;
        double minValue = Double.POSITIVE_INFINITY;

        for (Map.Entry<ActionType, Node<ActionType>> entry : rootNode.children.entrySet()) {
            System.out.println("\nAction Value: " + entry.getValue().totalValue);
            System.out.println("\nAction Visits: " + entry.getValue().visitCount);
            double valueVisitRatio = entry.getValue().totalValue / entry.getValue().visitCount;
            System.out.println("Action key: " + entry.getKey());
            if (valueVisitRatio < minValue) {
                minValue = valueVisitRatio;
                bestAction = entry.getKey();
                bestValue = entry.getValue();
            }
        }
        System.out.println("Best value : " + bestValue.totalValue);
        System.out.println("BAction Visit count : " + bestValue.visitCount);
        System.out.println("Best value : " + minValue);
        System.out.println("Best key : " + bestAction);

        return bestAction;
    }

    // Example usage (requires game-specific implementation)

}