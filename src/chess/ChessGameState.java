package chess;

import main.GameState;

import java.util.*;
import java.util.ArrayList;

public class ChessGameState implements GameState<ChessMove> {
    private final ChessBoard board;
    private final int currentPlayer; // 0 = White, 1 = Black
    private final int moveCount;

    public ChessGameState() {
        this.board = new ChessBoard(); // Initialize standard chess board
        this.currentPlayer = 0; // White starts
        this.moveCount = 0;
    }

    private ChessGameState(ChessBoard board, int currentPlayer, int moveCount) {
        this.board = board;
        this.currentPlayer = currentPlayer;
        this.moveCount = moveCount;
    }

    @Override
    public boolean isTerminal() {
        return board.isCheckmate() || board.isStalemate() || board.isDraw();
    }

    @Override
    public List<ChessMove> getLegalActions() {
        return board.getLegalMoves(currentPlayer);
    }

    @Override
    public List<ChessMove> getAllActions() {
        List<ChessMove> allMoves = new ArrayList<>();
        for (int fromRow = 0; fromRow < 8; fromRow++) {
            for (int fromCol = 0; fromCol < 8; fromCol++) {
                for (int toRow = 0; toRow < 8; toRow++) {
                    for (int toCol = 0; toCol < 8; toCol++) {
                        ChessMove move = new ChessMove(fromRow, fromCol, toRow, toCol);
                            allMoves.add(move);
                    }
                }
            }
        }
        return allMoves;
    }

    @Override
    public GameState<ChessMove> takeAction(ChessMove action) {
        ChessBoard newBoard = board.copy();
        newBoard.applyMove(action);
        return new ChessGameState(newBoard, 1 - currentPlayer, moveCount + 1);
    }

    @Override
    public int getCurrentPlayer() {
        return currentPlayer;
    }

    @Override
    public double getReward(int player) {
        if (board.isCheckmate()) {
            // Checkmate: +1 for the winning player, -1 for the losing player
            int winner = board.getWinner();
            return (winner == player) ? 1.0 : -1.0;
        } else if (board.isStalemate() || board.isDraw()) {
            return 0.0; // Draw
        } else {
            throw new IllegalStateException("Non-terminal state has no reward");
        }
    }

    @Override
    public int getMaximumPlays() {
        // Maximum possible moves in a chess game is 5,949 (theoretical maximum)
        return 5949 - moveCount;
    }

    @Override
    public void printWinner() {
        if (board.isCheckmate()) {
            System.out.println("Checkmate! " + (board.getWinner() == 0 ? "White" : "Black") + " wins!");
        } else if (board.isStalemate()) {
            System.out.println("Stalemate! It's a draw.");
        } else if (board.isDraw()) {
            System.out.println("Draw by repetition or 50-move rule.");
        } else {
            System.out.println("Game is still in progress.");
        }
    }

    @Override
    public void printCurrentPlayer() {
        System.out.println("Current player: " + (currentPlayer == 0 ? "White" : "Black") + " (Player " + currentPlayer + ")");
    }

    @Override
    public int[][] convertBoard() {
        return new int[0][];
    }

    @Override
    public String toString() {
        return board.toString();
    }
}
