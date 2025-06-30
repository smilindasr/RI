package main;

import java.util.List;

public interface GameState<ActionType> {
    boolean isTerminal();
    List<ActionType> getLegalActions();
    List<ActionType> getAllActions();
    GameState<ActionType> takeAction(ActionType action);
    int getCurrentPlayer();
    double getReward(int player);
    int getMaximumPlays(); // in the case of tic-tac-toe, this is the number of empty cells
    void printWinner();
    int[][] convertBoard();

    /**
     * Prints a human-readable representation of the current player
     * For example: "Current player: X (Player 0)" or "Current player: White (Player 0)"
     */
    void printCurrentPlayer();
}
