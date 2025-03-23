import java.util.List;

interface GameState<ActionType> {
    boolean isTerminal();
    List<ActionType> getLegalActions();
    GameState<ActionType> takeAction(ActionType action);
    int getCurrentPlayer();
    double getReward(int player);
    char[] getBoard();
    int getEmptyCells();
    void printWinner();
}