import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        MCTS<Integer> mcts = new MCTS<>();
        GameState<Integer> currentState = new TicTacToeState();

        System.out.println("Tic-Tac-Toe - AI (X) vs Human (O)");
        System.out.println("Board positions:");
        System.out.println(" 0 | 1 | 2 ");
        System.out.println("-----------");
        System.out.println(" 3 | 4 | 5 ");
        System.out.println("-----------");
        System.out.println(" 6 | 7 | 8 ");
        System.out.println("\nLet's begin!\n");

        while (!currentState.isTerminal()) {
            System.out.println(currentState);

            if (currentState.getCurrentPlayer() == 0) { // AI's turn
                System.out.println("\nAI is thinking...");
                Node<Integer> rootNode = mcts.run(currentState, 2000);
                Integer bestMove = MCTS.getBestAction(rootNode);
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
            String winnerName = winner == 0 ? "AI (X)" : "Human (O)";
            System.out.println("\n" + winnerName + " wins!");
        }

        scanner.close();
    }
}
