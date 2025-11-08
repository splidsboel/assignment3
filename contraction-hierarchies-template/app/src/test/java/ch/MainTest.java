package ch;

import static org.junit.Assert.assertThrows;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Scanner;

import org.junit.Test;

public class MainTest {

    @Test
    public void testReadGraphFailsWhenEdgesMissing() throws Exception {
        String truncated = ""
                + "2 2\n"
                + "1 0 0\n"
                + "2 1 1\n"
                + "1 2 5\n";

        assertThrows(IllegalStateException.class, () -> invokeReadGraph(truncated));
    }

    private static Graph invokeReadGraph(String data) throws Exception {
        Method method = Main.class.getDeclaredMethod("readGraph", Scanner.class);
        method.setAccessible(true);
        try (Scanner sc = new Scanner(data)) {
            try {
                return (Graph) method.invoke(null, sc);
            } catch (InvocationTargetException ex) {
                Throwable cause = ex.getCause();
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                }
                if (cause instanceof Error) {
                    throw (Error) cause;
                }
                throw new RuntimeException(cause);
            }
        }
    }
}
