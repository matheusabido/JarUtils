package br.com.abidux.installer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarUtils {
	
	private static boolean RUNNING_FROM_JAR = JarUtils.class.getClassLoader().getResource("plugin.yml") != null;
	
	private static JarFile getRunningJar() throws IOException {
		if (RUNNING_FROM_JAR) {
			String path = new File(JarUtils.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getAbsolutePath();
			path = URLDecoder.decode(path, "UTF-8");
			return new JarFile(path);
		}
		return null;
	}
	
	private static boolean extract(String fileName, String dest) throws IOException {
		if (getRunningJar() != null) return false;
		File file = new File(dest);
		if (file.isDirectory()) {
			file.mkdir();
			return false;
		}
		if (!file.exists()) file.getParentFile().mkdirs();
		
		JarFile jar = getRunningJar();
		Enumeration<JarEntry> entry = jar.entries();
		while (entry.hasMoreElements()) {
			JarEntry e = entry.nextElement();
			if (!e.getName().contains(fileName)) continue;
			InputStream input = new BufferedInputStream(jar.getInputStream(e));
			OutputStream output = new BufferedOutputStream(new FileOutputStream(file));
			copyInputStream(input, output);
			jar.close();
			return true;
		}
		jar.close();
		return false;
	}
	
	private static void copyInputStream(InputStream input, OutputStream output) throws IOException {
		try {
			byte[] buff = new byte[4096];
			int n;
			while ((n = input.read(buff)) > 0) {
				output.write(buff, 0, n);
			}
		} finally {
			output.flush();
			output.close();
			input.close();
		}
	}
	
	private static URL getJarURL(File file) throws MalformedURLException {
		return new URL("jar:" + file.toURI().toURL().toExternalForm() + "!/");
	}
	
	private static void addClassPath(URL url) {
		URLClassLoader uc = (URLClassLoader) ClassLoader.getSystemClassLoader();
		Class<URLClassLoader> sysclass = URLClassLoader.class;
		try {
			Method method = sysclass.getDeclaredMethod("addURL", new Class[] {URL.class});
			method.setAccessible(true);
			method.invoke(uc, new Object[] {url});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void installLibs(String path, String... libsName) {
		try {
			File[] libs = new File[libsName.length];
			for (int i = 0; i < libs.length; i++) libs[i] = new File(path, libsName[i]);
			for (File lib : libs) if (!lib.exists()) extract(lib.getName(), lib.getAbsolutePath());
			for (File lib : libs) {
				if (!lib.exists()) continue;
				addClassPath(getJarURL(lib));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}