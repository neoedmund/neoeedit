package neoe . ne ;

import java . awt . RenderingHints ;

/*not used?*/
public class EditorPanelConfig {
	public String [ ] disabledCommand = { } ;
	public String [ ] disabledFeature = { } ;

	enum Feature {
		editable , showLineNo , showTopBar
	}

	public static Object VALUE_TEXT_ANTIALIAS
	= RenderingHints . VALUE_TEXT_ANTIALIAS_LCD_HRGB ;
}
