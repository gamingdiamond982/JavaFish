import board.Board;
import board.Move;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;



public class UCIManager {
    public final String STARTPOS = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    Board board = new Board();

    public UCIManager() {
    }

    public void runUCI() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        SearchWorker worker;
        Thread workerThread;
        while (true) {
            String[] cmd = reader.readLine().split("\\s+");
            if (cmd.length == 0) {
                continue;
            }
            switch (cmd[0]) {
                case "uci":
                    System.out.println("id name JavaFish");
                    System.out.println("id author Oisin Johnston");
                    System.out.println("uciok");
                    break;

                case "isready":
                    System.out.println("readyok");
                    break;

                case "ucinewgame":
                    board = new Board();
                    workerThread = null;
                    break;

                case "position":
                    int movesIndex;
                    if (Objects.equals(cmd[1], "startpos")) {
                        board.setPosition(STARTPOS);
                        movesIndex = 2;
                    } else {
                        board.setPosition(cmd[1] + " " + cmd[2] + " " + cmd[3] + " " + cmd[4] + " " + cmd[5] + " " + cmd[6]);
                        movesIndex = 7;
                    }
                    if (cmd.length > movesIndex && Objects.equals(cmd[movesIndex], "moves")) {
                        for (int i = movesIndex+1; i< cmd.length; i++) {
                            board.makeMove(Move.fromAlgebra(cmd[i]));
                        }
                    }
                    break;

                case "go":
                    worker = new SearchWorker(board, 2);
                    worker.run();
                    System.out.println("bestmove " + worker.bestMove.getAlgebra());
                    break;

                case "quit":
                    return;


            }
        }
    }


}