package neoe.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class FileIterator implements Iterable<File> {

	List<File> buf;

	public FileIterator(String dir) {
		buf = new ArrayList<File>();
		File f = new File(dir);
		buf.add(f);
	}

	@Override
	public Iterator<File> iterator() {
		return new Iterator<File>() {

			@Override
			public boolean hasNext() {
				return buf.size() > 0;
			}

			@Override
			public File next() {
				File f = buf.remove(0);
				String name = f.getName();
				if (f.isDirectory() && !name.equals(".svn") && !name.equals(".cvs") && !name.equals(".bzr") && !name.equals(".git")) {
					File[] sub = f.listFiles();
					if (sub != null) {
						buf.addAll(Arrays.asList(sub));
					}
				}
				return f;
			}

			@Override
			public void remove() {
			}
		};
	}

}
