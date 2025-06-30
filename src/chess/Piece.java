package chess;

public class Piece {
    private final PieceType type;
    private final int player; // 0 = White, 1 = Black

    public Piece(PieceType type, int player) {
        this.type = type;
        this.player = player;
    }

    @Override
    public String toString() {
        return type.toString() + (player == 0 ? "W" : "B");
    }

    public PieceType getType() {
        return type;
    }

    public int getPlayer() {
        return player;
    }
}