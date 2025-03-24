import java.util.List;

interface GameState<ActionType> {
    boolean isTerminal();
    List<ActionType> getLegalActions();
    GameState<ActionType> takeAction(ActionType action);
    int getCurrentPlayer();
    double getReward(int player);
    int getMaximumPlays(); // in the case of tic-tac-toe, this is the number of empty cells
    void printWinner();
}