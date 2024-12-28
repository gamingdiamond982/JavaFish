
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

import board.*;

import java.io.IOException;
import java.util.Objects;

public class Main {

    public static void main(String[] args) throws IOException {
        if (args.length == 0)  {
            // Defaults to launching in UCI mode
            UCIManager uciManager = new UCIManager();
            uciManager.runUCI();
            return;

        }


        if (Objects.equals(args[0], "perft")) {
            Board board = new Board();
            board.setPosition(args[2]);
            int depth = Integer.parseInt(args[1]);
            Move[] legalMoves = board.getUnsortedLegalMoves();
            int totalPerft = 0;
            for (Move move : legalMoves) {
                board.makeMove(move);
                int thisPerft = board.perft(depth - 1);
                System.out.print(move.getAlgebra() + " " + thisPerft + "\n");
                totalPerft += thisPerft;
                board.unMakeMove();
            }
            System.out.println();
            System.out.println(totalPerft);
        }

    }
}