package neoe.ne;

import javax.swing.JPanel;

public class TopCoderEditorPlugin {
	EditPanel editor;

	public JPanel getEditorPanel() {
		try {
			editor = new EditPanel();
			editor.getPage().ui.applyColorMode(1);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return editor;
	}

	public String getSource() {
		if (editor != null) {
			return U.getText(editor.getPage());
		}
		return null;
	}

	public void setSource(String s) {
		if (editor != null) {
			editor.getPage().pageData.setText(s);
			editor.revalidate();
			editor.repaint();
		}
	}
}
