/** A guidance counselor shown in the Guidance Office section of Resources and in booking. */
record CounselorInfo(String id, String name, String role, String email) {
    CounselorInfo(String id, String name) { this(id, name, "Guidance Counselor", "guidance@tip.edu.ph"); }
}
