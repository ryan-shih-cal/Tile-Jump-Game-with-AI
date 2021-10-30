package jump61;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Formatter;

import java.util.Stack;
import java.util.function.Consumer;

import static jump61.Side.*;
import static jump61.Square.square;

/** Represents the state of a Jump61 game.  Squares are indexed either by
 *  row and column (between 1 and size()), or by square number, numbering
 *  squares by rows, with squares in row 1 numbered from 0 to size()-1, in
 *  row 2 numbered from size() to 2*size() - 1, etc. (i.e., row-major order).
 *
 *  A Board may be given a notifier---a Consumer<Board> whose
 *  .accept method is called whenever the Board's contents are changed.
 *
 *  @author Ryan Shih
 */
class Board {

    /** An uninitialized Board.  Only for use by subtypes. */
    protected Board() {
        _notifier = NOP;
    }

    /** Current board state: represented by an Arraylist that stores squares
     *  indexed from 0 to numSquares - 1. */
    private ArrayList<Square> _gameBoard;
    /** Size: length of rows and columns. */
    private int _size;
    /** Number of Squares: the total number of tiles in the board. */
    private int _numSquares;
    /** Number of moves: records the current move number. */
    private int _numMoves;
    /** Game Over? returns true if the game is over and false otherwise. */
    private boolean _gameOver;

    /** An N x N board in initial configuration. */
    Board(int N) {
        this();
        _size = N;
        _numSquares = _size * _size;
        _gameBoard = new ArrayList<Square>(_numSquares);
        for (int i = 0; i < (_numSquares); i += 1) {
            _gameBoard.add(Square.square(WHITE, 1));
        }
        _gameOver = false;
        save();
    }

    /** A board whose initial contents are copied from BOARD0, but whose
     *  undo history is clear, and whose notifier does nothing. */
    Board(Board board0) {
        _size = board0.size();
        _numSquares = _size * _size;
        _numMoves = 0;
        _gameBoard = new ArrayList<Square>(_numSquares);
        for (int i = 0; i < (_numSquares); i += 1) {
            _gameBoard.add(board0.board().get(i));
        }
        _gameOver = getWinner() != null;
        _readonlyBoard = new ConstantBoard(this);
    }

    /** Gets Board. Relevant for Constant Boards. Returns NULL. */
    Board getBoard() {
        return null;
    }

    /** Copies the game board from ArrayList FROM to ArrayList TO. */
    public void copyGameBoard(ArrayList<Square> from, ArrayList<Square> to) {
        for (int i = 0; i < from.size(); i += 1) {
            Side side = from.get(i).getSide();
            int spots = from.get(i).getSpots();
            to.set(i, Square.square(side, spots));
        }
    }

    /** Returns a readonly version of this board. */
    Board readonlyBoard() {
        return _readonlyBoard;
    }

    /** Return the board state of THIS. */
    ArrayList<Square> board() {
        return _gameBoard;
    }

    /** Return the number of rows and of columns of THIS. */
    int size() {
        return _size;
    }

    /** Return the number of squares in THIS. */
    int numSquares() {
        return _numSquares;
    }

    /** (Re)initialize me to a cleared board with N squares on a side. Clears
     *  the undo history and sets the number of moves to 0. */
    void clear(int N) {
        _size = N;
        _numSquares = _size * _size;
        _numMoves = 0;
        _gameBoard = new ArrayList<Square>(_numSquares);
        for (int i = 0; i < (_numSquares); i += 1) {
            _gameBoard.add(Square.square(WHITE, 1));
        }
        _gameOver = false;
        restart();
        announce();
    }

    /** Copy the contents of BOARD into me. */
    void copy(Board board) {
        internalCopy(board);
    }

    /** Copy the contents of BOARD into me, without modifying my undo
     *  history. Assumes BOARD and I have the same size. */
    private void internalCopy(Board board) {
        assert size() == board.size();
        _size = board.size();
        _numSquares = _size * _size;
        _numMoves = 0;
        _gameBoard = new ArrayList<Square>(_numSquares);
        for (int i = 0; i < (_numSquares); i += 1) {
            _gameBoard.add(board.board().get(i));
        }
        _gameOver = getWinner() != null;
    }

    /** Returns the contents of the square at row R, column C
     *  1 <= R, C <= size (). */
    Square get(int r, int c) {
        return get(sqNum(r, c));
    }

    /** Returns the contents of square #N, numbering squares by rows, with
     *  squares in row 1 number 0 - size()-1, in row 2 numbered
     *  size() - 2*size() - 1, etc. */
    Square get(int n) {
        return board().get(n);
    }

    /** Returns the total number of spots on the board. */
    int numPieces() {
        int total = 0;
        for (Square s: board()) {
            total += s.getSpots();
        }
        return total;
    }

    /** Returns the Side of the player who would be next to move. If the
     *  game is won, this will return the loser (assuming legal position). */
    Side whoseMove() {
        return ((numPieces() + size()) & 1) == 0 ? RED : BLUE;
    }

    /** Return true iff row R and column C denotes a valid square. */
    final boolean exists(int r, int c) {
        return 1 <= r && r <= size() && 1 <= c && c <= size();
    }

    /** Return true iff S is a valid square number. */
    final boolean exists(int s) {
        int N = size();
        return 0 <= s && s < N * N;
    }

    /** Return the row number for square #N. */
    final int row(int n) {
        return n / size() + 1;
    }

    /** Return the column number for square #N. */
    final int col(int n) {
        return n % size() + 1;
    }

    /** Return the square number of row R, column C. */
    final int sqNum(int r, int c) {
        return (c - 1) + (r - 1) * size();
    }

    /** Return a string denoting move (ROW, COL)N. */
    String moveString(int row, int col) {
        return String.format("%d %d", row, col);
    }

    /** Return a string denoting move N. */
    String moveString(int n) {
        return String.format("%d %d", row(n), col(n));
    }

    /** Returns true iff it would currently be legal for PLAYER to add a spot
        to square at row R, column C. */
    boolean isLegal(Side player, int r, int c) {
        return isLegal(player, sqNum(r, c));
    }

    /** Returns true iff it would currently be legal for PLAYER to add a spot
     *  to square #N. */
    boolean isLegal(Side player, int n) {
        Side existingSide = board().get(n).getSide();
        return isLegal(player)
                && (player.equals(existingSide) || WHITE.equals(existingSide));
    }

    /** Returns true iff PLAYER is allowed to move at this point. */
    boolean isLegal(Side player) {
        return !_gameOver;
    }

    /** Returns the winner of the current position, if the game is over,
     *  and otherwise null. */
    final Side getWinner() {
        Side refSide = board().get(0).getSide();
        if (refSide.equals(WHITE)) {
            return null;
        }
        if (refSide.equals(BLUE) || refSide.equals(RED))  {
            for (Square s: board()) {
                if (!s.getSide().equals(refSide)) {
                    return null;
                }
            }
        }
        return refSide;
    }

    /** Return the number of squares of given SIDE. */
    int numOfSide(Side side) {
        int squares = 0;
        for (Square s: board()) {
            if (s.getSide().equals(side)) {
                squares += 1;
            }
        }
        return squares;
    }

    /** Add a spot from PLAYER at row R, column C.  Assumes
     *  isLegal(PLAYER, R, C). */
    void addSpot(Side player, int r, int c) {
        addSpot(player, sqNum(r, c));
    }

    /** Add a spot from PLAYER at square #N.  Assumes isLegal(PLAYER, N). */
    void addSpot(Side player, int n) {
        internalAddSpot(player, n);
        _numMoves += 1;
        save();
    }

    /** Add a spot from PLAYER at square #N.  Assumes isLegal(PLAYER, N).
     *  Does not save. */
    void internalAddSpot(Side player, int n) {
        assert isLegal(player, n);
        int ogSpots = board().get(n).getSpots();
        int newSpots = ogSpots + 1;
        _gameBoard.set(n, Square.square(player, newSpots));
        if (newSpots > neighbors(n)) {
            jump(n);
        }
    }

    /** Set the square at row R, column C to NUM spots (0 <= NUM), and give
     *  it color PLAYER if NUM > 0 (otherwise, white). */
    void set(int r, int c, int num, Side player) {
        internalSet(r, c, num, player);
        announce();
    }

    /** Set the square at row R, column C to NUM spots (0 <= NUM), and give
     *  it color PLAYER if NUM > 0 (otherwise, white).  Does not announce
     *  changes. */
    private void internalSet(int r, int c, int num, Side player) {
        internalSet(sqNum(r, c), num, player);
    }

    /** Set the square #N to NUM spots (0 <= NUM), and give it color PLAYER
     *  if NUM > 0 (otherwise, white). Does not announce changes. */
    private void internalSet(int n, int num, Side player) {
        assert num >= 0;
        if (num == 0) {
            _gameBoard.set(n, Square.square(WHITE, 1));
        } else {
            _gameBoard.set(n, Square.square(player, num));
        }
    }

    /** Undo the effects of one move (that is, one addSpot command).  One
     *  can only undo back to the last point at which the undo history
     *  was cleared, or the construction of this Board. */
    void undo() {
        _numMoves -= 1;
        _history.pop();
        copyGameBoard(_history.peek().board(), _gameBoard);
        if (_gameOver) {
            _gameOver = false;
        }
    }

    /** Record a move into undo history. */
    private void save() {
        if (history().size() == 0) {
            _history.push(new Board(this));
        }
        _history.push(new Board(this));
    }

    /** Resets a board's undo history. */
    private void restart() {
        _history = new Stack<Board>();
        save();
    }

    /** History: a Stack of boards that represent past boards where the
     *  first index is the starting board configuration and the last index is
     *  the most recent move recorded. */
    private Stack<Board> _history = new Stack<Board>();

    /** returns history ArrayList as an object array. */
    public Stack<Board> history() {
        return _history;
    }

    /** Index of History: index of the most recent history in history
     *  Arraylist. */
    private int _historyIndex;

    /** Add DELTASPOTS spots of side PLAYER to row R, column C,
     *  updating counts of numbers of squares of each color. */
    private void simpleAdd(Side player, int r, int c, int deltaSpots) {
        internalSet(r, c, deltaSpots + get(r, c).getSpots(), player);
    }

    /** Add DELTASPOTS spots of color PLAYER to square #N,
     *  updating counts of numbers of squares of each color. */
    private void simpleAdd(Side player, int n, int deltaSpots) {
        internalSet(n, deltaSpots + get(n).getSpots(), player);
    }

    /** Used in jump to keep track of squares needing processing.  Allocated
     *  here to cut down on allocations. */
    private final ArrayDeque<Integer> _workQueue = new ArrayDeque<>();

    /** Do all jumping on this board, assuming that initially, S is the only
     *  square that might be over-full. */
    private void jump(int S) {
        Square square = board().get(S);
        Side side = square.getSide();
        int size = size();
        int[] up = {row(S) - 1, col(S)};
        int[] right = {row(S), col(S) + 1};
        int[] down = {row(S) + 1, col(S)};
        int[] left = {row(S), col(S) - 1};
        int[][] directionals = new int[][]{up, right, down, left};
        _gameBoard.set(S,
                Square.square(side, square.getSpots() - neighbors(S)));
        for (int[] direction: directionals) {
            if (exists(direction[0], direction[1])) {
                Square neighbor = board().get(
                        sqNum(direction[0], direction[1]));
                if (neighbor.getSide() != side) {
                    _gameBoard.set(sqNum(direction[0], direction[1]),
                            Square.square(side, neighbor.getSpots()));
                }
                if (getWinner() != null) {
                    _gameOver = true;
                    break;
                }
                internalAddSpot(side, sqNum(direction[0], direction[1]));
            }
        }
    }

    /** Returns my dumped representation. */
    @Override
    public String toString() {
        Formatter out = new Formatter();
        out.format("===\n    ");
        for (int i = 0; i < _numSquares; i += 1) {
            int spots = board().get(i).getSpots();
            if (board().get(i).getSide() == WHITE) {
                out.format("1-");
            } else if (board().get(i).getSide() == RED) {
                out.format("%dr", spots);
            } else if (board().get(i).getSide() == BLUE) {
                out.format("%db", spots);
            } else {
                out.format("err");
            }
            if ((i + 1) % size() == 0) {
                out.format("\n");
                if ((i + 1) != _numSquares) {
                    out.format("    ");
                }
            } else {
                out.format(" ");
            }
        }
        out.format("===");
        return out.toString();
    }

    /** Returns an external rendition of me, suitable for human-readable
     *  textual display, with row and column numbers.  This is distinct
     *  from the dumped representation (returned by toString). */
    public String toDisplayString() {
        String[] lines = toString().trim().split("\\R");
        Formatter out = new Formatter();
        for (int i = 1; i + 1 < lines.length; i += 1) {
            out.format("%2d %s%n", i, lines[i].trim());
        }
        out.format("  ");
        for (int i = 1; i <= size(); i += 1) {
            out.format("%3d", i);
        }
        return out.toString();
    }

    /** Returns the number of neighbors of the square at row R, column C. */
    int neighbors(int r, int c) {
        int size = size();
        int n;
        n = 0;
        if (r > 1) {
            n += 1;
        }
        if (c > 1) {
            n += 1;
        }
        if (r < size) {
            n += 1;
        }
        if (c < size) {
            n += 1;
        }
        return n;
    }

    /** Returns the number of neighbors of square #N. */
    int neighbors(int n) {
        return neighbors(row(n), col(n));
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Board)) {
            return false;
        } else {
            Board B = (Board) obj;
            return this == obj;
        }
    }

    @Override
    public int hashCode() {
        return numPieces();
    }

    /** Set my notifier to NOTIFY. */
    public void setNotifier(Consumer<Board> notify) {
        _notifier = notify;
        announce();
    }

    /** Take any action that has been set for a change in my state. */
    private void announce() {
        _notifier.accept(this);
    }

    /** A notifier that does nothing. */
    private static final Consumer<Board> NOP = (s) -> { };

    /** A read-only version of this Board. */
    private ConstantBoard _readonlyBoard;

    /** Use _notifier.accept(B) to announce changes to this board. */
    private Consumer<Board> _notifier;
}
