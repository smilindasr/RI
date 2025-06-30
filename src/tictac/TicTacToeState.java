package tictac;

import main.GameState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TicTacToeState implements GameState<Integer> {
    private final char[] board; // 'X', 'O', or ' ' for empty
    private final int currentPlayer; // 0 = X, 1 = O

    public TicTacToeState() {
        this(new char[]{' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' '}, 0);
    }

    private TicTacToeState(char[] board, int currentPlayer) {
        this.board = Arrays.copyOf(board, board.length);
        this.currentPlayer = currentPlayer;
    }

    public char[] getBoard() {
        return board;
    }

    public int[][] convertBoard() {
        int[][] converted = new int[3][3];
        for (int i = 0; i < 9; i++) {
            int row = i / 3;
            int col = i % 3;
            converted[row][col] = switch (board[i]) {
                case 'X' -> 1;
                case 'O' -> -1;
                default -> 0; // handles empty space
            };
        }
        return converted;
    }

    @Override
    public int getMaximumPlays() {
        int initialEmptyCells = 0;
        for (char c : board) {
            if (c == ' ') {
                initialEmptyCells++;
            }
        }
        return initialEmptyCells;
    }

    // GameState interface implementation
    @Override
    public boolean isTerminal() {
        return getWinner() != -1 || isBoardFull();
    }

    @Override
    public List<Integer> getLegalActions() {
        List<Integer> actions = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            if (board[i] == ' ') {
                actions.add(i);
            }
        }
        return actions;
    }

    @Override
    public List<Integer> getAllActions() {
        List<Integer> actions = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
                actions.add(i);
        }
        return actions;
    }

    @Override
    public GameState<Integer> takeAction(Integer action) {
        char[] newBoard = Arrays.copyOf(board, board.length);
        newBoard[action] = (currentPlayer == 0) ? 'X' : 'O';
        return new TicTacToeState(newBoard, 1 - currentPlayer);
    }

    @Override
    public int getCurrentPlayer() {
        return currentPlayer;
    }

    @Override
    public double getReward(int player) {
        int winner = getWinner();
        if (winner == -1) return 0.0; // Draw
        return (winner == player) ? 1.0 : -1.0;
    }

    // Helper methods
    public int getWinner() {
        // Check rows
        for (int i = 0; i < 3; i++) {
            if (checkTriple(i*3, i*3+1, i*3+2)) {
                return board[i*3] == 'X' ? 0 : 1;
            }
        }
        // Check columns
        for (int i = 0; i < 3; i++) {
            if (checkTriple(i, i+3, i+6)) {
                return board[i] == 'X' ? 0 : 1;
            }
        }
        // Check diagonals
        if (checkTriple(0, 4, 8)) return board[0] == 'X' ? 0 : 1;
        if (checkTriple(2, 4, 6)) return board[2] == 'X' ? 0 : 1;
        return -1; // No winner
    }

    @Override
    public void printWinner() {
        if(getWinner() == -1) {
            System.out.println("It's a tie!, Current player: " + getCurrentPlayerName());
        } else if(getWinner() == 0) {
            System.out.println("Winner is X, Current player: " + getCurrentPlayerName());
        } else {
            System.out.println("Winner is O, Current player: " + getCurrentPlayerName());
        }

    }

    @Override
    public void printCurrentPlayer() {
        System.out.println("Current player: " + (currentPlayer == 0 ? "X" : "O") + " (Player " + currentPlayer + ")");
    }

    private String getCurrentPlayerName() {
        return currentPlayer == 0 ? "X": "0";
    }

    private boolean checkTriple(int a, int b, int c) {
        return board[a] != ' ' && board[a] == board[b] && board[a] == board[c];
    }

    private boolean isBoardFull() {
        for (char c : board) {
            if (c == ' ') return false;
        }
        return true;
    }

    // Utility methods for display
    @Override
    public String toString() {
        return
                " " + board[0] + " | " + board[1] + " | " + board[2] + "\n" +
                        "-----------\n" +
                        " " + board[3] + " | " + board[4] + " | " + board[5] + "\n" +
                        "-----------\n" +
                        " " + board[6] + " | " + board[7] + " | " + board[8];
    }
}
