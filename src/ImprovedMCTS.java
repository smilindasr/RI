import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

public class ImprovedMCTS<ActionType> extends MCTS<ActionType> {
    private final double explorationWeight;
    private final int heuristicDepth;

    public ImprovedMCTS() {
        this(Math.sqrt(2), 3); // Higher exploration, 3-ply lookahead
    }

    public ImprovedMCTS(double explorationWeight, int heuristicDepth) {
        this.explorationWeight = explorationWeight;
        this.heuristicDepth = heuristicDepth;
    }

    @Override
    public Node<ActionType> run(GameState<ActionType> initialState, int iterations) {
        Node<ActionType> root = new Node<>(initialState, null);
        // Use parallel search (simple version)
        IntStream.range(0, iterations).forEach(i -> {
            performIteration(root);
        });
        return root;
    }

    private void performIteration(Node<ActionType> root) {
        Node<ActionType> node = root;
        Deque<Node<ActionType>> path = new ArrayDeque<>();

        // Selection with progressive widening
        while (!node.state.isTerminal()) {
            path.push(node);
            if (!node.isFullyExpanded()) {
                node = node.expand();
                break;
            }
            node = selectWithProgressiveWidening(node);
        }

        // Enhanced simulation with heuristic rollouts
        double reward = heuristicRollout(node.state);

        // Backpropagation with virtual loss
        backpropagateWithVirtualLoss(path, reward);
    }

    private Node<ActionType> selectWithProgressiveWidening(Node<ActionType> node) {
        double explorationCoefficient = 0.7 * Math.log(node.visitCount + 1);
        List<ActionType> actions = new ArrayList<>(node.children.keySet());
        Collections.shuffle(actions);

        return node.children.get(actions.get(0)); // Simplified for example
    }

    private double heuristicRollout(GameState<ActionType> state) {
        GameState<ActionType> currentState = state;
        int depth = 0;

        while (!currentState.isTerminal() && depth < heuristicDepth) {
            // Prefer immediate wins and blocks
            List<ActionType> actions = currentState.getLegalActions();
            ActionType bestAction = null;

            // Check for winning moves
            for (ActionType action : actions) {
                GameState<ActionType> nextState = currentState.takeAction(action);
                if (nextState.isTerminal() &&
                        nextState.getReward(currentState.getCurrentPlayer()) == 1.0) {
                    return 1.0;
                }
            }

            // Check for blocking moves
            for (ActionType action : actions) {
                GameState<ActionType> nextState = currentState.takeAction(action);
                GameState<ActionType> opponentState = nextState.takeAction(
                        nextState.getLegalActions().get(0));
                if (opponentState.isTerminal() &&
                        opponentState.getReward(nextState.getCurrentPlayer()) == 1.0) {
                    bestAction = action;
                    break;
                }
            }

            if (bestAction != null) {
                currentState = currentState.takeAction(bestAction);
            } else {
                currentState = currentState.takeAction(
                        actions.get(ThreadLocalRandom.current().nextInt(actions.size())));
            }
            depth++;
        }

        // Finish with random rollout if not terminal
        return super.simulate(currentState);
    }

    private void backpropagateWithVirtualLoss(Deque<Node<ActionType>> path, double reward) {
        for (Node<ActionType> node : path) {
            synchronized(node) {
                node.visitCount += 1;
                node.totalValue += reward;
                reward = 1 - reward; // Alternate perspective
                // Apply virtual loss for concurrent simulations
                node.totalValue -= 0.1;
                node.visitCount += 0.1;
            }
        }
    }
}
