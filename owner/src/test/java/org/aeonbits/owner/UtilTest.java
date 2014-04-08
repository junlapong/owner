/*
 * Copyright (c) 2012-2014, Luigi R. Viggiano
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package org.aeonbits.owner;

import org.aeonbits.owner.Util.SystemProvider;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import static java.io.File.createTempFile;
import static org.aeonbits.owner.Util.unreachableButCompilerNeedsThis;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * This class contains tests for the {@link Util} class as well utility methods used for test classes.
 *
 * @author Luigi R. Viggiano
 */
public class UtilTest {

    public static SystemProvider setSystem(Object system) {
        SystemProvider save = Util.system;
        Util.system = (SystemProvider)system;
        return save;
    }

    public static Properties getSystemProperties() {
        return Util.system().getProperties();
    }

    public SystemProvider system() {
        return Util.system();
    }

    @Test
    public void testReverse() {
        Integer[] i = {1, 2, 3, 4, 5};
        Integer[] result = Util.reverse(i);
        assertTrue(Arrays.equals(new Integer[] {1, 2, 3, 4, 5}, i));
        assertTrue(Arrays.equals(new Integer[] {5, 4, 3, 2, 1}, result));
    }

    @Test
    public void testIgnore() {
        Object result = ignore();
        assertNull(result);
    }

    @Test
    public void testUnreachable() {
        try {
            unreachableButCompilerNeedsThis();
        } catch (AssertionError err) {
            assertEquals("this code should never be reached", err.getMessage());
        }
    }

    public static void save(File target, Properties p) throws IOException {
        File parent = target.getParentFile();
        parent.mkdirs();
        if (isWindows()) {
            store(target, p);
        } else {
            File tempFile = createTempFile(target.getName(), ".temp", parent);
            store(tempFile, p);
            rename(tempFile, target);
        }
    }

    private static void store(File target, Properties p) throws IOException {
        OutputStream out = new FileOutputStream(target);
        try {
            store(out, p);
        } finally {
            out.close();
        }
    }

    private static void store(OutputStream out, Properties p) throws IOException {
        p.store(out, "saved for test");
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().indexOf("win") >= 0;
    }

    public static void delete(File target) {
        target.delete();
    }

    public static void saveJar(File target, String entryName, Properties props) throws IOException {
        File parent = target.getParentFile();
        parent.mkdirs();
        storeJar(target, entryName, props);
    }

    private static void rename(File source, File target) throws IOException {
        if (!source.renameTo(target))
            throw new IOException(String.format("Failed to overwrite %s to %s", source.toString(), target.toString()));
    }

    private static void storeJar(File target, String entryName, Properties props) throws IOException {
        byte[] bytes = toBytes(props);
        InputStream input = new ByteArrayInputStream(bytes);
        JarOutputStream output = new JarOutputStream(new FileOutputStream(target));
        try {
            ZipEntry entry = new ZipEntry(entryName);
            output.putNextEntry(entry);
            byte[] buffer = new byte[4096];
            int size;
            while ((size = input.read(buffer)) != -1)
                output.write(buffer, 0, size);
        } finally {
            input.close();
            output.close();
        }
    }

    private static byte[] toBytes(Properties props) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            store(out, props);
            return out.toByteArray();
        } finally {
            out.close();
        }
    }

    public static void debug(String format, Object... args) {
        if (Boolean.getBoolean("debug"))
            System.out.printf(format, args);
    }

    public static <T> T  ignore() {
        return Util.ignore();
    }

    public static File fileFromURL(String spec) throws MalformedURLException {
        return Util.fileFromURL(spec);
    }

    public static String getSystemProperty(String key) {
        return Util.system().getProperty(key);
    }

    public static String getenv(String home) {
        return Util.system().getenv().get(home);
    }

    public static Map<String, String> getenv() {
        return Util.system().getenv();
    }

    public static interface MyCloneable extends Cloneable {
        // for some stupid reason java.lang.Cloneable doesn't define this method...
        public Object clone() throws CloneNotSupportedException;
    }

    @SuppressWarnings("unchecked")
    public static <T extends MyCloneable> T[] newArray(int size, T cloneable) throws CloneNotSupportedException {
        Object array = Array.newInstance(cloneable.getClass(), size);
        Array.set(array, 0, cloneable);
        for (int i = 1; i < size; i++)
            Array.set(array, i, cloneable.clone());
        return (T[]) array;
    }

    public static boolean eq(Object o1, Object o2) {
        return Util.eq(o1, o2);
    }

    @Test
    public void testExpandUserHomeOnUnix() {
        SystemProvider save = UtilTest.setSystem(new SystemProviderForTest(
                new Properties() {{
                    setProperty("user.home", "/home/john");
                }},  new HashMap<String, String>()
        ));

        try {
            assertEquals("/home/john", Util.expandUserHome("~"));
            assertEquals("/home/john/foo/bar/", Util.expandUserHome("~/foo/bar/"));
            assertEquals("file:/home/john/foo/bar/", Util.expandUserHome("file:~/foo/bar/"));
            assertEquals("jar:file:/home/john/foo/bar/", Util.expandUserHome("jar:file:~/foo/bar/"));

            assertEquals("/home/john\\foo\\bar\\", Util.expandUserHome("~\\foo\\bar\\"));
            assertEquals("file:/home/john\\foo\\bar\\", Util.expandUserHome("file:~\\foo\\bar\\"));
            assertEquals("jar:file:/home/john\\foo\\bar\\", Util.expandUserHome("jar:file:~\\foo\\bar\\"));
        } finally {
            UtilTest.setSystem(save);
        }
    }

    @Test
    public void testExpandUserHomeOnWindows() {
        SystemProvider save = UtilTest.setSystem(new SystemProviderForTest(
                new Properties() {{
                    setProperty("user.home", "C:\\Users\\John");
                }}, new HashMap<String, String>()
        ));
        try {
            assertEquals("C:\\Users\\John", Util.expandUserHome("~"));
            assertEquals("C:\\Users\\John/foo/bar/", Util.expandUserHome("~/foo/bar/"));
            assertEquals("file:C:\\Users\\John/foo/bar/", Util.expandUserHome("file:~/foo/bar/"));
            assertEquals("jar:file:C:\\Users\\John/foo/bar/", Util.expandUserHome("jar:file:~/foo/bar/"));

            assertEquals("C:\\Users\\John\\foo\\bar\\", Util.expandUserHome("~\\foo\\bar\\"));
            assertEquals("file:C:\\Users\\John\\foo\\bar\\", Util.expandUserHome("file:~\\foo\\bar\\"));
            assertEquals("jar:file:C:\\Users\\John\\foo\\bar\\", Util.expandUserHome("jar:file:~\\foo\\bar\\"));
        } finally {
            UtilTest.setSystem(save);
        }
    }

}
