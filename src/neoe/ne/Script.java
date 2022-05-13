package neoe.ne;

import java.util.List;

/**
 * Script Notes:
 *
 * I found All script sucks except use java itself.
 *
 * Press Alt-J can bring up "script window" for the current page.
 *
 * The script is really a java source file implements interface neoe.ne.Script.
 * The source can use package, but only one java file is allowed.
 *
 * The script will be compiled and executed and deleted on hot.
 *
 * No external library is needed. But the JDK is needed and will be auto
 * detected for use of javac.
 *
 */
public interface Script {
  List<CharSequence> run(List<CharSequence> lines);
}
