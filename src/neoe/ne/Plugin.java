/* neoe */
package neoe.ne;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import neoe.util.FileIterator;

/**
 *
 * @author neoedmund
 */
class Plugin {
    static ClassLoader cl;

    static void load() throws Exception {
        if (cl!=null){
            System.out.println("plugin seems already loaded, pass");
            return;
        }
        String libDir = U.getMyDir()+"/plugins";
            Iterable<File> it = new FileIterator(libDir);
        List<URL> jars = new ArrayList<URL>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        for (File f : it) {
            if (f.isDirectory()) {
                continue;
            }
            if (f.getName().toLowerCase().endsWith(".jar")) {
                System.out.println("add plugin " + f.getAbsolutePath()+" \t "+sdf.format(new Date(f.lastModified())));
                jars.add(f.toURI().toURL());
            }
        }
        System.out.println("added plugin count:" + jars.size());
        cl = new URLClassLoader(jars.toArray(new URL[jars.size()]), Plugin.class
                .getClassLoader());        
        
    }    
}
