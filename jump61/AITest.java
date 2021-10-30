package jump61;

import static jump61.Side.*;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;

public class AITest {
    private int searchForMove(Board b, Side side) {
        int bestScore = negInf;
        int bestMove = -1;
        int score;
        for (int i: validIndices(b, side)) {
            b.addSpot(side, i);
            score = minMax(b, 1, -1, side.opposite(), negInf, posInf);
            b.undo();
            if (score > bestScore) {
                bestScore = score;
                bestMove = i;
            }
        }
        return bestMove;
    }

    private int minMax(Board b, int depth, int sense, Side side,
                       int alpha, int beta) {
        if (depth == 0 || b.getWinner() != null) {
            return dummyStaticEval(b, sense);
        }
        int bestScore = sense * negInf;
        for (int i: validIndices(b, side)) {
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

    private ArrayList<Integer> validIndices(Board work, Side player) {
        ArrayList<Integer> vInd = new ArrayList<Integer>();
        for (int index = 0; index < work.numSquares(); index += 1) {
            if (work.isLegal(player, index)) {
                vInd.add(index);
            }
        }
        return vInd;
    }

    private ArrayList<Square> validMoves(Board work, Side player) {
        ArrayList<Square> vMoves = new ArrayList<Square>();
        ArrayList<Square> gameBoard = work.board();
        for (int index = 0; index < work.numSquares(); index += 1) {
            if (work.isLegal(player, index)) {
                vMoves.add(gameBoard.get(index));
            } else {
                vMoves.add(null);
            }
        }
        return vMoves;
    }

    private ArrayList<Square> validConcise(Board work, Side player) {
        ArrayList<Square> vMoves = validMoves(work, player);
        ArrayList<Square> vConcise = new ArrayList<Square>();
        for (Square s: vMoves) {
            if (s == null) {
                continue;
            }
            vConcise.add(s);
        }
        return vConcise;
    }

    private int dummyStaticEval(Board b, int sense) {
        Side result = b.getWinner();
        if (result != null) {
            return 100 * sense;
        }
        return 1;
    }

    private int staticEval(Board b, int sense) {
        Side result = b.getWinner();
        if (result != null) {
            return 100 * sense;
        }
        return 1;
    }

    @Test
    public void testSearchForMove() {
        Board B = new Board(4);
        System.out.println("Default Board:\n" + B);
        assertEquals(0, searchForMove(B, RED));
        B.addSpot(RED, searchForMove(B, RED));
        System.out.println("RED Move: \n" + B);
        assertEquals(1, searchForMove(B, BLUE));
        B.addSpot(BLUE, searchForMove(B, BLUE));
        System.out.println("BLUE Move:\n" + B);
        assertEquals(0, searchForMove(B, RED));
        B.addSpot(RED, searchForMove(B, RED));
        System.out.println("RED Move:\n" + B);
        assertEquals(2, searchForMove(B, BLUE));
        B.addSpot(BLUE, searchForMove(B, BLUE));
        System.out.println("BLUE Move:\n" + B);
    }

    @Test
    public void testValidMoves() {
        Board B = new Board(3);
        ArrayList<Square> vMR = new ArrayList<Square>(B.board());
        ArrayList<Square> vMB = new ArrayList<Square>(B.board());
        assertArrayEquals(vMR.toArray(), validMoves(B, RED).toArray());
        assertArrayEquals(vMB.toArray(), validMoves(B, BLUE).toArray());

        B.addSpot(RED, 0);
        vMR = new ArrayList<Square>(B.board());
        vMB = new ArrayList<Square>(B.board()); vMB.set(0, null);
        assertArrayEquals(vMR.toArray(), validMoves(B, RED).toArray());
        assertArrayEquals(vMB.toArray(), validMoves(B, BLUE).toArray());

        B.addSpot(BLUE, 1);
        vMR = new ArrayList<Square>(B.board()); vMR.set(1, null);
        vMB = new ArrayList<Square>(B.board()); vMB.set(0, null);
        assertArrayEquals(vMR.toArray(), validMoves(B, RED).toArray());
        assertArrayEquals(vMB.toArray(), validMoves(B, BLUE).toArray());

        B.addSpot(RED, 0);
        vMR = new ArrayList<Square>(B.board());
        vMB = new ArrayList<Square>(B.board());
        vMB.set(0, null); vMB.set(1, null); vMB.set(3, null);
        assertArrayEquals(vMR.toArray(), validMoves(B, RED).toArray());
        assertArrayEquals(vMB.toArray(), validMoves(B, BLUE).toArray());
        ArrayList<Square> vMBConcise = vMB;
        for (int i = 0; i < 3; i += 1) {
            vMBConcise.remove(null);
        }
        assertArrayEquals(vMBConcise.toArray(),
                validConcise(B, BLUE).toArray());
        ArrayList<Integer> vMBInd = new ArrayList<Integer>();
        vMBInd.add(2);
        for (int i = 4; i < 9; i += 1) {
            vMBInd.add(i);
        }
        assertArrayEquals(vMBInd.toArray(), validIndices(B, BLUE).toArray());
    }

    /** Checks that B conforms to the description given by CONTENTS.
     *  CONTENTS should be a sequence of groups of 4 items:
     *  r, c, n, s, where r and c are row and column number of a square of B,
     *  n is the number of spots that are supposed to be there and s is the
     *  color (RED or BLUE) of the square.  All squares not listed must
     *  be WHITE with one spot.  Raises an exception signaling a unit-test
     *  failure if B does not conform. */
    private void checkBoard(String msg, Board B, Object... contents) {
        for (int k = 0; k < contents.length; k += 4) {
            String M = String.format("%s at %d %d", msg, contents[k],
                    contents[k + 1]);
            assertEquals(M, (int) contents[k + 2],
                    B.get((int) contents[k],
                            (int) contents[k + 1]).getSpots());
            assertEquals(M, contents[k + 3],
                    B.get((int) contents[k],
                            (int) contents[k + 1]).getSide());
        }
        int c;
        c = 0;
        for (int i = B.size() * B.size() - 1; i >= 0; i -= 1) {
            assertTrue("bad white square #" + i,
                    (B.get(i).getSide() != WHITE)
                            || (B.get(i).getSpots() == 1));
            if (B.get(i).getSide() != WHITE) {
                c += 1;
            }
        }
        assertEquals("extra squares filled", contents.length / 4, c);
    }

    private final int posInf = Integer.MAX_VALUE;

    private final int negInf = Integer.MIN_VALUE;

}
