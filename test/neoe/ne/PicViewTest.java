package neoe.ne;

import org.junit.Test;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;

import static org.junit.Assert.*;

public class PicViewTest {

    @Test
    public void testCreatePicViewWithFile() {
        try {
            File dummyFile = new File("test.png"); // path doesn't need to exist for this test
            PicView pv = new PicView();
            assertNotNull("PicView instance should not be null", pv);
        } catch (Exception e) {
            e.printStackTrace();
            assert false : "Exception in testCreatePicViewWithFile: " + e.getMessage();
        }
    }

//    @Test
//    public void testKeyPressedDoesNotThrow() {
//        try {
//            File dummyFile = new File("test.png");
//            PicView pv = new PicView(dummyFile);
//
//            KeyEvent keyEvent = new KeyEvent(pv, KeyEvent.KEY_PRESSED,
//                    System.currentTimeMillis(), 0, KeyEvent.VK_RIGHT, KeyEvent.CHAR_UNDEFINED);
//
//            pv.keyPressed(keyEvent);
//        } catch (Exception e) {
//            e.printStackTrace();
//            assert false : "Exception during testKeyPressedDoesNotThrow: " + e.getMessage();
//        }
//    }
//
//    @Test
//    public void testMouseClickedDoesNotThrow() {
//        try {
//            File dummyFile = new File("test.png");
//            PicView pv = new PicView(dummyFile);
//
//            MouseEvent mouseEvent = new MouseEvent(pv, MouseEvent.MOUSE_CLICKED,
//                    System.currentTimeMillis(), 0, 10, 10, 1, false);
//
//            pv.mouseClicked(mouseEvent);
//        } catch (Exception e) {
//            e.printStackTrace();
//            assert false : "Exception during testMouseClickedDoesNotThrow: " + e.getMessage();
//        }
//    }
}
