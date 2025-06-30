package main;

import java.util.List;

public class TrainingExample<ActionType> {
    public int[][] board;
    public List<Double> policy;
    public double value;
    public int current_player;

    public TrainingExample(int[][] board, List<Double> policy, double value, int current_player) {
        this.board = board;
        this.policy = policy;
        this.value = value;
        this.current_player = current_player;
    }

    public void printInGridLayout() {
        System.out.println("\nTraining Example:");
        System.out.println("Current Player: " + (current_player == 0 ? "X" : "O"));
        System.out.println("Value: " + value);

        // Print board
        System.out.println("\nBoard State:");
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                String symbol = switch (board[i][j]) {
                    case 1 -> " X ";
                    case -1 -> " O ";
                    default -> " - ";
                };
                System.out.print(symbol + "|");
            }
            System.out.println("\n-----------");
        }

        // Print policy probabilities in grid layout
        System.out.println("\nPolicy Probabilities:");
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                int index = i * 3 + j;
                if (index < policy.size()) {
                    System.out.printf("%4.1f|", policy.get(index) * 100); // Show as percentage
                } else {
                    System.out.print("  - |");
                }
            }
            System.out.println("\n-----------");
        }
        System.out.println(); // Extra line for spacing
    }
}
