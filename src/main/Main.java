package main;

import tictac.TicTacToeState;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        MCTS<Integer> mcts = new MCTS<>();
        GameState<Integer> currentState = new TicTacToeState();

        // Let player choose their symbol
        System.out.println("Welcome to Tic-Tac-Toe!");
        System.out.println("Would you like to play as X or O?");
        System.out.print("Enter X or O: ");

        boolean humanIsX = false;
        while (true) {
            String choice = scanner.nextLine().toUpperCase();
            if (choice.equals("X")) {
                humanIsX = true;
                System.out.println("\nYou'll play as X (goes first)");
                System.out.println("AI will play as O");
                break;
            } else if (choice.equals("O")) {
                humanIsX = false;
                System.out.println("\nYou'll play as O");
                System.out.println("AI will play as X (goes first)");
                break;
            } else {
                System.out.print("Invalid choice. Please enter X or O: ");
            }
        }

        System.out.println("\nBoard positions:");
        System.out.println(" 0 | 1 | 2 ");
        System.out.println("-----------");
        System.out.println(" 3 | 4 | 5 ");
        System.out.println("-----------");
        System.out.println(" 6 | 7 | 8 ");
        System.out.println("\nLet's begin!\n");

        while (!currentState.isTerminal()) {
            System.out.println(currentState);
            currentState.printCurrentPlayer();

            boolean isHumanTurn = (currentState.getCurrentPlayer() == 0 && humanIsX) ||
                                (currentState.getCurrentPlayer() == 1 && !humanIsX);

            if (!isHumanTurn) { // AI's turn
                System.out.println("\nAI is thinking...");
                Node<Integer> rootNode = mcts.run(currentState, 1000);
                Integer bestMove = mcts.getBestAction(rootNode);
                currentState = currentState.takeAction(bestMove);
                System.out.println("AI plays at position: " + bestMove);
            } else { // Human's turn
                while (true) {
                    System.out.print("\nYour move (0-8): ");
                    String input = scanner.nextLine();

                    try {
                        int position = Integer.parseInt(input);
                        if (position < 0 || position > 8) {
                            System.out.println("Please enter a number between 0-8");
                            continue;
                        }

                        if (!currentState.getLegalActions().contains(position)) {
                            System.out.println("Position already occupied!");
                            continue;
                        }

                        currentState = currentState.takeAction(position);
                        break;
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid input! Please enter a number between 0-8");
                    }
                }
            }
        }

        // Game over - show final state
        System.out.println("\nFinal board:");
        System.out.println(currentState);

        TicTacToeState finalState = (TicTacToeState) currentState;
        int winner = finalState.getWinner();
        if (winner == -1) {
            System.out.println("\nIt's a draw!");
        } else {
            String winnerName;
            if (winner == 0) { // X wins
                winnerName = humanIsX ? "You (X)" : "AI (X)";
            } else { // O wins
                winnerName = humanIsX ? "AI (O)" : "You (O)";
            }
            System.out.println("\n" + winnerName + " wins!");
        }

        scanner.close();
    }
}
