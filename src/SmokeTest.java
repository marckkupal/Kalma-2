import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.*;

/**
 * Debug helper: opens every page for every role and reports which ones throw.
 * Run:  ./run-smoketest.sh   (needs a display; on a headless Linux box use xvfb-run)
 * A FAIL line names the page (and so the View class) that broke, plus the first stack frames.
 */
public class SmokeTest {
    public static void main(String[] args) throws Exception {
        final int[] fails = {0};
        SwingUtilities.invokeAndWait(() -> {
            Theme.install();
            DataStore.getInstance().seed();
            MainWindow w = new MainWindow();
            w.showSplash();
            check("Splash", w, fails, () -> w.showSplash());
            check("Tutorial", w, fails, () -> w.showTutorial());
            check("Login", w, fails, () -> w.showLogin());
            for (String email : new String[]{"student@tip.edu.ph", "admin@tip.edu.ph", "guidance@tip.edu.ph"}) {
                User u = DataStore.getInstance().findByEmail(email).orElseThrow();
                check(u.getRole() + " enter", w, fails, () -> w.enter(u));
                List<String> keys = new ArrayList<>(Arrays.asList(u.getMenu()));
                if (u.getRole().equals("student")) keys.add("Book Consultation");
                for (String key : keys) check(u.getRole() + " / " + key, w, fails, () -> w.open(key));
                if (u.getRole().equals("student"))
                    for (Community c : DataStore.getInstance().getCommunities())
                        check("student / Peer Support / " + c.name(), w, fails, () -> w.openCommunity(c.id()));
            }
        });
        System.out.println(fails[0] == 0 ? "ALL PAGES OK" : fails[0] + " PAGE(S) FAILED");
        System.exit(fails[0] == 0 ? 0 : 1);
    }

    private static void check(String label, MainWindow w, int[] fails, Runnable action) {
        try {
            action.run();
            w.setSize(1360, 780);
            w.getContentPane().doLayout();
            BufferedImage img = new BufferedImage(1360, 780, BufferedImage.TYPE_INT_ARGB);
            w.getContentPane().setSize(1360, 780);
            w.getContentPane().validate();
            w.getContentPane().paint(img.createGraphics());
            System.out.println("OK    " + label);
        } catch (Throwable t) {
            fails[0]++;
            System.out.println("FAIL  " + label + "  ->  " + t);
            StackTraceElement[] st = t.getStackTrace();
            for (int i = 0; i < Math.min(4, st.length); i++) System.out.println("        at " + st[i]);
        }
    }
}
