import java.util.HashMap;
import java.util.Map;

class Node<ActionType> {
    GameState<ActionType> state;
    Node<ActionType> parent;
    Map<ActionType, Node<ActionType>> children;
    int visitCount;
    double totalValue;
    boolean fullyExplored;

    public Node(GameState<ActionType> state, Node<ActionType> parent) {
        this.state = state;
        this.parent = parent;
        this.children = new HashMap<>();
        this.visitCount = 0;
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

    public ActionType selectChild(double explorationWeight, boolean print) {
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
                double exploitation = -1 * child.totalValue/ child.visitCount;
                double exploration = explorationWeight *
                        Math.sqrt(Math.log(this.visitCount) / child.visitCount);
                score = exploitation + exploration;
                /*if( print) {
                    System.out.println("child.totalValue " + child.totalValue + "\nchild.visitCount " + child.visitCount +
                            "\nexploitation " + exploitation
                    + "\nexploration " + exploration
                    + "\nscore " + score);

                    System.out.println("this state: \n" + this.state);
                    System.out.println("child state: \n" + child.state + "\n\n");

                }*/
            }

            if (score > bestScore) {
                bestScore = score;
                bestAction = entry.getKey();
            }
        }
        if(print){
            System.out.println("*****************");
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

    public void setFullyExplored() {
        if(state.isTerminal()){
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
            if(!children.isEmpty() && exploredCount == children.size()){
                this.fullyExplored = true;
                Node<ActionType> parent = this.parent;
                if(parent != null) {
                    parent.setFullyExplored();
                }
            }
        }
    }
}
