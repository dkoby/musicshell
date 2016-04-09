/*
 *
 */
package mshell.util;


/*
 *
 */
public class DPrint {
    /**
     * Levels of debug
     * 0 - no  debug messages
     * 6 - all debug messages
     */
    private static int level = 2;
    public enum Level {
        NONE      (0),
        EXCEPTION (1),
        VERBOSE4  (2),
        VERBOSE3  (3),
        VERBOSE2  (4),
        VERBOSE1  (5),
        VERBOSE0  (6);

        private final int value;
        Level(int value) {
            this.value = value;
        }
        public int getValue() {
            return value;
        }
    }
    /**
     *
     */
    public static void hex(byte[] data) {
        int i = 0;
        for (byte b: data) {
            if ((i % 8) == 0)
            {
                System.out.format(" ");
                if ((i % 16) == 0)
                {
                    System.out.format("%n");
                    if ((i % 256) == 0)
                    {
                        System.out.format("------: ");
                        for (int k = 0; k < 16; k++)
                            System.out.format("%02X%s", k, k == 7 ? "  " : "-");
                        System.out.format("--%n");
                    }
                    System.out.format("%06X: ", i);
                }
            }

            System.out.format("%02X ", b);
            i++;
        }
        System.out.println();
    }
    /**
     *
     */
    public static void format(Level plevel, String format, Object... args)
    {
        if (DPrint.level == 0)
            return;  
        if (plevel.getValue() <= DPrint.level)
            System.out.format(format, (Object[])args);
    }
    /**
     * Print formatted message, if level is zero output nothing
     */
    public static void format(String format, Object... args)
    {
        if (DPrint.level == 0)
            return;  
        System.out.format(format, (Object[])args);
    }

    /**
     *
     */
    public static void stackTrace() {
        int level = 0;
        System.out.println("--- trace begin --- ");
        for (StackTraceElement st: Thread.currentThread().getStackTrace())
        {
            if (level > 1)
                System.out.println(st);
            else
                level++;
        }
        System.out.println("--- trace end --- ");

        System.out.println("Press Enter");
        try {
            System.in.read();
        } catch (Exception e) {};
    }
    /**
     * Used to mark some dirty code
     */
    @Deprecated
    public static void warning(String msg) {
        System.out.println("Warning:  " + msg);
    }
};

