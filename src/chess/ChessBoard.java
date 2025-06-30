package chess;

import java.util.ArrayList;
import java.util.List;

public class ChessBoard {
    private Piece[][] board;
    private boolean whiteToMove;
    private int halfMoveClock; // For 50-move rule
    private int fullMoveNumber;
    private List<String> positionHistory;
    private int[] kingPositions; // [whiteKingRow, whiteKingCol, blackKingRow, blackKingCol]
    private boolean[] castlingRights; // [whiteKingside, whiteQueenside, blackKingside, blackQueenside]
    private int enPassantTarget; // -1 if none, otherwise encoded position

    public ChessBoard() {
        board = new Piece[8][8];
        initializeBoard();
        whiteToMove = true;
        halfMoveClock = 0;
        fullMoveNumber = 1;
        positionHistory = new ArrayList<>();
        kingPositions = new int[4];
        castlingRights = new boolean[]{true, true, true, true};
        enPassantTarget = -1;
    }

    private void initializeBoard() {
        // Set up pawns
        for (int col = 0; col < 8; col++) {
            board[1][col] = new Piece(PieceType.PAWN, 1); // Black pawns
            board[6][col] = new Piece(PieceType.PAWN, 0); // White pawns
        }

        // Set up other pieces
        // Black pieces (top row)
        board[0][0] = new Piece(PieceType.ROOK, 1);
        board[0][1] = new Piece(PieceType.KNIGHT, 1);
        board[0][2] = new Piece(PieceType.BISHOP, 1);
        board[0][3] = new Piece(PieceType.QUEEN, 1);
        board[0][4] = new Piece(PieceType.KING, 1);
        board[0][5] = new Piece(PieceType.BISHOP, 1);
        board[0][6] = new Piece(PieceType.KNIGHT, 1);
        board[0][7] = new Piece(PieceType.ROOK, 1);

        // White pieces (bottom row)
        board[7][0] = new Piece(PieceType.ROOK, 0);
        board[7][1] = new Piece(PieceType.KNIGHT, 0);
        board[7][2] = new Piece(PieceType.BISHOP, 0);
        board[7][3] = new Piece(PieceType.QUEEN, 0);
        board[7][4] = new Piece(PieceType.KING, 0);
        board[7][5] = new Piece(PieceType.BISHOP, 0);
        board[7][6] = new Piece(PieceType.KNIGHT, 0);
        board[7][7] = new Piece(PieceType.ROOK, 0);

        // Initialize king positions
        kingPositions[0] = 7; // White king row
        kingPositions[1] = 4; // White king col
        kingPositions[2] = 0; // Black king row
        kingPositions[3] = 4; // Black king col
    }

    public ChessBoard copy() {
        ChessBoard copy = new ChessBoard();
        copy.whiteToMove = this.whiteToMove;
        copy.halfMoveClock = this.halfMoveClock;
        copy.fullMoveNumber = this.fullMoveNumber;
        copy.enPassantTarget = this.enPassantTarget;
        System.arraycopy(this.kingPositions, 0, copy.kingPositions, 0, 4);
        System.arraycopy(this.castlingRights, 0, copy.castlingRights, 0, 4);

        // Deep copy the board
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                if (this.board[row][col] != null) {
                    copy.board[row][col] = new Piece(
                            this.board[row][col].getType(),
                            this.board[row][col].getPlayer()
                    );
                }
            }
        }

        // Deep copy position history
        copy.positionHistory = new ArrayList<>(this.positionHistory);

        return copy;
    }

    public void applyMove(ChessMove move) {
        Piece movingPiece = board[move.fromRow][move.fromCol];

        // Handle special moves
        if (movingPiece.getType() == PieceType.KING && Math.abs(move.fromCol - move.toCol) == 2) {
            // Castling
            handleCastling(move);
        } else if (movingPiece.getType() == PieceType.PAWN && move.toCol != move.fromCol &&
                board[move.toRow][move.toCol] == null) {
            // En passant capture
            handleEnPassantCapture(move);
        } else if (movingPiece.getType() == PieceType.PAWN &&
                (move.toRow == 0 || move.toRow == 7)) {
            // Pawn promotion (simplified to always promote to queen)
            movingPiece = new Piece(PieceType.QUEEN, movingPiece.getPlayer());
        }

        // Update en passant target
        enPassantTarget = -1;
        if (movingPiece.getType() == PieceType.PAWN && Math.abs(move.fromRow - move.toRow) == 2) {
            enPassantTarget = (move.fromRow + move.toRow) / 2 * 8 + move.fromCol;
        }

        // Update castling rights if rook or king moves
        updateCastlingRights(move, movingPiece);

        // Move the piece
        board[move.toRow][move.toCol] = movingPiece;
        board[move.fromRow][move.fromCol] = null;

        // Update king position if king moved
        if (movingPiece.getType() == PieceType.KING) {
            int kingIndex = movingPiece.getPlayer() == 0 ? 0 : 2;
            kingPositions[kingIndex] = move.toRow;
            kingPositions[kingIndex + 1] = move.toCol;
        }

        // Update move counters
        if (movingPiece.getType() == PieceType.PAWN || board[move.toRow][move.toCol] != null) {
            halfMoveClock = 0;
        } else {
            halfMoveClock++;
        }

        if (!whiteToMove) {
            fullMoveNumber++;
        }

        whiteToMove = !whiteToMove;

        // Add current position to history (simplified FEN)
        positionHistory.add(toFEN());
    }

    private void handleCastling(ChessMove move) {
        int row = move.fromRow;
        int kingCol = move.fromCol;
        int rookCol = move.toCol > kingCol ? 7 : 0;
        int newRookCol = move.toCol > kingCol ? 5 : 3;

        // Move the rook
        board[row][newRookCol] = board[row][rookCol];
        board[row][rookCol] = null;
    }

    private void handleEnPassantCapture(ChessMove move) {
        int capturedPawnRow = move.fromRow; // Same row as moving pawn before move
        board[capturedPawnRow][move.toCol] = null; // Remove the captured pawn
    }

    private void updateCastlingRights(ChessMove move, Piece movingPiece) {
        int player = movingPiece.getPlayer();
        int startIndex = player == 0 ? 0 : 2;

        if (movingPiece.getType() == PieceType.KING) {
            castlingRights[startIndex] = false;
            castlingRights[startIndex + 1] = false;
        } else if (movingPiece.getType() == PieceType.ROOK) {
            if (move.fromRow == (player == 0 ? 7 : 0)) {
                if (move.fromCol == 0) {
                    castlingRights[startIndex + 1] = false; // Queenside
                } else if (move.fromCol == 7) {
                    castlingRights[startIndex] = false; // Kingside
                }
            }
        }

        // If opponent captures a rook, update their castling rights
        Piece capturedPiece = board[move.toRow][move.toCol];
        if (capturedPiece != null && capturedPiece.getType() == PieceType.ROOK) {
            int opponent = 1 - player;
            int oppStartIndex = opponent == 0 ? 0 : 2;
            if (move.toRow == (opponent == 0 ? 7 : 0)) {
                if (move.toCol == 0) {
                    castlingRights[oppStartIndex + 1] = false;
                } else if (move.toCol == 7) {
                    castlingRights[oppStartIndex] = false;
                }
            }
        }
    }

    public List<ChessMove> getLegalMoves(int player) {
        List<ChessMove> moves = new ArrayList<>();

        for (int fromRow = 0; fromRow < 8; fromRow++) {
            for (int fromCol = 0; fromCol < 8; fromCol++) {
                Piece piece = board[fromRow][fromCol];
                if (piece != null && piece.getPlayer() == player) {
                    moves.addAll(getMovesForPiece(fromRow, fromCol, piece));
                }
            }
        }

        // Filter out moves that leave king in check
        moves.removeIf(move -> {
            ChessBoard tempBoard = this.copy();
            tempBoard.applyMove(move);
            return tempBoard.isInCheck(player);
        });

        return moves;
    }

    private List<ChessMove> getMovesForPiece(int row, int col, Piece piece) {
        List<ChessMove> moves = new ArrayList<>();

        switch (piece.getType()) {
            case PAWN:
                addPawnMoves(row, col, piece.getPlayer(), moves);
                break;
            case KNIGHT:
                addKnightMoves(row, col, piece.getPlayer(), moves);
                break;
            case BISHOP:
                addSlidingMoves(row, col, piece.getPlayer(), new int[][]{{1,1}, {1,-1}, {-1,1}, {-1,-1}}, moves);
                break;
            case ROOK:
                addSlidingMoves(row, col, piece.getPlayer(), new int[][]{{1,0}, {-1,0}, {0,1}, {0,-1}}, moves);
                break;
            case QUEEN:
                addSlidingMoves(row, col, piece.getPlayer(),
                        new int[][]{{1,0}, {-1,0}, {0,1}, {0,-1}, {1,1}, {1,-1}, {-1,1}, {-1,-1}}, moves);
                break;
            case KING:
                addKingMoves(row, col, piece.getPlayer(), moves);
                break;
        }

        return moves;
    }

    private void addPawnMoves(int row, int col, int player, List<ChessMove> moves) {
        int direction = player == 0 ? -1 : 1; // White moves up, black moves down

        // Forward move
        if (isValidSquare(row + direction, col) && board[row + direction][col] == null) {
            moves.add(new ChessMove(row, col, row + direction, col));

            // Double move from starting position
            if ((player == 0 && row == 6) || (player == 1 && row == 1)) {
                if (board[row + 2*direction][col] == null) {
                    moves.add(new ChessMove(row, col, row + 2*direction, col));
                }
            }
        }

        // Captures
        for (int colOffset : new int[]{-1, 1}) {
            int newCol = col + colOffset;
            if (newCol >= 0 && newCol < 8) {
                // Normal capture
                if (isValidSquare(row + direction, newCol) &&
                        board[row + direction][newCol] != null &&
                        board[row + direction][newCol].getPlayer() != player) {
                    moves.add(new ChessMove(row, col, row + direction, newCol));
                }

                // En passant
                if (enPassantTarget != -1 &&
                        (row + direction) == enPassantTarget / 8 &&
                        newCol == enPassantTarget % 8) {
                    moves.add(new ChessMove(row, col, row + direction, newCol));
                }
            }
        }
    }

    private void addKnightMoves(int row, int col, int player, List<ChessMove> moves) {
        int[][] knightMoves = {
                {2,1}, {2,-1}, {-2,1}, {-2,-1},
                {1,2}, {1,-2}, {-1,2}, {-1,-2}
        };

        for (int[] move : knightMoves) {
            int newRow = row + move[0];
            int newCol = col + move[1];
            if (isValidSquare(newRow, newCol) &&
                    (board[newRow][newCol] == null || board[newRow][newCol].getPlayer() != player)) {
                moves.add(new ChessMove(row, col, newRow, newCol));
            }
        }
    }

    private void addSlidingMoves(int row, int col, int player, int[][] directions, List<ChessMove> moves) {
        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];

            while (isValidSquare(newRow, newCol)) {
                if (board[newRow][newCol] == null) {
                    moves.add(new ChessMove(row, col, newRow, newCol));
                } else {
                    if (board[newRow][newCol].getPlayer() != player) {
                        moves.add(new ChessMove(row, col, newRow, newCol));
                    }
                    break;
                }
                newRow += dir[0];
                newCol += dir[1];
            }
        }
    }

    private void addKingMoves(int row, int col, int player, List<ChessMove> moves) {
        // Normal king moves
        for (int rowOffset = -1; rowOffset <= 1; rowOffset++) {
            for (int colOffset = -1; colOffset <= 1; colOffset++) {
                if (rowOffset == 0 && colOffset == 0) continue;

                int newRow = row + rowOffset;
                int newCol = col + colOffset;
                if (isValidSquare(newRow, newCol) &&
                        (board[newRow][newCol] == null || board[newRow][newCol].getPlayer() != player)) {
                    moves.add(new ChessMove(row, col, newRow, newCol));
                }
            }
        }

        // Castling
        int castlingIndex = player == 0 ? 0 : 2;
        int backRank = player == 0 ? 7 : 0;

        if (row == backRank && col == 4) { // King on starting position
            // Kingside castling
            if (castlingRights[castlingIndex] &&
                    board[backRank][5] == null &&
                    board[backRank][6] == null &&
                    board[backRank][7] != null &&
                    board[backRank][7].getType() == PieceType.ROOK &&
                    board[backRank][7].getPlayer() == player &&
                    !isInCheck(player) &&
                    !isSquareAttacked(backRank, 5, 1 - player) &&
                    !isSquareAttacked(backRank, 6, 1 - player)) {
                moves.add(new ChessMove(row, col, row, col + 2));
            }

            // Queenside castling
            if (castlingRights[castlingIndex + 1] &&
                    board[backRank][3] == null &&
                    board[backRank][2] == null &&
                    board[backRank][1] == null &&
                    board[backRank][0] != null &&
                    board[backRank][0].getType() == PieceType.ROOK &&
                    board[backRank][0].getPlayer() == player &&
                    !isInCheck(player) &&
                    !isSquareAttacked(backRank, 3, 1 - player) &&
                    !isSquareAttacked(backRank, 2, 1 - player)) {
                moves.add(new ChessMove(row, col, row, col - 2));
            }
        }
    }

    public boolean isInCheck(int player) {
        int kingRow = kingPositions[player == 0 ? 0 : 2];
        int kingCol = kingPositions[player == 0 ? 1 : 3];
        return isSquareAttacked(kingRow, kingCol, 1 - player);
    }

    private boolean isSquareAttacked(int row, int col, int byPlayer) {
        // Check for pawn attacks
        int pawnDirection = byPlayer == 0 ? -1 : 1;
        for (int colOffset : new int[]{-1, 1}) {
            int pawnRow = row - pawnDirection;
            int pawnCol = col + colOffset;
            if (isValidSquare(pawnRow, pawnCol) &&
                    board[pawnRow][pawnCol] != null &&
                    board[pawnRow][pawnCol].getType() == PieceType.PAWN &&
                    board[pawnRow][pawnCol].getPlayer() == byPlayer) {
                return true;
            }
        }

        // Check for knight attacks
        int[][] knightMoves = {
                {2,1}, {2,-1}, {-2,1}, {-2,-1},
                {1,2}, {1,-2}, {-1,2}, {-1,-2}
        };
        for (int[] move : knightMoves) {
            int knightRow = row + move[0];
            int knightCol = col + move[1];
            if (isValidSquare(knightRow, knightCol) &&
                    board[knightRow][knightCol] != null &&
                    board[knightRow][knightCol].getType() == PieceType.KNIGHT &&
                    board[knightRow][knightCol].getPlayer() == byPlayer) {
                return true;
            }
        }

        // Check for sliding pieces (queen, rook, bishop)
        int[][] queenDirections = {
                {1,0}, {-1,0}, {0,1}, {0,-1}, {1,1}, {1,-1}, {-1,1}, {-1,-1}
        };

        for (int[] dir : queenDirections) {
            int r = row + dir[0];
            int c = col + dir[1];
            while (isValidSquare(r, c)) {
                if (board[r][c] != null) {
                    if (board[r][c].getPlayer() == byPlayer) {
                        PieceType type = board[r][c].getType();
                        if ((dir[0] == 0 || dir[1] == 0) &&
                                (type == PieceType.QUEEN || type == PieceType.ROOK)) {
                            return true;
                        }
                        if ((dir[0] != 0 && dir[1] != 0) &&
                                (type == PieceType.QUEEN || type == PieceType.BISHOP)) {
                            return true;
                        }
                    }
                    break;
                }
                r += dir[0];
                c += dir[1];
            }
        }

        // Check for king attacks
        for (int rOffset = -1; rOffset <= 1; rOffset++) {
            for (int cOffset = -1; cOffset <= 1; cOffset++) {
                if (rOffset == 0 && cOffset == 0) continue;
                int kingRow = row + rOffset;
                int kingCol = col + cOffset;
                if (isValidSquare(kingRow, kingCol) &&
                        board[kingRow][kingCol] != null &&
                        board[kingRow][kingCol].getType() == PieceType.KING &&
                        board[kingRow][kingCol].getPlayer() == byPlayer) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean isCheckmate() {
        if (!isInCheck(whiteToMove ? 0 : 1)) {
            return false;
        }
        return getLegalMoves(whiteToMove ? 0 : 1).isEmpty();
    }

    public boolean isStalemate() {
        if (isInCheck(whiteToMove ? 0 : 1)) {
            return false;
        }
        return getLegalMoves(whiteToMove ? 0 : 1).isEmpty();
    }

    public boolean isDraw() {
        // 50-move rule
        if (halfMoveClock >= 50) {
            return true;
        }

        // Threefold repetition
        String currentFEN = toFEN();
        int repetitions = 0;
        for (String position : positionHistory) {
            if (position.equals(currentFEN)) {
                repetitions++;
                if (repetitions >= 2) { // Current position + 2 previous occurrences
                    return true;
                }
            }
        }

        // Insufficient material
        return hasInsufficientMaterial();
    }

    private boolean hasInsufficientMaterial() {
        int whitePieces = 0;
        int blackPieces = 0;
        boolean whiteHasNonKing = false;
        boolean blackHasNonKing = false;
        boolean whiteHasBishopOrKnight = false;
        boolean blackHasBishopOrKnight = false;

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = board[row][col];
                if (piece != null) {
                    if (piece.getPlayer() == 0) {
                        whitePieces++;
                        if (piece.getType() != PieceType.KING) {
                            whiteHasNonKing = true;
                            if (piece.getType() == PieceType.BISHOP || piece.getType() == PieceType.KNIGHT) {
                                whiteHasBishopOrKnight = true;
                            }
                        }
                    } else {
                        blackPieces++;
                        if (piece.getType() != PieceType.KING) {
                            blackHasNonKing = true;
                            if (piece.getType() == PieceType.BISHOP || piece.getType() == PieceType.KNIGHT) {
                                blackHasBishopOrKnight = true;
                            }
                        }
                    }
                }
            }
        }

        // King vs King
        if (!whiteHasNonKing && !blackHasNonKing) {
            return true;
        }

        // King + bishop/knight vs King
        if ((whitePieces == 1 && blackPieces == 2 && blackHasBishopOrKnight) ||
                (blackPieces == 1 && whitePieces == 2 && whiteHasBishopOrKnight)) {
            return true;
        }

        // King + bishop vs King + bishop with bishops on same color
        if (whitePieces == 2 && blackPieces == 2 &&
                whiteHasBishopOrKnight && blackHasBishopOrKnight) {
            // Check if both sides have only a bishop and they're on the same color
            return areOnlyBishopsOnSameColor();
        }

        return false;
    }

    private boolean areOnlyBishopsOnSameColor() {
        Piece whiteBishop = null;
        Piece blackBishop = null;

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = board[row][col];
                if (piece != null && piece.getType() == PieceType.BISHOP) {
                    if (piece.getPlayer() == 0) {
                        whiteBishop = piece;
                    } else {
                        blackBishop = piece;
                    }
                } else if (piece != null && piece.getType() != PieceType.KING) {
                    return false; // Found a non-bishop, non-king piece
                }
            }
        }

        if (whiteBishop == null || blackBishop == null) {
            return false;
        }

        // Find positions of the bishops
        int whiteBishopRow = -1, whiteBishopCol = -1;
        int blackBishopRow = -1, blackBishopCol = -1;

        outer:
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = board[row][col];
                if (piece == whiteBishop) {
                    whiteBishopRow = row;
                    whiteBishopCol = col;
                } else if (piece == blackBishop) {
                    blackBishopRow = row;
                    blackBishopCol = col;
                }

                if (whiteBishopRow != -1 && blackBishopRow != -1) {
                    break outer;
                }
            }
        }

        // Check if bishops are on the same color
        return (whiteBishopRow + whiteBishopCol) % 2 == (blackBishopRow + blackBishopCol) % 2;
    }

    public int getWinner() {
        if (isCheckmate()) {
            return whiteToMove ? 1 : 0; // The player who just moved won
        }
        return -1; // No winner yet
    }

    private boolean isValidSquare(int row, int col) {
        return row >= 0 && row < 8 && col >= 0 && col < 8;
    }

    public String toFEN() {
        // Simplified FEN representation for position history
        StringBuilder fen = new StringBuilder();

        // Piece placement
        for (int row = 0; row < 8; row++) {
            int emptyCount = 0;
            for (int col = 0; col < 8; col++) {
                Piece piece = board[row][col];
                if (piece == null) {
                    emptyCount++;
                } else {
                    if (emptyCount > 0) {
                        fen.append(emptyCount);
                        emptyCount = 0;
                    }
                    char c = piece.getType().toString().charAt(0);
                    if (piece.getPlayer() == 1) {
                        c = Character.toLowerCase(c);
                    }
                    fen.append(c);
                }
            }
            if (emptyCount > 0) {
                fen.append(emptyCount);
            }
            if (row < 7) {
                fen.append('/');
            }
        }

        // Active color
        fen.append(' ').append(whiteToMove ? 'w' : 'b');

        // Castling availability
        fen.append(' ');
        boolean anyCastling = false;
        if (castlingRights[0]) { fen.append('K'); anyCastling = true; }
        if (castlingRights[1]) { fen.append('Q'); anyCastling = true; }
        if (castlingRights[2]) { fen.append('k'); anyCastling = true; }
        if (castlingRights[3]) { fen.append('q'); anyCastling = true; }
        if (!anyCastling) fen.append('-');

        // En passant
        fen.append(' ');
        if (enPassantTarget != -1) {
            int col = enPassantTarget % 8;
            int row = enPassantTarget / 8;
            fen.append((char)('a' + col)).append(8 - row);
        } else {
            fen.append('-');
        }

        // Halfmove clock and fullmove number
        fen.append(' ').append(halfMoveClock).append(' ').append(fullMoveNumber);

        return fen.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("  a b c d e f g h\n");
        for (int row = 0; row < 8; row++) {
            sb.append(8 - row).append(" ");
            for (int col = 0; col < 8; col++) {
                Piece piece = board[row][col];
                if (piece == null) {
                    sb.append(". ");
                } else {
                    char c = piece.getType().toString().charAt(0);
                    if (piece.getPlayer() == 1) {
                        c = Character.toLowerCase(c);
                    }
                    sb.append(c).append(" ");
                }
            }
            sb.append(8 - row).append("\n");
        }
        sb.append("  a b c d e f g h\n");
        sb.append(whiteToMove ? "White" : "Black").append(" to move\n");
        return sb.toString();
    }
}