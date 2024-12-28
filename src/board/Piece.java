package board;

public class Piece {

    public enum PieceType {
        PAWN,
        BISHOP,
        KNIGHT,
        ROOK,
        QUEEN,
        KING,
    }

    public boolean isWhite = true;
    public PieceType type;

    public Piece(PieceType pieceType, boolean white) {
        isWhite = white;
        type = pieceType;
    }
}
