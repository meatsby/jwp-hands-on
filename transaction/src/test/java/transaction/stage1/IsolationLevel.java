package transaction.stage1;

public enum IsolationLevel {

    READ_UNCOMMITTED(1),
    READ_COMMITTED(2),
    REPEATABLE_READ(4),
    SERIALIZABLE(8),
    ;

    private final int level;

    IsolationLevel(final int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }
}
