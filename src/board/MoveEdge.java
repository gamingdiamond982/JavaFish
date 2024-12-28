package board;

import java.util.ArrayList;

import static board.Board.*;

public class MoveEdge {
    public BoardEdge[] transformations;
    public boolean unsetWhiteKingside;
    public boolean unsetBlackKingside;

    public boolean unsetWhiteQueenside;
    public boolean unsetBlackQueenside;

    public int newEnpassantSqr = 64;
    public int oldEnpassantSqr;

    public int oldHalfMoveClock;

    public static BoardEdge unset(int square, Board board) {
        return new BoardEdge(square, board.getPiece(square), null);
    }

    public static BoardEdge set(int square, Piece piece, Board board) {
        return new BoardEdge(square, board.getPiece(square), piece);
    }

    public MoveEdge(Move move, Board board) {
        oldHalfMoveClock = board.halfmoveClock;
        Piece mover = board.getPiece(move.fromSquare);
        oldEnpassantSqr = board.enpassantSqr;
        if (move.kingsideCastle) {
            transformations = new BoardEdge[4];
            if (mover.isWhite) {
                transformations[0] = unset(3, board);
                transformations[1] = unset(0, board);
                transformations[2] = set(2, new Piece(Piece.PieceType.ROOK, true), board);
                transformations[3] = (set(1, mover, board));
                unsetWhiteQueenside = true;
                unsetWhiteKingside = true;
            } else {
                transformations[0] = unset(59, board);
                transformations[1] = unset(56, board);
                transformations[2] = set(58, new Piece(Piece.PieceType.ROOK, false), board);
                transformations[3] = set(57, mover, board);
                unsetBlackQueenside = true;
                unsetBlackKingside = true;
            }

            return;
        } else if (move.queensideCastle) {
            transformations = new BoardEdge[4];
            if (mover.isWhite) {
                transformations[0] = unset(7, board);
                transformations[1] = unset(3, board);

                transformations[2] = set(4, new Piece(Piece.PieceType.ROOK, true), board);
                transformations[3] = set(3, mover, board);
                unsetWhiteQueenside = true;
                unsetWhiteKingside = true;
            } else {
                transformations[0] = unset(63, board);
                transformations[1] = unset(56, board);
                transformations[2] = set(57, new Piece(Piece.PieceType.ROOK, false), board);
                transformations[3] = set(58, mover, board);
                unsetBlackQueenside = true;
                unsetBlackKingside = true;
            }
            return;
        }
        int squareAhead = getSquare(getRank(move.fromSquare) + (board.whiteMove ? 1 : -1), getFile(move.fromSquare));
        int twoSquareAhead = getSquare(getRank(move.fromSquare) + (board.whiteMove ? 2 : -2), getFile(move.fromSquare));
        if (mover.type == Piece.PieceType.PAWN && move.toSquare == board.enpassantSqr) {
            transformations = new BoardEdge[3];
            int piece = getSquare(getRank(board.enpassantSqr) + (board.whiteMove ? -1 : 1), getFile(board.enpassantSqr));
            transformations[2] = unset(piece, board);
        } else {
            transformations = new BoardEdge[2];
        }
        if (mover.type == Piece.PieceType.PAWN && move.toSquare == twoSquareAhead) newEnpassantSqr = squareAhead;


        Piece piece = board.getPiece(move.fromSquare);
        transformations[0] = unset(move.fromSquare, board);
        transformations[1] = set(move.toSquare, move.promotionType == null ? piece : new Piece(move.promotionType, mover.isWhite), board);
        if (piece.type == Piece.PieceType.KING) {
            if (piece.isWhite) {
                unsetWhiteKingside = true;
                unsetWhiteQueenside = true;
            } else {
                unsetBlackKingside = true;
                unsetBlackQueenside = true;
            }
        }

        if (piece.type == Piece.PieceType.ROOK) {
            if (move.fromSquare == 0 && piece.isWhite) unsetWhiteKingside = true;
            if (move.fromSquare == 7 && piece.isWhite) unsetWhiteQueenside = true;
            if (move.fromSquare == 56 && !piece.isWhite) unsetWhiteQueenside = true;
            if (move.fromSquare == 63 && !piece.isWhite) unsetWhiteQueenside = true;
        }

        switch (move.toSquare) {
            case 0:
                unsetWhiteKingside = true;
                break;
            case 7:
                unsetWhiteQueenside = true;
                break;
            case 56:
                unsetBlackKingside = true;
                break;
            case 63:
                unsetBlackQueenside = true;
        }

        unsetBlackQueenside = unsetBlackQueenside && board.blackQueenside;
        unsetBlackKingside = unsetBlackKingside && board.blackKingside;


        unsetWhiteKingside = unsetWhiteKingside && board.whiteKingside;
        unsetWhiteQueenside = unsetWhiteQueenside && board.whiteQueenside;

    }
}
