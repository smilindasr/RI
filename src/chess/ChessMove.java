package chess;

public class ChessMove {
    final int fromRow;
    final int fromCol;
    final int toRow;
    final int toCol;

    public ChessMove(int fromRow, int fromCol, int toRow, int toCol) {
        this.fromRow = fromRow;
        this.fromCol = fromCol;
        this.toRow = toRow;
        this.toCol = toCol;
    }

    @Override
    public String toString() {
        return String.format("(%d,%d)->(%d,%d)", fromRow, fromCol, toRow, toCol);
    }
}
