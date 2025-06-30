package main;

import java.util.*;
import java.util.stream.Collectors;

public class Node<ActionType extends Comparable> {
    GameState<ActionType> state;
    Node<ActionType> parent;
    Map<ActionType, Node<ActionType>> children;
    Map<ActionType, Double> priorProbabilities;
    int visitCount;
    double totalValue;
    private static final int VISIT_THRESHOLD = 10;  // Minimum visits before considering a node fully explored
    boolean fullyExplored;

    public Node(GameState<ActionType> state, Node<ActionType> parent) {
        this.state = state;
        this.parent = parent;
        this.children = new HashMap<>();
        this.visitCount = 0;
        this.priorProbabilities = new HashMap<>();
        this.totalValue = 0.0;
    }

    public boolean isFullyExpanded() {
        // Ensure legal actions are available
        if (state.getLegalActions().isEmpty()) {
            return true; // Terminal state is considered fully expanded
        }

        // Compare number of children to legal actions
        return children.size() >= state.getLegalActions().size();
    }

    public boolean isFullyExplored(){
        return this.fullyExplored;
    }

    public ActionType selectChildUCT(double explorationWeight, boolean print) {
        double bestScore = Double.NEGATIVE_INFINITY;
        ActionType bestAction = null;


        for (Map.Entry<ActionType, Node<ActionType>> entry : children.entrySet()) {

            Node<ActionType> child = entry.getValue();
            if(child.isFullyExplored()) {
                continue;
            }
            double score;

            if (child.visitCount == 0) {
                score = Double.POSITIVE_INFINITY;
            } else {
                double exploitation = child.totalValue / child.visitCount;
                double exploration = explorationWeight *
                        Math.sqrt(Math.log(this.visitCount) / child.visitCount);
                // Normalize exploitation to [0,1] range and balance with exploration
                double normalizedExploitation = (exploitation + 1.0) / 2.0;
                score = normalizedExploitation + exploration;
            }

            if (score > bestScore) {
                bestScore = score;
                bestAction = entry.getKey();
            }
        }
        return bestAction;
    }

    public ActionType selectChildPUCT(double c_puct) {
        double bestScore = Double.NEGATIVE_INFINITY;
        ActionType bestAction = null;

        for (Map.Entry<ActionType, Node<ActionType>> entry : children.entrySet()) {
            ActionType action = entry.getKey();
            Node<ActionType> child = entry.getValue();

            if (child.isFullyExplored()) {
                continue;
            }

            // Get prior probability for this action
            double prior = priorProbabilities.getOrDefault(action, 0.0);

            // Calculate PUCT score
            double score;
            if (child.visitCount == 0) {
                score = Double.POSITIVE_INFINITY;
            } else {
                // Q-value (exploitation term)
                double qValue = child.totalValue / child.visitCount;

                // U-value (exploration term using prior probability)
                double uValue = c_puct * prior * Math.sqrt(Math.log(this.visitCount + 1)) / (1 + child.visitCount);

                // Combine Q and U values
                score = qValue + uValue;
            }

            if (score > bestScore) {
                bestScore = score;
                bestAction = action;
            }
        }

        return bestAction;
    }


    public Node<ActionType> expand() {
        for (ActionType action : state.getLegalActions()) {
            if (!children.containsKey(action)) {
                GameState<ActionType> newState = state.takeAction(action);
                Node<ActionType> newChild = new Node<>(newState, this);
                children.put(action, newChild);
                newChild.setFullyExplored();
                return newChild;
            }
        }
        return null;
    }

    public void expandWithPriors(Map<ActionType, Double> policy) {
        // Store prior probabilities
        this.priorProbabilities.putAll(policy);

        // Create child nodes for all legal actions
        for (ActionType action : state.getLegalActions()) {
            if (!children.containsKey(action)) {
                GameState<ActionType> newState = state.takeAction(action);
                Node<ActionType> newChild = new Node<>(newState, this);
                children.put(action, newChild);
            }
        }
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }

    public void setFullyExplored() {
        if(state.isTerminal() && visitCount >= VISIT_THRESHOLD){
            this.fullyExplored = true;
            if(parent != null) {
                parent.setFullyExplored();
            }
        } else {
            int exploredCount = 0;
            for(Node<ActionType> child : children.values()){
                if(child.fullyExplored){
                    exploredCount++;
                }
            }
            if(!children.isEmpty() && exploredCount == children.size() && visitCount >= VISIT_THRESHOLD){
                this.fullyExplored = true;
                Node<ActionType> parent = this.parent;
                if(parent != null) {
                    parent.setFullyExplored();
                }
            }
        }
    }

    /**
     * Returns a probability distribution over actions based on visit counts.
     * This is typically used for actual gameplay after the search is complete.
     * The distribution can be used to either select the most visited action
     * or sample from the distribution for more diverse gameplay.
     *
     * @param temperature Controls the sharpness of the distribution:
     *                    - temperature → 0: Converges to selecting the most visited action
     *                    - temperature = 1: Standard visit count distribution
     *                    - temperature → ∞: Converges to uniform distribution
     * @return Map of actions to their probabilities based on visit counts, sorted by probability in descending order
     */
    public Map<ActionType, Double> getVisitDistribution(double temperature) {
        Map<ActionType, Double> distribution = getDistribution(temperature);

        // Sort by probabilities in descending order and return as LinkedHashMap to maintain order
        return distribution.entrySet()
                .stream()
                .sorted(Map.Entry.<ActionType, Double>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }


    public List<Double> getVisitDistributionSorted(double temperature) {
        Map<ActionType, Double> distribution = getDistribution(temperature);

        // Sort by probabilities in descending order and return as LinkedHashMap to maintain order
        return distribution.entrySet().stream()
                .sorted((e1, e2) -> {
                    try {
                        return ((Comparable<ActionType>) e1.getKey()).compareTo(e2.getKey());
                    } catch (ClassCastException e) {
                        throw new ClassCastException("ActionType must implement Comparable for sorting by action");
                    }
                })
                .map((entry) -> entry.getValue())
                .collect(Collectors.toList());
    }

    private Map<ActionType, Double> getDistribution(double temperature) {
        Map<ActionType, Double> distribution = new HashMap<>();
        double sum = 0.0;
        List<ActionType> legalActions = state.getLegalActions();

        // Initialize all legal actions with zero probability
        for (ActionType action : state.getAllActions()) {
            distribution.put(action, 0.0);
        }

        // Calculate visit count based distribution with temperature
        for (Map.Entry<ActionType, Node<ActionType>> entry : children.entrySet()) {
            if (legalActions.contains(entry.getKey())) {
                Node<ActionType> child = entry.getValue();
                if (child.visitCount > 0) {
                    // Apply temperature to visit counts: visits^(1/temperature)
                    double temperedVisits = Math.pow(child.visitCount, 1.0 / temperature);
                    distribution.put(entry.getKey(), temperedVisits);
                    sum += temperedVisits;
                }
            }
        }

        // Normalize to get probabilities
        if (sum > 0) {
            double finalSum = sum;
            distribution.replaceAll((k, v) -> v / finalSum);
        } else {
            // If no visits, return uniform distribution over legal actions
            double uniformProb = 1.0 / legalActions.size();
            for (ActionType action : legalActions) {
                distribution.put(action, uniformProb);
            }
        }

        return distribution;
    }
    /**
     * Expands the current node using a policy from a neural network.
     * This method is used in AlphaZero/MuZero style MCTS where the expansion phase
     * is guided by a learned policy rather than uniform random expansion.
     *
     * @return The newly created child node, or null if no expansion was possible
     * @throws IllegalStateException if policy contains invalid moves or probabilities
     */
    public Node<ActionType> expandWithPolicy(Map<ActionType, Double> policy) {
        // Get legal actions for current state
        List<ActionType> legalActions = state.getLegalActions();

        // If no legal actions or node is terminal, return null
        if (legalActions.isEmpty() || state.isTerminal()) {
            return null;
        }

        // Validate and normalize the policy for legal actions
        Map<ActionType, Double> normalizedPolicy = new HashMap<>();
        double policySum = 0.0;

        // Only consider legal actions from the policy
        for (ActionType action : legalActions) {
            Double probability = policy.getOrDefault(action, 0.0);
            if (probability < 0.0 || probability > 1.0) {
                throw new IllegalStateException("Invalid probability in policy for action: " + action);
            }
            normalizedPolicy.put(action, probability);
            policySum += probability;
        }

        // If policy sum is 0, use uniform distribution over legal actions
        if (policySum == 0.0) {
            double uniformProb = 1.0 / legalActions.size();
            legalActions.forEach(action -> normalizedPolicy.put(action, uniformProb));
        } else {
            // Normalize probabilities to sum to 1
            double finalPolicySum = policySum;
            normalizedPolicy.replaceAll((k, v) -> v / finalPolicySum);
        }

        // Select an unexplored action with highest policy probability
        return legalActions.stream()
               // .filter(action -> !children.containsKey(action))
                .max(Comparator.comparingDouble(normalizedPolicy::get))
                .map(action -> {
                    GameState<ActionType> newState = state.takeAction(action);
                    Node<ActionType> newChild = new Node<>(newState, this);
                    children.put(action, newChild);
                    return newChild;
                })
                .orElse(null);
    }
}
