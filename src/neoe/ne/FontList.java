/*
 *  
 */
package neoe.ne;

import java.awt.Font;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author neoe
 */
public class FontList {

    public Font[] font;
    public Object[][] charWidthCaches256 = new Object[256][];
    public Map<Character, Object[] /* Font, Integer */> charWidthCaches = new HashMap();

    public FontList(Font[] font) {
        this.font = font;
    }

    public int getlineHeight() {
        if (font.length > 0) {
            return font[0].getSize();
        }
        return 10;
    }
}
