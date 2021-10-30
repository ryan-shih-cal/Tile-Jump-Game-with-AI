package jump61;

import java.util.ArrayList;
import java.util.Random;

import static jump61.Side.*;

/** An automated Player.
 *  @author P. N. Hilfinger
 */
class AI extends Player {

    /** A new player of GAME initially COLOR that chooses moves automatically.
     *  SEED provides a random-number seed used for choosing moves.
     */
    AI(Game game, Side color, long seed) {
        super(game, color);
        _random = new Random(seed);
    }

    @Override
    String getMove() {
        Board board = getGame().getBoard();

        assert getSide() == board.whoseMove();
        int choice = searchForMove(board, board.whoseMove());
        getGame().reportMove(board.row(choice), board.col(choice));
        return String.format("%d %d", board.row(choice), board.col(choice));
    }

    /** Return a move after searching the game tree to DEPTH>0 moves
     *  from the current position. Assumes the game is not over.
     *  Takes in Board B and SIDE. */
    private int searchForMove(Board b, Side side) {
        Board board = b.getBoard();
        int bestScore = negInf;
        int bestMove = -1;
        int score;
        ArrayList<Integer> vI = validIndices(board, side);
        if (board.getWinner() != null) {
            System.out.println("Winner detected: " + board.getWinner());
        }
        if (vI.size() == 0) {
            System.out.println("No valid moves on this board:\n" + board);
        }
        for (int i: vI) {
            board.addSpot(side, i);
            score = minMax(board, 7, -1, side.opposite(), negInf, posInf);
            board.undo();
            if (score > bestScore) {
                bestScore = score;
                bestMove = i;
            }
        }
        return bestMove;
    }

    /** Returns an ArrayList of Integers the same size as the board WORK.
     *  It will fill in all valid positions PLAYER can move with its
     *  respective index. */
    private ArrayList<Integer> validIndices(Board work, Side player) {
        ArrayList<Integer> vInd = new ArrayList<Integer>();
        for (int index = 0; index < work.numSquares(); index += 1) {
            if (work.isLegal(player, index)) {
                vInd.add(index);
            }
        }
        return vInd;
    }

    /** Find a move from Board B by SIDE and return its value. The move
     *  should have maximal value or have value > BETA if SENSE==1,
     *  and minimal value or value < ALPHA if SENSE==-1. Searches up to
     *  DEPTH levels.  Searching at level 0 simply returns a static estimate
     *  of the board value and does not set _foundMove. If the game is over
     *  on BOARD, does not set _foundMove. */
    private int minMax(Board b, int depth, int sense, Side side,
                       int alpha, int beta) {
        if (depth == 0 || b.getWinner() != null) {
            return staticEval(b, sense, depth);
        }
        int bestScore = sense * negInf;
        ArrayList<Integer> vI = validIndices(b, side);
        for (int i: vI) {
            b.addSpot(side, i);
            int score = minMax(b, depth - 1, sense * -1,
                    side.opposite(), alpha, beta);
            b.undo();
            if (sense * (score - bestScore) > 0) {
                bestScore = score;
                if (sense == 1) {
                    alpha = Math.max(alpha, bestScore);
                } else {
                    beta = Math.min(beta, bestScore);
                }
                if (alpha >= beta) {
                    return bestScore;
                }
            }
        }
        return bestScore;
    }

    /** Return a heuristic estimate of the value of board position B.
     *  Takes in SENSE to differentiate between a maxizing and minimizing
     *  player along with DEPTH to prioritize boards that win quicker. */
    private int staticEval(Board b, int sense, int depth) {
        Side result = b.getWinner();
        if (result != null) {
            return 100 * sense * depth;
        }
        return 1;
    }

    /** A random-number generator used for move selection. */
    private Random _random;

    /** A large positive number representing positive infinity. */
    private final int posInf = 1000000;

    /** A large negative number representing negative infinity. */
    private final int negInf = -1000000;
}
