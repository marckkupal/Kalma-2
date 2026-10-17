/** A result band. {@code rank} is 0 (low), 1 (moderate) or 2 (higher) so levels can be compared. */
record ConcernLevel(int rank, String label, String message) {}
