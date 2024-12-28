import board.*;


public class SearchWorker implements Runnable {

    Board board;
    int depth;
    int eval;
    Move bestMove;

    public SearchWorker(Board board, int depth) {
        this.board = board;
        this.depth = depth;

    }


    public static int minimax(Board board, int depth, int a, int b) {
        boolean quiesce = false;
        Board.GameState gameState = board.getGameState();
        if (gameState != Board.GameState.ONGOING) {
            return switch (gameState) {
                case WHITE_WIN -> Integer.MAX_VALUE;
                case BLACK_WIN -> Integer.MIN_VALUE;
                case DRAW -> 0;
                default -> throw new IllegalStateException("Unreachable Code");
            };
        }

        if (depth <= 0) {
            quiesce = true;
        }
        Move[] moves = board.getLegalMoves(quiesce);
        if (moves.length == 0 || depth <= -2) {
            // we must be in a quiescent search as the game would be over were this not the case
            return board.eval();
        }

        if (board.whiteMove) {
            int best = Integer.MIN_VALUE;
            for (Move move: moves) {
                board.makeMove(move);
                int value = minimax(board, depth - 1, a, b);
                board.unMakeMove();
                best = Integer.max(best, value);
                a = Integer.max(a, best);
                if (b <= a) {
                    break;
                }

            }
            return best;
        } else {
            int best = Integer.MAX_VALUE;
            for (Move move: moves) {
                board.makeMove(move);
                int value = minimax(board, depth - 1, a, b);
                board.unMakeMove();
                best = Integer.min(value, best);
                b = Integer.min(b, best);
                if (b <= a) {
                    break;
                }
            }
            return best;
        }

    }


    @Override
    public void run() {
        int a = Integer.MIN_VALUE;
        int b = Integer.MAX_VALUE;
        if (board.whiteMove) {
            int best = Integer.MIN_VALUE;
            for (Move move: board.getLegalMoves()) {
                board.makeMove(move);
                int eval = minimax(board, depth-1, a, b);
                board.unMakeMove();

                best = Integer.max(eval, best);
                if (best == eval) {
                    this.eval = best;
                    this.bestMove = move;
                    System.out.println("info currmove " + bestMove.getAlgebra());
                    System.out.println("info eval " + eval);
                }
                a = best;
                if (b == a) {
                    break;
                }

            }

        } else {
            int best = Integer.MAX_VALUE;
            for (Move move: board.getLegalMoves()) {
                board.makeMove(move);
                int eval = minimax(board, depth-1, a, b);
                board.unMakeMove();

                best = Integer.min(eval, best);
                if (best == eval) {
                    this.eval = best;
                    this.bestMove = move;
                    System.out.println("info currmove " + bestMove.getAlgebra());
                    System.out.println("info eval " + eval);
                }
                b = best;
                if (b==a) {
                    break;
                }
            }
        }

    }
}
