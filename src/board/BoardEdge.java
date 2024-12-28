package board;

public class BoardEdge {
    public int targetSquare;
    public Piece fromPiece;
    public Piece toPiece;

    public BoardEdge(int targetSquare, Piece fromPiece, Piece toPiece) {
        this.targetSquare = targetSquare;
        this.fromPiece = fromPiece;
        this.toPiece = toPiece;
    }
}
