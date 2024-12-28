package board;
/*
    |
   \|/ MSB
8 | 0 0 0 0 0 0 0 0
7 | 0 1 0 0 0 0 0 0
6 | 0 0 1 0 0 0 1 0
5 | 0 0 0 1 0 1 0 0
4 | 0 0 0 0 B 0 0 0
3 | 0 0 0 1 0 1 0 0
2 | 0 0 1 0 0 0 1 0
1 | 0 0 0 0 0 0 0 0 <- LSB
    ---------------
    a b c d e f g h
 */



import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Board {

    private final Magics MAGICS = new Magics();
    private final long[] BISHOP_MASKS = new long[64];
    private final long[] ROOK_MASKS = new long[64];
    private final long[] KING_MASKS = new long[64];
    private final long[] KNIGHT_MASKS = new long[64];
    private final long[][] PAWN_ATTACK_MASKS = new long[2][64];
    private final long[][] BISHOP_LOOKUPS = new long[64][512];
    private final long[][] ROOK_LOOKUPS = new long[64][4096];
    public final int[] PIECE_VALUES = new int[] {1, 3, 3, 5, 9, Integer.MAX_VALUE};

    public GameState getGameState() {
        return gameState;
    }

    public enum GameState {
        WHITE_WIN,
        DRAW,
        BLACK_WIN,
        ONGOING,
    }

    private GameState gameState = GameState.ONGOING;
    private ArrayList<MoveEdge> MOVES = new ArrayList<MoveEdge>();

    public int enpassantSqr;

    public boolean whiteKingside = false;
    public boolean blackKingside = false;
    public boolean whiteQueenside = false;
    public boolean blackQueenside = false;


    public long[] whiteBitboards = new long[6];
    public long[] blackBitboards = new long[6];



    public boolean whiteMove = true;
    public int halfmoveClock = 0;
    public int fullmoveClock = 0;




    public Board() {
        this.generateBishopMasks();
        this.generateRookMasks();
        this.generateKingMasks();
        this.generateKnightMasks();
        this.generatePawnAttackMasks();
        this.initBishopLookups();
        this.initRookLookups();
    }


    public static String getAlgebra(int square) {
        char[] result = new char[2];
        result[0] = (char) (104 - getFile(square));
        result[1] = (char) (getRank(square) + '1');
        return new String(result);
    }

    public static int getSquare(String algebra) {
        assert algebra.length() == 2;
        return getSquare(algebra.charAt(1) - '1' , 104 - algebra.charAt(0));
    }

    public Piece getPiece(int square) {
        long mask = 1L << square;
        for (Piece.PieceType type : Piece.PieceType.values()) {
            long bitboard = whiteBitboards[type.ordinal()];

            if ((bitboard & mask) != 0) {
                return new Piece(type, true);
            }
            bitboard = blackBitboards[type.ordinal()];
            if ((bitboard & mask) != 0) {
                return new Piece(type, false);
            }
        }
        return null;
    }


    public String drawBoard() {
        StringBuilder result = new StringBuilder();

        for (int square = 63; square >= 0; square--) {
            if (square % 8 == 7) {
                result.append("\n").append(String.valueOf(getRank(square))).append("| ");
            }
            Piece piece = getPiece(square);
            if (piece == null) {
                result.append(" ");
            } else {
                char next = ' ';
                switch (piece.type) {
                    case PAWN -> next = 'p';
                    case BISHOP -> next = 'b';
                    case KNIGHT -> next = 'n';
                    case ROOK -> next = 'r';
                    case QUEEN -> next = 'q';
                    case KING -> next = 'k';
                }
                if (piece.isWhite) {
                    next = Character.toUpperCase(next);
                }
                result.append(next);
            }
        }
        result.append("\n | abcdefgh");
        return result.toString();
    }

    public void setPosition(String fen) {
        String[] parts = fen.split(" ", 6);
        String pieces = parts[0].replace("/", "");
        int square = 63;
        for (int i = 0; i < pieces.length(); i++) {
            char c = pieces.charAt(i);
            boolean white = Character.isUpperCase(c);
            c = Character.toLowerCase(c);
            Piece.PieceType piece = null;
            switch (c) {
                case 'p':
                    piece = Piece.PieceType.PAWN;
                    break;
                case 'b':
                    piece = Piece.PieceType.BISHOP;
                    break;
                case 'n':
                    piece = Piece.PieceType.KNIGHT;
                    break;
                case 'r':
                    piece = Piece.PieceType.ROOK;
                    break;
                case 'q':
                    piece = Piece.PieceType.QUEEN;
                    break;
                case 'k':
                    piece = Piece.PieceType.KING;
                    break;
                default:
                    assert Character.isDigit(c);
                    square -= Character.getNumericValue(c);
            }

            if (piece != null) {
                this.setSquare(square, piece, white);
                square -= 1;
            }
        }
        this.whiteMove = parts[1].equals("w");
        String castleStr = parts[2];
        for (int i = 0; i < castleStr.length(); i++) {
            switch (castleStr.charAt(i)) {
                case 'k':
                    blackKingside = true;
                    break;
                case 'q':
                    blackQueenside = true;
                    break;
                case 'K':
                    whiteKingside = true;
                    break;
                case 'Q':
                    whiteQueenside = true;
                    break;
            }
        }
        if (parts[3].equals("-")) {
            this.enpassantSqr = 64;
        } else {
            this.enpassantSqr = getSquare(parts[3]);
        }
        this.halfmoveClock = Integer.parseInt(parts[4]);
        this.fullmoveClock = Integer.parseInt(parts[5]);
        this.MOVES = new ArrayList<>();
    }

    public void unsetSquare(int square) {
        long mask = 1L << square;
        for (int i = 0; i < 6; i++) {
            whiteBitboards[i] &= ~mask;
            blackBitboards[i] &= ~mask;
        }
    }

    public void setSquare(int square, Piece piece) {
        if (piece != null) {
            setSquare(square, piece.type, piece.isWhite);
        } else {
            unsetSquare(square);
        }
    }

    public void setSquare(int square, Piece.PieceType piece, boolean whiteMove) {
        unsetSquare(square);
        long mask = 1L << square;
        if (whiteMove) {
            whiteBitboards[piece.ordinal()] |= mask;
        } else {
            blackBitboards[piece.ordinal()] |= mask;
        }
    }


    public static int getFile(int square) {
        return square % 8;
    }

    public static int getRank(int square) {
        return square / 8;
    }

    public static int getSquare(int rank, int file) {
        return rank * 8 + file;
    }

    public long getPieces(boolean white) {
        long result = 0;
        for (int i = 0; i < 6; i++) {
            result |= white ? this.whiteBitboards[i] : this.blackBitboards[i];
        }
        return result;
    }

    public long allPieces() {
        return getPieces(true) | getPieces(false);
    }

    private void generatePawnAttackMasks() {
        for (int square = 0; square < 64; square++) {
            long mask = 0L;
            int rank = getRank(square);
            int file = getFile(square);

            if (rank + 1 <= 7 && file - 1 >= 0) {
                mask |= 1L << getSquare(rank + 1, file - 1);
            }

            if (rank + 1 <= 7 && file + 1 <= 7) {
                mask |= 1L << getSquare(rank + 1, file + 1);
            }

            PAWN_ATTACK_MASKS[1][square] = mask;
            mask = 0L;
            if (rank - 1 >= 0 && file - 1 >= 0) {
                mask |= 1L << getSquare(rank - 1, file - 1);
            }

            if (rank - 1 >= 0 && file + 1 <= 7) {
                mask |= 1L << getSquare(rank - 1, file + 1);
            }
            PAWN_ATTACK_MASKS[0][square] = mask;
        }
    }

    private void generateKnightMasks() {
        for (int square = 0; square < 64; square++) {
            long mask = 0L;
            int rank = getRank(square);
            int file = getFile(square);

            if (rank + 2 <= 7 && file - 1 >= 0) {
                mask |= 1L << getSquare(rank + 2, file - 1);
            }

            if (rank + 2 <= 7 && file + 1 <= 7) {
                mask |= 1L << getSquare(rank + 2, file + 1);
            }

            if (rank + 1 <= 7 && file + 2 <= 7) {
                mask |= 1L << getSquare(rank + 1, file + 2);
            }

            if (rank - 1 >= 0 && file + 2 <= 7) {
                mask |= 1L << getSquare(rank - 1, file + 2);
            }

            if (rank - 2 >= 0 && file + 1 <= 7) {
                mask |= 1L << getSquare(rank - 2, file + 1);
            }

            if (rank - 2 >= 0 && file - 1 >= 0) {
                mask |= 1L << getSquare(rank - 2, file - 1);
            }

            if (rank - 1 >= 0 && file - 2 >= 0) {
                mask |= 1L << getSquare(rank - 1, file - 2);
            }

            if (rank + 1 <= 7 && file - 2 >= 0) {
                mask |= 1L << getSquare(rank + 1, file - 2);
            }

            KNIGHT_MASKS[square] = mask;
        }
    }

    private void generateKingMasks() {
        for (int square = 0; square < 64; square++) {
            long mask = 0L;
            int rank = getRank(square);
            int file = getFile(square);

            if (rank + 1 <= 7) {
                mask |= 1L << getSquare(rank + 1, file);
            }

            if (rank + 1 <= 7 && file + 1 <= 7) {
                mask |= 1L << getSquare(rank + 1, file + 1);
            }

            if (rank + 1 <= 7 && file - 1 >= 0) {
                mask |= 1L << getSquare(rank + 1, file - 1);
            }

            if (file - 1 >= 0) {
                mask |= 1L << getSquare(rank, file - 1);
            }

            if (file - 1 >= 0 && rank - 1 >= 0) {
                mask |= 1L << getSquare(rank - 1, file - 1);
            }

            if (rank - 1 >= 0) {
                mask |= 1L << getSquare(rank - 1, file);
            }

            if (file + 1 <= 7 && rank - 1 >= 0) {
                mask |= 1L << getSquare(rank - 1, file + 1);
            }

            if (file + 1 <= 7) {
                mask |= 1L << getSquare(rank, file + 1);
            }

            KING_MASKS[square] = mask;

        }
    }

    private void generateRookMasks() {
        for (int square = 0; square < 64; square++) {
            long mask = 0L;
            int rank = getRank(square);
            int file = getFile(square);
            while (rank < 6) {
                int nextSquare = getSquare(rank + 1, file);
                mask |= 1L << (nextSquare);
                rank = getRank(nextSquare);
                file = getFile(nextSquare);
            }

            rank = getRank(square);
            file = getFile(square);

            while (file < 6) {
                int nextSquare = getSquare(rank, file + 1);
                mask |= 1L << (nextSquare);
                rank = getRank(nextSquare);
                file = getFile(nextSquare);
            }

            rank = getRank(square);
            file = getFile(square);

            while (rank > 1) {
                int nextSquare = getSquare(rank - 1, file);
                mask |= 1L << (nextSquare);
                rank = getRank(nextSquare);
                file = getFile(nextSquare);
            }

            rank = getRank(square);
            file = getFile(square);
            while (file > 1) {
                int nextSquare = getSquare(rank, file - 1);
                mask |= 1L << (nextSquare);
                rank = getRank(nextSquare);
                file = getFile(nextSquare);
            }


            ROOK_MASKS[square] = mask;
        }
    }

    private void generateBishopMasks() {
        for (int square = 0; square < 64; square++) {
            long mask = 0L;

            // top left diagonal
            int rank = getRank(square);
            int file = getFile(square);
            while (rank < 6 && file < 6) {
                int nextSquare = getSquare(rank + 1, file + 1);
                mask |= 1L << (nextSquare);
                rank = getRank(nextSquare);
                file = getFile(nextSquare);
            }

            // bottom left diagonal
            rank = getRank(square);
            file = getFile(square);
            while (rank < 6 && file > 1) {
                int nextSquare = getSquare(rank + 1, file - 1);
                mask |= 1L << (nextSquare);
                rank = getRank(nextSquare);
                file = getFile(nextSquare);
            }

            // top right diagonal
            rank = getRank(square);
            file = getFile(square);
            while (rank > 1 && file < 6) {
                int nextSquare = getSquare(rank - 1, file + 1);
                mask |= 1L << (nextSquare);
                rank = getRank(nextSquare);
                file = getFile(nextSquare);
            }

            // bottom right diagonal
            rank = getRank(square);
            file = getFile(square);
            while (rank > 1 && file > 1) {
                int nextSquare = getSquare(rank - 1, file - 1);
                mask |= 1L << (nextSquare);
                rank
                        = getRank(nextSquare);
                file = getFile(nextSquare);
            }

            this.BISHOP_MASKS[square] = mask;

        }
    }


    private static long blockersGenerator(int index, long mask) {
        long blockers = 0;
        while (index != 0) {
            if ((index & 1) != 0) {
                int shift = Long.numberOfTrailingZeros(mask);
                blockers |= (1L << shift);
            }
            index >>>= 1;
            mask &= (mask - 1);
        }

        return blockers;
    }

    private static int transform(long blockers, long magic, int shift) {
        return (int) ((blockers * magic) >>> (64 - shift));
    }

    ;

    private void initRookLookups() {
        for (int square = 0; square < 64; square++) {
            long mask = ROOK_MASKS[square];
            int permCount = 1 << Long.bitCount(mask);
            for (int i = 0; i < permCount; i++) {
                long blockers = blockersGenerator(i, mask);
                long attacks = 0L;
                int file = getFile(square), f;
                int rank = getRank(square), r;

                for (r = rank + 1; r <= 7; r++) {
                    attacks |= (1L << getSquare(r, file));
                    if ((blockers & (1L << getSquare(r, file))) != 0) {
                        break;
                    }
                }

                for (f = file + 1; f <= 7; f++) {
                    attacks |= (1L << getSquare(rank, f));
                    if ((blockers & (1L << getSquare(rank, f))) != 0) {
                        break;
                    }
                }

                for (r = rank - 1; r >= 0; r--) {
                    attacks |= (1L << getSquare(r, file));
                    if ((blockers & (1L << getSquare(r, file))) != 0) {
                        break;
                    }
                }

                for (f = file - 1; f >= 0; f--) {
                    attacks |= (1L << getSquare(rank, f));
                    if ((blockers & (1L << getSquare(rank, f))) != 0) {
                        break;
                    }
                }
                int key = transform(blockers, MAGICS.RMagic[square], Long.bitCount(ROOK_MASKS[square]));
                ROOK_LOOKUPS[square][key] = attacks;
            }
        }
    }

    private void initBishopLookups() {
        for (int square = 0; square < 64; square++) {
            long mask = BISHOP_MASKS[square];
            int permCount = 1 << Long.bitCount(mask);
            for (int i = 0; i < permCount; i++) {
                long blockers = blockersGenerator(i, mask);
                long attacks = 0L;
                int file = getFile(square), f;
                int rank = getRank(square), r;

                for (r = rank + 1, f = file + 1; r <= 7 && f <= 7; r++, f++) {
                    attacks |= (1L << getSquare(r, f));
                    if ((blockers & (1L << getSquare(r, f))) != 0) {
                        break;
                    }
                }

                for (r = rank - 1, f = file + 1; r >= 0 && f <= 7; r--, f++) {
                    attacks |= (1L << getSquare(r, f));
                    if ((blockers & (1L << getSquare(r, f))) != 0) {
                        break;
                    }
                }

                for (r = rank - 1, f = file - 1; r >= 0 && f >= 0; r--, f--) {
                    attacks |= (1L << getSquare(r, f));
                    if ((blockers & (1L << getSquare(r, f))) != 0) {
                        break;
                    }
                }

                for (r = rank + 1, f = file - 1; r <= 7 && f >= 0; r++, f--) {
                    attacks |= (1L << getSquare(r, f));
                    if ((blockers & (1L << getSquare(r, f))) != 0) {
                        break;
                    }
                }
                int key = transform(blockers, MAGICS.BMagic[square], Long.bitCount(mask));
                BISHOP_LOOKUPS[square][key] = attacks;

            }
        }
    }


    public long getBishopAttacks(int square) {
        long blockers = allPieces() & BISHOP_MASKS[square];
        int key = transform(blockers, MAGICS.BMagic[square], Long.bitCount(BISHOP_MASKS[square]));
        return BISHOP_LOOKUPS[square][key];
    }

    public long getRookAttacks(int square) {
        long blockers = allPieces() & ROOK_MASKS[square];
        int key = transform(blockers, MAGICS.RMagic[square], Long.bitCount(ROOK_MASKS[square]));
        return ROOK_LOOKUPS[square][key];
    }

    public static int[] getSquares(long BB) {
        int[] results = new int[Long.bitCount(BB)];
        int i = 0;
        while (BB != 0) {
            results[i] = 63 - Long.numberOfLeadingZeros(BB);
            BB &= ~(1L << results[i]);
            i++;
        }
        return results;
    }


    public long getAttackers(int square, boolean white) {
        long attackers = 0L;
        long[] potentialAttackerBB = white ? whiteBitboards : blackBitboards;

        long qBishops = (potentialAttackerBB[Piece.PieceType.QUEEN.ordinal()] | potentialAttackerBB[Piece.PieceType.BISHOP.ordinal()]);
        long qRooks = (potentialAttackerBB[Piece.PieceType.QUEEN.ordinal()] | potentialAttackerBB[Piece.PieceType.ROOK.ordinal()]);
        attackers |= potentialAttackerBB[Piece.PieceType.PAWN.ordinal()] & PAWN_ATTACK_MASKS[white ? 0 : 1][square];
        attackers |= potentialAttackerBB[Piece.PieceType.KNIGHT.ordinal()] & KNIGHT_MASKS[square];
        attackers |= qBishops & getBishopAttacks(square);
        attackers |= qRooks & getRookAttacks(square);
        attackers |= potentialAttackerBB[Piece.PieceType.KING.ordinal()] & KING_MASKS[square];

        return attackers;
    }

    private void applyBoardEdge(BoardEdge edge) {
        if (edge == null ) {
            return;
        }
        this.setSquare(edge.targetSquare, edge.toPiece);
    }

    private void undoBoardEdge(BoardEdge edge) {
        if (edge == null ) {
            return;
        }
        this.setSquare(edge.targetSquare, edge.fromPiece);
    }

    private void applyMoveEdge(MoveEdge edge) {
        for (BoardEdge boardEdge : edge.transformations) {
            applyBoardEdge(boardEdge);
        }

        if (edge.unsetWhiteKingside) whiteKingside = false;
        if (edge.unsetWhiteQueenside) whiteQueenside = false;

        if (edge.unsetBlackQueenside) blackQueenside = false;
        if (edge.unsetBlackKingside) blackKingside = false;

        enpassantSqr = edge.newEnpassantSqr;

    }

    private void undoMoveEdge(MoveEdge edge) {
        for (BoardEdge boardEdge: edge.transformations) {
            undoBoardEdge(boardEdge);
        }

        if (edge.unsetBlackKingside) blackKingside = true;
        if (edge.unsetWhiteKingside) whiteKingside = true;

        if (edge.unsetWhiteQueenside) whiteQueenside = true;
        if (edge.unsetBlackQueenside) blackQueenside = true;

        enpassantSqr = edge.oldEnpassantSqr;
        this.halfmoveClock = edge.oldHalfMoveClock;
    }

    private void forceMakeMove(Move move) {
        // makes a move whether it is legal or not.
        MoveEdge edge = new MoveEdge(move, this);
        this.whiteMove = !this.whiteMove;
        this.halfmoveClock += 1;
        boolean castling = (move.queensideCastle || move.kingsideCastle);
        if (getPiece(move.fromSquare).type == Piece.PieceType.PAWN || (getPiece(move.toSquare) != null && !castling)) {
            halfmoveClock = 0;
        }
        this.fullmoveClock += this.whiteMove ? 1 : 0;
        applyMoveEdge(edge);
        this.MOVES.add(edge);

    }




    public void makeMove(Move move) {
        if (this.isLegalMove(move) && gameState == GameState.ONGOING) {
            this.forceMakeMove(move);
            if ((getUnsortedLegalMoves().length == 0) && inCheck(whiteMove)) {
                this.gameState = whiteMove ? GameState.BLACK_WIN : GameState.WHITE_WIN;
            } else if (getUnsortedLegalMoves().length == 0 || halfmoveClock >= 50) {
                this.gameState = GameState.DRAW;
            }

        } else {
            throw new RuntimeException("Attempted to make an illegal move, or this game has finished.");
        }
    }

    public void unMakeMove() {
        gameState = GameState.ONGOING;
        this.forceUnMakeMove();
    }

    private void forceUnMakeMove() {
        MoveEdge move = this.MOVES.remove(MOVES.size() - 1);
        undoMoveEdge(move);
        this.whiteMove = !this.whiteMove;
        this.fullmoveClock -= this.whiteMove ? 0 : 1;
    }

    private long[] friendlyBitboards() {
        return whiteMove ? this.whiteBitboards : this.blackBitboards;
    }

    private long[] enemyBitboards() {
        return !whiteMove ? this.whiteBitboards : this.blackBitboards;
    }

    public boolean inCheck(boolean white) {
        int kingSquare = getSquares((white ? whiteBitboards : blackBitboards)[Piece.PieceType.KING.ordinal()])[0];
        return getAttackers(kingSquare, !white) != 0;
    }

    public boolean  isLegalMove(Move move) {
        this.forceMakeMove(move);
        boolean inCheck = inCheck(!whiteMove);
        this.forceUnMakeMove();
        return !inCheck;
    }


    public Move[] getUnsortedLegalMoves() {
        ArrayList<Move> moves = new ArrayList<>(List.of());

        for (Move move : getPseudoLegalMoves()) {
            if (isLegalMove(move)) {
                moves.add(move);
            }
        }
        return moves.toArray(new Move[0]);
    }
    public Move[] getUnsortedLegalQuiescentMoves() {
        ArrayList<Move> moves = new ArrayList<>(List.of(getUnsortedLegalMoves()));
        if (inCheck(whiteMove)) {
            return moves.toArray(new Move[0]); // if we're in check we need to look at every way of escaping it
        }

        ArrayList<Move> quiescents = new ArrayList<>(List.of());
        for (Move move: moves) {
            if ((1L << move.toSquare & getPieces(!whiteMove)) != 0) { // if their are any captures the position is not quiet
                quiescents.add(move);
            }
            makeMove(move);
            if (inCheck(whiteMove)) { // if there are checks the position isn't quiet
                quiescents.add(move);
            }
            if (gameState != GameState.ONGOING) {
                quiescents.add(move); // if we can get mated in one the position isn't quiet
            }

            unMakeMove();
        }
        return quiescents.toArray(new Move[0]);
    }

    public Move[] getLegalMoves() {
        return getLegalMoves(false);
    }

    private class SortByCaptureValue implements Comparator<Move> {

        public int getScore(Move move) {
            return PIECE_VALUES[getPiece(move.toSquare).type.ordinal()] - PIECE_VALUES[getPiece(move.fromSquare).type.ordinal()];
        }

        @Override
        public int compare(Move a, Move b) {
            return getScore(a) - getScore(b);
        }
    }

    public Move[] getLegalMoves(boolean quiesce) {
        ArrayList<Move> moves = new ArrayList<>(List.of(quiesce ? getUnsortedLegalQuiescentMoves(): getUnsortedLegalMoves()));
        Move[] sortedMoves = new Move[moves.size()];
        int i = 0;
        // add terminal nodes first
        for (int j=0; j< moves.size(); j++) {
            Move move = moves.get(j);
            makeMove(move);
            if (gameState != GameState.ONGOING) {
                sortedMoves[i] = move;
                i++;
                moves.remove(move);
            }
            unMakeMove();
        }
        // then add checks
        for (int j=0; j<moves.size(); j++) {
            Move move = moves.get(j);
            makeMove(move);
            if (inCheck(whiteMove)) {
                sortedMoves[i] = move;
                i++;
                moves.remove(move);
            }
            unMakeMove();
        }
        // captures
        ArrayList<Move> captures = new ArrayList<Move>(List.of());
        for (int j=0; j<moves.size(); j++) {
            Move move = moves.get(j);
            if ((1L << move.toSquare & getPieces(!whiteMove)) != 0) {
                captures.add(move);
                moves.remove(move);
            }
        }
        captures.sort(new SortByCaptureValue());
        for (Move capture: captures) {
            sortedMoves[i] = capture;
            i++;
        }

        // everything else
        for (Move move: moves) {
            sortedMoves[i] = move;
            i++;
        }


        return sortedMoves;

    }


    private Move[] getPseudoLegalMoves() {
        // PSEUDO LEGAL PAWN MOVES
        ArrayList<Move> moves = new ArrayList<>(List.of());
        int[] pawns = getSquares(friendlyBitboards()[Piece.PieceType.PAWN.ordinal()]);
        for (int pawn : pawns) {
            moves.addAll(List.of(getPawnMoves(pawn)));
        }
        // PSEUDO LEGAL KNIGHT MOVES
        int[] knights = getSquares(friendlyBitboards()[Piece.PieceType.KNIGHT.ordinal()]);
        for (int knight : knights) {
            moves.addAll(List.of(getKnightMoves(knight)));
        }

        // PSEUDO LEGAL BISHOP MOVES
        int[] bishops = getSquares(friendlyBitboards()[Piece.PieceType.BISHOP.ordinal()]);
        for (int bishop : bishops) {
            moves.addAll(List.of(getBishopMoves(bishop)));
        }

        // PSEUDO LEGAL ROOK MOVES
        int[] rooks = getSquares(friendlyBitboards()[Piece.PieceType.ROOK.ordinal()]);
        for (int rook : rooks) {
            moves.addAll(List.of(getRookMoves(rook)));
        }
        // PSEUDO LEGAL QUEEN MOVES
        int[] queens = getSquares(friendlyBitboards()[Piece.PieceType.QUEEN.ordinal()]);
        for (int queen : queens) {
            moves.addAll(List.of(getQueenMoves(queen)));
        }

        // PSEUDO LEGAL KING MOVES
        int[] kings = getSquares(friendlyBitboards()[Piece.PieceType.KING.ordinal()]);
        for (int king : kings) {
            moves.addAll(List.of(getKingMoves(king)));
        }

        return moves.toArray(new Move[0]);

    }


    private Move[] getMoves(long movesBB, int square, Piece mover) {
        Move[] moves = new Move[Long.bitCount(movesBB)];
        int i = 0;
        while (movesBB != 0) {
            int target = 63 - Long.numberOfLeadingZeros(movesBB);
            moves[i] = new Move(square, target);
            movesBB &= ~(1L << target);
            i++;
        }

        return moves;
    }

    public Move[] getBishopMoves(int square) {
        long movesBB = getBishopAttacks(square) & ~getPieces(whiteMove);
        return getMoves(movesBB, square, new Piece(Piece.PieceType.BISHOP, whiteMove));
    }



    public Move[] getRookMoves(int square) {
        long movesBB = getRookAttacks(square) & ~getPieces(whiteMove);
        return getMoves(movesBB, square, new Piece(Piece.PieceType.ROOK, whiteMove));
    }


    public Move[] getQueenMoves(int square) {
        long movesBB = (getRookAttacks(square) | getBishopAttacks(square)) & ~getPieces(whiteMove);
        return getMoves(movesBB, square, new Piece(Piece.PieceType.QUEEN, whiteMove));
    }



    public Move[] getKingMoves(int square) {
        long movesBB = KING_MASKS[square] & ~getPieces(whiteMove);
        ArrayList<Move> moves = new ArrayList<>(Long.bitCount(movesBB));
        moves.addAll(List.of(getMoves(movesBB, square, new Piece(Piece.PieceType.KING, whiteMove))));
        if (whiteMove && whiteKingside) {
            if ((getAttackers(3, false) | getAttackers(2, false) | getAttackers(1, false)) == 0) {
                if ((0b110L & allPieces()) == 0) moves.add(Move.kingsideCastle(true));
            }
        }
        if (!whiteMove && blackKingside) {
            if ((getAttackers(59, true) | getAttackers(58, true) | getAttackers(57, true)) == 0) {
                if (((0b110L << 56) & allPieces()) == 0) moves.add(Move.kingsideCastle(false));
            }
        }

        if (whiteMove && whiteQueenside) {
            if ((getAttackers(3, false) | getAttackers(4, false) | getAttackers(5, false)) == 0) {
                if ((0b1110000L & allPieces()) == 0) moves.add(Move.queensideCastle(true));
            }
        }
        if (!whiteMove && blackQueenside) {
            if ((getAttackers(59, true) | getAttackers(60, true) | getAttackers(61, true)) == 0) {
                if (((0b1110000L << 56) & allPieces()) == 0) moves.add(Move.queensideCastle(false));
            }
        }

        return moves.toArray(new Move[0]);
    }

    public Move[] getKnightMoves(int square) {
        long movesBB = KNIGHT_MASKS[square] & ~getPieces(whiteMove);
        return getMoves(movesBB, square, new Piece(Piece.PieceType.KNIGHT, whiteMove));
    }

    public Move[] getPawnMoves(int square) {
        return getPawnMoves(square, false);
    }

    public Move[] getPawnMoves(int square, boolean capturesOnly) {
        long squareInFront = (1L << (square + (whiteMove ? 8 : -8))) & ~allPieces();
        long movesBB = PAWN_ATTACK_MASKS[whiteMove ? 1 : 0][square] & getPieces(!whiteMove);
        movesBB |= enpassantSqr <= 63 ? PAWN_ATTACK_MASKS[whiteMove ? 1 : 0][square] & (1L << enpassantSqr) : 0;
        movesBB |= capturesOnly ? 0 : squareInFront;
        if (getRank(square) == 1 && whiteMove && squareInFront != 0 && !capturesOnly) {
            movesBB |= (1L << (square + 16)) & ~allPieces();
        }
        if (getRank(square) == 6 && !whiteMove && squareInFront != 0 && !capturesOnly) {
            movesBB |= (1L << (square - 16)) & ~allPieces();
        }
        boolean promoting = (whiteMove && getRank(square) == 6) || (!whiteMove && getRank(square) == 1);

        Move[] beforePromotions = this.getMoves(movesBB, square, new Piece(Piece.PieceType.PAWN, whiteMove));
        ArrayList<Move> moves = new ArrayList<>();
        if (!promoting) {
            moves.addAll(List.of(beforePromotions));
        } else {
            for (Move move : beforePromotions) {
                for (int i = 1; i < 5; i++) {
                    moves.add(new Move(square, move.toSquare, Piece.PieceType.values()[i]));
                }
            }
        }

        return moves.toArray(new Move[0]);
    }

    public int perft(int depth) {

        int nodes = 0;
        Move[] moves = getUnsortedLegalMoves();
        if (depth == 0) {
            return 1;
        }

        if (depth == 1) {
            return moves.length;
        }

        for (Move move : moves) {
            makeMove(move);
            nodes += perft(depth - 1);
            unMakeMove();
        }

        return nodes;
    }

    public int eval() {
        int eval = 0;
        eval += Long.bitCount(whiteBitboards[Piece.PieceType.PAWN.ordinal()]);
        eval -= Long.bitCount(blackBitboards[Piece.PieceType.PAWN.ordinal()]);

        eval += 3*(Long.bitCount(whiteBitboards[Piece.PieceType.BISHOP.ordinal()])
                + Long.bitCount(whiteBitboards[Piece.PieceType.KNIGHT.ordinal()]));
        eval -= 3*(Long.bitCount(blackBitboards[Piece.PieceType.BISHOP.ordinal()])
                + Long.bitCount(blackBitboards[Piece.PieceType.KNIGHT.ordinal()]));

        eval += 5*(Long.bitCount(whiteBitboards[Piece.PieceType.ROOK.ordinal()]));
        eval -= 5*(Long.bitCount(blackBitboards[Piece.PieceType.ROOK.ordinal()]));

        eval += 9*(Long.bitCount(whiteBitboards[Piece.PieceType.QUEEN.ordinal()]));
        eval -= 9*(Long.bitCount(blackBitboards[Piece.PieceType.QUEEN.ordinal()]));


        return eval;
    }

}