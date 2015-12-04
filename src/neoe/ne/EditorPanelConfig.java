package neoe.ne;

public class EditorPanelConfig {
	public static final EditorPanelConfig DEFAULT = new EditorPanelConfig();
	public String[] disabledCommand = {};
	public String[] disabledFeature = {};
	enum Feature{
		editable, showLineNo, showTopBar
	}
}
