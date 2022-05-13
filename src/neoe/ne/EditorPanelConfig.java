package neoe.ne;

import java.awt.RenderingHints;

public class EditorPanelConfig {
  public static final EditorPanelConfig DEFAULT = new EditorPanelConfig();
  public String[] disabledCommand = {};
  public String[] disabledFeature = {};

  enum Feature { editable, showLineNo, showTopBar }

  public static Object VALUE_TEXT_ANTIALIAS =
      RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB;
}
