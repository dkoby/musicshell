/*
 *
 */
package mshell.view;
/* */

class FileDescription {
    String name;
    Type type;
    /**
     *
     */
    public FileDescription(String name, Type type) {
        this.name = name;
        this.type = type;
    }
    /**
     *
     */
    public enum Type {
        DIR,
        FILE,
    }
}

