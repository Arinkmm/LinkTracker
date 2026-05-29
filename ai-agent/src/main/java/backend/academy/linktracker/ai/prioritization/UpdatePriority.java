package backend.academy.linktracker.ai.prioritization;

public enum UpdatePriority {
    LOW(0),
    MEDIUM(1),
    HIGH(2);

    private final int rank;

    UpdatePriority(int rank) {
        this.rank = rank;
    }

    public static UpdatePriority max(String left, String right) {
        UpdatePriority leftPriority = valueOf(left);
        UpdatePriority rightPriority = valueOf(right);
        return leftPriority.rank >= rightPriority.rank ? leftPriority : rightPriority;
    }
}
