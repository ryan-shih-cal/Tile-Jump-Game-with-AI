package jump61;

import ucb.junit.textui;

/** The suite of all JUnit tests for the Jump61 program.
 *  @author Ryan Shih
 */
public class UnitTest {

    /** Run the JUnit tests in this package. Add xxxTest.class entries to
     *  the arguments of runClasses to run other JUnit tests. */
    public static void main(String[] ignored) {
        System.exit(textui.runClasses(jump61.BoardTest.class));
        System.exit(textui.runClasses(jump61.AITest.class));
    }

}


