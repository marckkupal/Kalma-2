import javax.swing.*;
/**
 * Kalma - Peer-Support Wellness Companion (Java Swing).
 * Flow: Splash -> Tutorial (general, 3 steps) -> Login (Student / Admin / Guidance) -> Home.
 * Admin and Guidance use the dark-sidebar dashboards from the Figma frames "04 Admin" and "04 Guidance".
 * Guidance pages Appointments, Students, Messages and Resources follow the "Guidance - ..." frames.
 * Student pages Resources, Guidance Office, Profile and Possible Solutions follow the "Student - ..." frames.
 *
 * Compile and run:  ./compile.sh && ./run.sh   (or: javac -d out src/*.java && java -cp out Kalma)
 * Debug:             ./run-smoketest.sh  opens every page for every role and names the one that fails.
 */
public class Kalma {
    static final String NOTE = "Kalma is not a medical diagnostic system, a crisis-detection system, "
            + "or a replacement for professional counseling. Results are informational only.";
    static final String BREATHING = "Breathe in for 4 counts, hold for 4, breathe out for 6.\nRepeat for five minutes.";
    static final String BREATHING_SHORT = "Breathe in for 4 counts, hold for 4, breathe out for 6.\nRepeat for about two minutes.";
    static final String[] MOODS = {"1 - Very Bad", "2 - Not Good", "3 - Okay", "4 - Good", "5 - Very Good"};

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            DataStore.getInstance().seed();
            new MainWindow().showSplash();
        });
    }
}
