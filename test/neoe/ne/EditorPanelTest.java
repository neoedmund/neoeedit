package neoe.ne;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.font.TextHitInfo;
import java.io.File;
import java.io.FileWriter;
import java.text.AttributedCharacterIterator;

import static org.junit.Assert.*;

public class EditorPanelTest {

    @Test
    public void testSetPageStoresPageCorrectly() {
        try {
            EditorPanel panel = new EditorPanel();
            PlainPage page = new PlainPage(panel, PageData.newUntitled(), null);
            panel.setPage(page, false);
            assertEquals("Page should be set properly", page, panel.page);
        } catch (Exception e) {
            e.printStackTrace();
            assert false : "Exception occurred: " + e.getMessage();
        }
    }

    @Test
    public void testKeyPressedAndReleased() {
        try {
            EditorPanel panel = new EditorPanel();

            // Simulate KeyEvent for 'A' key press
            KeyEvent keyEvent = new KeyEvent(panel, KeyEvent.KEY_PRESSED, System.currentTimeMillis(),
                    0, KeyEvent.VK_A, 'A');

            // Call keyPressed
            panel.keyPressed(keyEvent);

            keyEvent = new KeyEvent(panel, KeyEvent.KEY_RELEASED, System.currentTimeMillis(),
                    0, KeyEvent.VK_A, 'A');

            panel.keyReleased(keyEvent);

        } catch (Exception e) {
            e.printStackTrace();
            assert false : "Exception in testKeyPressedAndReleased: " + e.getMessage();
        }
    }

    @Test
    public void testMouseClicked() {
        try {
            EditorPanel panel = new EditorPanel();
            MouseEvent mouseEvent = new MouseEvent(panel, MouseEvent.MOUSE_CLICKED,
                    System.currentTimeMillis(), 0, 20, 20, 1, false);
            panel.mouseClicked(mouseEvent);
        } catch (Exception e) {
            e.printStackTrace();
            assert false : "Exception during testMouseClicked: " + e.getMessage();
        }
    }

    @Test
    public void testKeyTyped() {
        try {
            EditorPanel panel = new EditorPanel();
            KeyEvent keyEvent = new KeyEvent(panel, KeyEvent.KEY_TYPED,
                    System.currentTimeMillis(), 0, KeyEvent.VK_UNDEFINED, 'x');
            panel.keyTyped(keyEvent);
        } catch (Exception e) {
            e.printStackTrace();
            assert false : "Exception during testKeyTyped: " + e.getMessage();
        }
    }

    @Test
    public void testOpenWindowWithJFrame() {
        try {
            EditorPanel panel = new EditorPanel();
            JFrame frame = new JFrame(); // actual frame passed to method

            panel.openWindow(null, frame, frame, null); // call

            assertEquals(frame, panel.frame); // confirm it got set
        } catch (Exception e) {
            e.printStackTrace();
            assert false : "Exception during OpenWindow: " + e.getMessage();
        }
    }

    @Test
    public void testWindowClosed() {
        try {
            EditorPanel panel = new EditorPanel();
            JFrame frame = new JFrame();
            panel.openWindow(null, frame, frame, null);

            // Fire windowClosed manually
            WindowEvent event = new WindowEvent(frame, WindowEvent.WINDOW_CLOSED);
            for (WindowListener wl : frame.getWindowListeners()) {
                wl.windowClosed(event); // trigger
            }

        } catch (Exception e) {
            fail("Exception in testWindowClosed: " + e.getMessage());
        }
    }

    @Test
    public void testStartWindowMoveAndWindowMove() throws Exception {
        EditorPanel panel = new EditorPanel();
        JFrame frame = new JFrame();
        panel.openWindow(null, frame, frame, null);

        // Simulate pressing in window move range
        MouseEvent pressEvent = new MouseEvent(panel, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
                0, 5, 5, 1, false);
        panel.mousePressed(pressEvent);

        // Simulate dragging to move window
        MouseEvent dragEvent = new MouseEvent(panel, MouseEvent.MOUSE_DRAGGED, System.currentTimeMillis(),
                0, 10, 10, 1, false);
        panel.mouseDragged(dragEvent);
    }

    @Test
    public void testStartWindowResizeAndWindowResize() throws Exception {
        EditorPanel panel = new EditorPanel();
        JFrame frame = new JFrame();
        frame.setSize(300, 300); // Ensure some size for bounds
        panel.openWindow(null, frame, frame, null);

        Dimension size = panel.getSize();
        int toolbarOffset = panel.page.toolbarHeight;

        // Simulate pressing in resize range (bottom-right corner)
        MouseEvent pressEvent = new MouseEvent(panel, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
                0, size.width - toolbarOffset + 1, size.height - toolbarOffset + 1, 1, false);
        panel.mousePressed(pressEvent);

        // Simulate dragging to resize window
        MouseEvent dragEvent = new MouseEvent(panel, MouseEvent.MOUSE_DRAGGED, System.currentTimeMillis(),
                0, size.width + 10, size.height + 10, 1, false);
        panel.mouseDragged(dragEvent);
    }

    @Test
    public void testFindAndShowPageWithUntitledPage() throws Exception {
        EditorPanel panel = new EditorPanel();

        String title = panel.page.pageData.title;

        boolean result = panel.findAndShowPage(title, 1, false);

        assertTrue("Should be able to find and show existing untitled page", result);
    }

    @Test
    public void testFindAndShowPageOpensNewWindow() throws Exception {
        EditorPanel panel = new EditorPanel();
        panel.newWindow = true;

        // Save to a temp file to simulate opening
        File tmp = File.createTempFile("test", ".txt");
        tmp.deleteOnExit();

        try (FileWriter writer = new FileWriter(tmp)) {
            writer.write("test\n");
        }

        boolean result = panel.findAndShowPage(tmp.getAbsolutePath(), 1, false);

        assertTrue("Should open new window for file", result);
    }

    @Test
    public void testMouseEnteredAndExited() {
        try {
            EditorPanel panel = new EditorPanel();

            MouseEvent evt = new MouseEvent(panel, MouseEvent.MOUSE_ENTERED,
                    System.currentTimeMillis(), 0, 10, 10, 1, false);
            panel.mouseEntered(evt);

            evt = new MouseEvent(panel, MouseEvent.MOUSE_EXITED,
                    System.currentTimeMillis(), 0, 10, 10, 1, false);
            panel.mouseExited(evt);
        } catch (Exception e) {
            e.printStackTrace();
            assert false : "Exception occurred: " + e.getMessage();
        }
    }

    @Test
    public void testMouseReleased() {
        try {
            EditorPanel panel = new EditorPanel();
            panel.inWindowMove = true;
            panel.inWindowResize = true;

            MouseEvent evt = new MouseEvent(panel, MouseEvent.MOUSE_RELEASED,
                    System.currentTimeMillis(), 0, 10, 10, 1, false);

            panel.mouseReleased(evt);

            assertFalse(panel.inWindowMove);
            assertFalse(panel.inWindowResize);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testMyInputMethodRequestsHandlerMethods() {
        try {

            EditorPanel panel = new EditorPanel();
            EditorPanel.MyInputMethodRequestsHandler handler = panel.new MyInputMethodRequestsHandler();

            // Test all the implemented methods to achieve coverage
            handler.cancelLatestCommittedText(new AttributedCharacterIterator.Attribute[0]);
            handler.getCommittedText(0, 1, new AttributedCharacterIterator.Attribute[0]);
            handler.getCommittedTextLength();
            handler.getInsertPositionOffset();
            handler.getLocationOffset(10, 10);
            handler.getSelectedText(new AttributedCharacterIterator.Attribute[0]);
            handler.getTextLocation(TextHitInfo.leading(0));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



}
