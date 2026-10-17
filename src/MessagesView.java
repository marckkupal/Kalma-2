import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
/** Guidance - Messages (Figma frame): "Inbox" list. Click a message to read it and mark it as read. */
class MessagesView extends View {
    MessagesView(MainWindow w) {
        super(w);
        List<Message> list = store().messagesFor(user().getId());
        RoundPanel inbox = Staff.card("\uD83D\uDCAC  Inbox", "Mark all as read", () -> {
            list.forEach(Message::markRead);
            window.refresh();
        });
        if (list.isEmpty()) inbox.add(Staff.note("No messages yet."));
        for (Message m : list) {
            JComponent right = m.isRead() ? Staff.note(DataStore.ago(m.getAt()))
                    : new Pill("New", Brand.YELLOW, Brand.INK);
            RoundPanel r = Staff.personRow(false, store().initialsOf(m.getFromId()),
                    store().nameOf(m.getFromId()), m.getText(), right);
            Staff.click(r, () -> {
                m.markRead();
                window.message("From: " + store().nameOf(m.getFromId()) + " (" + store().courseOf(m.getFromId()) + ")\n"
                        + DataStore.ago(m.getAt()) + "\n\n" + m.getText()
                        + "\n\nReply in person or by school email.");
                window.refresh();
            });
            inbox.add(r);
            inbox.add(Staff.gap(6));
        }
        put(Staff.header("Messages", "Reply to students who reached out to the Guidance Office.",
                () -> window.open("Dashboard")), inbox);
    }
}
