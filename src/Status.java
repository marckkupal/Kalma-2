enum Status {
    PENDING("Pending"), CONFIRMED("Confirmed"), COMPLETED("Completed"),
    CANCELLED("Cancelled"), RESCHEDULED("Rescheduled");

    private final String label;
    Status(String label) { this.label = label; }
    @Override public String toString() { return label; }
}
