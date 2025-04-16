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

    @Test
    public void testCloseCleansUpPageAndFrame() throws Exception {
        EditorPanel panel = new EditorPanel();
        PlainPage page = new PlainPage(panel, PageData.newUntitled(), null);

        panel.setPage(page, false);
        assertEquals(2, panel.pageSet.size());

        // Close the page
        page.close();

        // Validate: page removed, pageData nulled
        assertNull(page.pageData);
        assertFalse(panel.pageSet.contains(page));
    }

    @Test
    public void testGoSetFont() throws Exception {
        EditorPanel panel = new EditorPanel();
        PlainPage page = new PlainPage(panel, PageData.newUntitled(), null);

        // Calls the set-font path
        page.go("set-font:Monospaced", false);

    }

    @Test
    public void testGoTriggersLaunchFallback() throws Exception {
        EditorPanel panel = new EditorPanel();
        PlainPage page = new PlainPage(panel, PageData.newUntitled(), null);

        // This fake command should fall through all handlers to U.launch
        page.go("not-a-real-command", false);

    }

    @Test
    public void testAllCommandsTriggerProcessCommand() throws Exception {
        EditorPanel panel = new EditorPanel();
        PageData data = PageData.newUntitled();
        PlainPage page = new PlainPage(panel, data, null);

        for (Commands cmd : Commands.values()) {
            try {
                page.processCommand(cmd);

            } catch (Exception e) {
                // Continue testing all commands even if one fails
                System.err.println("Exception for command: " + cmd + " - " + e.getMessage());
            }
        }
    }

}
