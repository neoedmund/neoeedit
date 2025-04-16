package neoe.ne;

import org.junit.Test;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import static org.junit.Assert.*;

public class PlainPageTest {

    @Test
    public void testPlainPageCreation() {
        try {
            EditorPanel panel = new EditorPanel();
            PageData data = PageData.newUntitled();
            PlainPage page = new PlainPage(panel, data, null);
            assertNotNull("PlainPage should be instantiated", page);
        } catch (Exception e) {
            e.printStackTrace();
            assert false : "Exception in testPlainPageCreation: " + e.getMessage();
        }
    }

    @Test
    public void testKeyPressed() {
        try {
            EditorPanel panel = new EditorPanel();
            PlainPage page = new PlainPage(panel, PageData.newUntitled(), null);

            KeyEvent keyEvent = new KeyEvent(panel, KeyEvent.KEY_PRESSED,
                    System.currentTimeMillis(), 0, KeyEvent.VK_DOWN, ' ');

            page.keyPressed(keyEvent);
        } catch (Exception e) {
            e.printStackTrace();
            assert false : "Exception in testKeyPressed: " + e.getMessage();
        }
    }

    @Test
    public void testMouseClickedDoesNotThrow() {
        try {
            EditorPanel panel = new EditorPanel();
            PlainPage page = new PlainPage(panel, PageData.newUntitled(), null);

            MouseEvent mouseEvent = new MouseEvent(panel, MouseEvent.MOUSE_CLICKED,
                    System.currentTimeMillis(), 0, 10, 10, 1, false);

            page.mouseClicked(mouseEvent);
        } catch (Exception e) {
            e.printStackTrace();
            assert false : "Exception in testMouseClickedDoesNotThrow: " + e.getMessage();
        }
    }

    @Test
    public void testMouseMoved() {
        try {
            EditorPanel panel = new EditorPanel();
            PlainPage page = new PlainPage(panel, PageData.newUntitled(), null);

            MouseEvent evt = new MouseEvent(panel, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(),
                    0, 100, 100, 0, false);
            page.mouseMoved(evt);

            // There's no exception, and method completes
            assertTrue(true);
        } catch (Exception e) {
            fail("Exception during mouseMoved: " + e.getMessage());
        }
    }

}
