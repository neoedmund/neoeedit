package neoe.ne;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

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

            // Now test keyReleased with same event
            keyEvent = new KeyEvent(panel, KeyEvent.KEY_RELEASED, System.currentTimeMillis(),
                    0, KeyEvent.VK_A, 'A');

            panel.keyReleased(keyEvent);

            // No assertion needed unless you want to check specific side effects.
            // We're testing for exceptions / errors (coverage and stability).
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

}
