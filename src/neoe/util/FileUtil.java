package neoe.util;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

public class FileUtil {

	public static InputStream getFileInputStream(String fn) {
		return FileUtil.class.getClassLoader().getResourceAsStream(fn);
	}

	public static BufferedReader getBufferedReader(String fn, String enc)
			throws IOException {
		InputStream in = getFileInputStream(fn);
		return new BufferedReader(new InputStreamReader(in, enc));
	}

	public static BufferedReader getRawBufferedReader(String fn, String enc)
			throws UnsupportedEncodingException, FileNotFoundException {
		InputStream in = new FileInputStream(fn);
		return new BufferedReader(new InputStreamReader(in, enc));
	}

	public static void copy(InputStream in, OutputStream outstream)
			throws IOException {
		BufferedOutputStream out = new BufferedOutputStream(outstream);
		byte[] buf = new byte[1024 * 10];
		int len;
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		in.close();
		out.close();
	}

	public static void copy2(InputStream in, OutputStream outstream)
			throws IOException {
		BufferedOutputStream out = new BufferedOutputStream(outstream);
		byte[] buf = new byte[1024 * 10];
		int len;
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		out.flush();
	}

	public static void save(byte[] bs, String fn) throws IOException {
		new File(fn).getParentFile().mkdirs();
		BufferedOutputStream out = new BufferedOutputStream(
				new FileOutputStream(fn));
		out.write(bs);
		out.close();
	}

	public static void pass(InputStream in, OutputStream out, long total)
			throws IOException {
		byte[] buf = new byte[1024 * 10];
		int len;
		long sum = 0;
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
			sum += len;
			if (sum >= total) {
				// System.out.println("read finish");
				break;
			}
		}
		out.flush();
	}

	public static void pass(InputStream in, OutputStream out)
			throws IOException {
		byte[] buf = new byte[1024 * 10];
		int len;
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		out.flush();
	}

	public static void copy(File from, File to) throws IOException {
		FileInputStream in = new FileInputStream(from);
		FileOutputStream out = new FileOutputStream(to);
		copy(in, out);
		in.close();
		out.close();
	}

	public static String readString(InputStream ins, String enc)throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(ins, enc));
		char[] buf = new char[1000];
		int len;
		StringBuffer sb =new StringBuffer();
		while((len=in.read(buf))>0){
			sb.append(buf,0,len);
		}
		in.close();
		return sb.toString();
	}
}
