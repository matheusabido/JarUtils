package teste;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class JarUtils {
	
	// based on https://bukkit.org/threads/tutorial-use-external-library-s-with-your-plugin.103781/
	
    private static boolean RUNNING_FROM_JAR = false;
    
    static {
        URL resource = JarUtils.class.getClassLoader().getResource("plugin.yml");
        if (resource != null) {
            RUNNING_FROM_JAR = true;
        }
    }
    
    private static JarFile getRunningJar() {
    	try {
    		if (!RUNNING_FROM_JAR) {
        		return null;
        	}
        	String path = new File(JarUtils.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getAbsolutePath();
        	path = URLDecoder.decode(path, "UTF-8");
        	return new JarFile(path);
    	} catch (IOException ex) {
    		ex.printStackTrace();
    	}
    	return null;
    }
    
    private static boolean extractFromJar(String fileName, String dest) throws IOException {
        if (getRunningJar() == null) return false;
        File file = new File(dest);
        if (file.isDirectory()) {
            file.mkdir();
            return false;
        }
        if (!file.exists()) file.getParentFile().mkdirs();
 
        JarFile jar = getRunningJar();
        Enumeration<JarEntry> e = jar.entries();
        while (e.hasMoreElements()) {
            JarEntry je = e.nextElement();
            if (!je.getName().contains(fileName)) continue;
            InputStream in = new BufferedInputStream(jar.getInputStream(je));
            OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
            copyInputStream(in, out);
            jar.close();
            return true;
        }
        jar.close();
        return false;
    }
 
    private static void copyInputStream(InputStream in, OutputStream out) throws IOException {
        try {
            final byte[] buff = new byte[4096];
            int n;
            while ((n = in.read(buff)) > 0) {
                out.write(buff, 0, n);
            }
        } finally {
            out.flush();
            out.close();
            in.close();
        }
    }
    
    private static URL getJarUrl(File file) throws IOException {
        return new URL("jar:" + file.toURI().toURL().toExternalForm() + "!/");
    }
    
    private static void addClassPath(URL url) throws IOException {
        URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        Class<URLClassLoader> sysclass = URLClassLoader.class;
        try {
            Method method = sysclass.getDeclaredMethod("addURL", new Class[] { URL.class });
            method.setAccessible(true);
            method.invoke(sysloader, new Object[] { url });
        } catch (final Throwable t) {
            t.printStackTrace();
            throw new IOException("Error adding " + url + " to system classloader");
        }
    }
    
    public static void loadLibs(JavaPlugin plugin, String... libsName) {
    	try {
            File[] libs = new File[libsName.length];
            for (int i = 0; i < libsName.length; i++) libs[i] = new File(plugin.getDataFolder() + "/libs", libsName[i]);
            for (final File lib : libs) {
                if (!lib.exists()) {
                    JarUtils.extractFromJar(lib.getName(), lib.getAbsolutePath());
                }
            }
            for (final File lib : libs) {
                if (!lib.exists()) {
                    plugin.getLogger().warning("Could not find lib: " + lib.getName());
                    Bukkit.getServer().getPluginManager().disablePlugin(plugin);
                    return;
                }
                addClassPath(JarUtils.getJarUrl(lib));
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
	
}