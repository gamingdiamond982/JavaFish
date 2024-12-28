package board;

public class Move {
    public int fromSquare;
    public int toSquare;
    public Piece.PieceType promotionType;
    public boolean kingsideCastle = false;
    public boolean queensideCastle = false;


    public static Move kingsideCastle(boolean white) {
        Piece king = new Piece(Piece.PieceType.KING, white);
        Move move = white ? new Move(3, 0) : new Move(59, 56);
        move.kingsideCastle = true;
        return move;
    }

    public static Move queensideCastle(boolean white) {
        Piece king = new Piece(Piece.PieceType.KING, white);
        Move move = white ? new Move(3, 7) : new Move(59, 63);
        move.queensideCastle = true;
        return move;
    }

    public Move(int fromSquare, int toSquare) {
        this.fromSquare = fromSquare;
        this.toSquare = toSquare;
        this.promotionType = null;
    }

    public Move(int fromSquare, int toSquare, Piece.PieceType promotionType) {
        this.fromSquare = fromSquare;
        this.toSquare = toSquare;
        this.promotionType = promotionType;
    }


    public String getAlgebra() {
        String promotion = promotionType != null ? switch (promotionType) {
            case BISHOP -> "b";
            case KNIGHT -> "n";
            case ROOK -> "r";
            case QUEEN -> "q";
            default -> "";
        } : "";
        return Board.getAlgebra(this.fromSquare) + Board.getAlgebra(this.toSquare) + promotion;
    }

    public static Move fromAlgebra(String algebra) {
        Piece.PieceType promotion = algebra.length() == 5 ? switch (algebra.charAt(0)) {
            case 'b' -> Piece.PieceType.BISHOP;
            case 'n' -> Piece.PieceType.KNIGHT;
            case 'r' -> Piece.PieceType.ROOK;
            case 'q' -> Piece.PieceType.QUEEN;
            default -> null;
        } : null;

        return new Move(Board.getSquare(algebra.substring(0, 2)), Board.getSquare(algebra.substring(2,4)), promotion);
    }
}
