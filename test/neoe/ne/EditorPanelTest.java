package neoe.ne;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
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

}
