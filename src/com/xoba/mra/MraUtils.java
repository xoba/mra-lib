package com.xoba.mra;

import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Toolkit;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.Map.Entry;

public class MraUtils {

    private static final ILogger logger = LogFactory.getDefault().create();

    private MraUtils() {
    }

    public static final String US_ASCII = "US-ASCII";

    public static Dimension getMaxWindowDimension(double fraction) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gs = ge.getScreenDevices();
        int height = new Double(fraction * gs[0].getDisplayMode().getHeight()).intValue();
        int width = new Double(fraction * gs[0].getDisplayMode().getWidth()).intValue();
        return new Dimension(width, height);
    }

    public static String getRandomMD5Hash(int bytesOfRandomness) {
        byte[] buf = new byte[bytesOfRandomness];
        new Random().nextBytes(buf);
        return md5Hash(buf);
    }

    public static String md5Hash(String buf) {
        try {
            return md5Hash(buf.getBytes(US_ASCII));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] md5HashStringToBytes(String buf) {
        try {
            return md5HashBytesToBytes(buf.getBytes(US_ASCII));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String md5Hash(File in) throws IOException {
        return hash(new BufferedInputStream(new FileInputStream(in)), "MD5");
    }

    public static String md5Hash(InputStream in) throws IOException {
        return hash(in, "MD5");
    }

    public static String hash(File f, String algo) throws IOException {
        return hash(new BufferedInputStream(new FileInputStream(f), 65536), algo);
    }

    public static String hash(File f, MessageDigest md) throws IOException {
        return hash(new BufferedInputStream(new FileInputStream(f), 65536), md);
    }

    public static String hash(InputStream in, String algo) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance(algo);
            return hash(in, md);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public final static String hash(final InputStream in, final MessageDigest md) throws IOException {
        try {
            boolean done = false;
            final byte[] buf = new byte[65536];
            while (!done) {
                final int length = in.read(buf);
                if (length <= 0) {
                    done = true;
                } else {
                    md.update(buf, 0, length);
                }
            }
            return convertToHex(md.digest());
        } finally {
            in.close();
        }
    }

    public static interface IHashWithLength {

        public String getHash();

        public long getByteCount();
    }

    public static byte[] md5HashBytesToBytes(byte[] buf) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(buf);
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] md5HashBytesToBytes(byte[] buf, int offset, int length) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(buf, offset, length);
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static String md5Hash(byte[] buf) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(buf);
            return convertToHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static String convertToHex(final byte[] buf) {
        if (buf == null) {
            return null;
        }
        final StringBuffer out = new StringBuffer();
        final int n = buf.length;
        for (int i = 0; i < n; i++) {
            out.append(convertLowerBitsToHex((buf[i] >> 4) & 0x0f));
            out.append(convertLowerBitsToHex(buf[i] & 0x0f));
        }
        return out.toString();
    }

    public final static byte[] convertFromHex(final String hex) {
        final int n = hex.length() / 2;
        byte[] out = new byte[n];
        for (int i = 0; i < n; i++) {
            out[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        return out;
    }

    public static boolean isEven(int x) {
        return (x & 1) == 0;
    }

    public static boolean isOdd(int x) {
        return !isEven(x);
    }

    private static char convertLowerBitsToHex(int b) {
        switch (b) {
        case 0:
            return '0';
        case 1:
            return '1';
        case 2:
            return '2';
        case 3:
            return '3';
        case 4:
            return '4';
        case 5:
            return '5';
        case 6:
            return '6';
        case 7:
            return '7';
        case 8:
            return '8';
        case 9:
            return '9';
        case 10:
            return 'a';
        case 11:
            return 'b';
        case 12:
            return 'c';
        case 13:
            return 'd';
        case 14:
            return 'e';
        case 15:
            return 'f';
        }
        throw new IllegalArgumentException("can't convert to hex character: " + b);
    }

    public static double interpolateYFromX(double x, double x0, double x1, double y0, double y1) {

        if (x < x0 || x > x1) {
            throw new IllegalArgumentException("can't extrapolate");
        }
        if (x0 == x1) {
            return y0;
        }
        double dy = y1 - y0;
        double dx = x1 - x0;
        double v = y0 + (dy / dx) * (x - x0);
        if (Double.isNaN(v)) {
            throw new IllegalArgumentException("can't interpolate");
        }

        return v;
    }

    public static String[] splitCSVWithQuoteEscape(String line) {

        SortedSet<Integer> commaIndicies = new TreeSet<Integer>();
        Set<Integer> quoteIndicies = new HashSet<Integer>();
        Map<Integer, Integer> priorQuotes = new HashMap<Integer, Integer>();

        char[] array = line.toCharArray();
        for (int i = 0; i < array.length; i++) {
            switch (array[i]) {
            case ',':
                commaIndicies.add(i);
                break;
            case '"':
            case '\'':
                quoteIndicies.add(i);
                break;
            }
            priorQuotes.put(i, quoteIndicies.size());
        }

        Iterator<Integer> it = commaIndicies.iterator();
        while (it.hasNext()) {
            Integer i = it.next();
            if (isOdd(priorQuotes.get(i))) {
                it.remove();
            }
        }

        Integer[] commas = commaIndicies.toArray(new Integer[commaIndicies.size()]);

        List<String> list = new LinkedList<String>();

        if (commas.length > 0) {
            list.add(line.substring(0, commas[0]));
        } else {
            list.add(line);
        }

        for (int i = 0; i < commas.length - 1; i++) {
            list.add(line.substring(commas[i] + 1, commas[i + 1]));
        }

        if (commas.length > 0) {
            list.add(line.substring(commas[commas.length - 1] + 1));
        }

        return list.toArray(new String[list.size()]);
    }

    public static Point centerLocation(Dimension window, int instance) {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension screenSize = toolkit.getScreenSize();

        return new Point((screenSize.width - window.width) / 2 + 100 * (instance % 3),
                (screenSize.height - window.height) / 2 + 100 * (instance % 3));
    }

    public static Point centerLocation(Dimension window) {
        return centerLocation(window, 0);
    }

    public static String join(String separator, List<? extends Object> items) {
        return join(separator, items.toArray(new Object[items.size()]));
    }

    public static <T> String join(String separator, T... items) {
        return join(separator, 0, items);
    }

    public static <T> String join(String separator, int offset, T... items) {
        StringBuffer buf = new StringBuffer();
        for (int i = offset; i < items.length; i++) {
            buf.append(items[i]);
            if (i < items.length - 1) {
                buf.append(separator);
            }
        }
        return buf.toString();
    }

    public static void copy(InputStream in, File file) throws IOException {
        OutputStream out = new BufferedOutputStream(new FileOutputStream(file), 65536);
        try {
            copy(in, out);
        } finally {
            out.close();
        }
    }

    public static interface ICopyMonitor {
        public void copiedMoreBytes(int n);
    }

    /**
     * copies the entire stream and closes input within "finally" clause
     */
    public static void copy(InputStream in, OutputStream out) throws IOException {
        copy(getReadable(in), getWriteable(out));
    }

    public static void copy(InputStream in, OutputStream out, ICopyMonitor mon) throws IOException {
        copy(getReadable(in), getWriteable(out), mon);
    }

    public static void copy(InputStream in, IWriteable out) throws IOException {
        copy(getReadable(in), out);
    }

    /**
     * copies the entire stream and closes input within "finally" clause
     */
    public final static void copy(final IReadable in, final IWriteable out) throws IOException {
        try {
            boolean done = false;
            final byte[] buf = new byte[65536];
            while (!done) {
                final int length = in.read(buf);
                if (length <= 0) {
                    done = true;
                } else {
                    out.write(buf, 0, length);
                }
            }
        } finally {
            in.close();
        }
    }

    public final static void copy(final IReadable in, final IWriteable out, final ICopyMonitor mon) throws IOException {
        try {
            boolean done = false;
            final byte[] buf = new byte[65536];
            while (!done) {
                final int length = in.read(buf);
                if (length <= 0) {
                    done = true;
                } else {
                    out.write(buf, 0, length);
                    mon.copiedMoreBytes(length);
                }
            }
        } finally {
            in.close();
        }
    }

    public static void copy(InputStream in, RandomAccessFile out) throws IOException {
        copy(in, getWriteable(out));
    }

    /**
     * copies the given length from in to out and DOESN'T close input stream
     */
    public static void copy(InputStream in, OutputStream out, long len) throws IOException {
        copy(getReadable(in), getWriteable(out), len);
    }

    /**
     * copies the given length from in to out and DOESN'T close input
     */
    public static void copy(IReadable in, OutputStream out, long len) throws IOException {
        copy(in, getWriteable(out), len);
    }

    /**
     * copies the given length from in to out and DOESN'T close input stream
     */
    public static void copy(InputStream in, RandomAccessFile out, long len) throws IOException {
        copy(getReadable(in), getWriteable(out), len);
    }

    private static IWriteable getWriteable(final RandomAccessFile raf) {
        return new SimpleWritable2(raf);
    }

    public static IWriteable getWriteable(final OutputStream out) {
        return new SimpleWritable(out);
    }

    public static IReadable getReadable(final RandomAccessFile raf) {
        return new SimpleReadable2(raf);
    }

    private static IReadable getReadable(final InputStream in) {
        return new SimpleReadable(in);
    }

    /**
     * copies the given length from in to out and DOESN'T close input stream
     */
    public static void copy(IReadable in, IWriteable out, long len) throws IOException {
        boolean done = false;
        final byte[] buf = new byte[65536];
        long alreadyCopied = 0;
        while (!done && alreadyCopied < len) {
            final long leftToRead = len - alreadyCopied;
            final long readThisTime = leftToRead > buf.length ? buf.length : leftToRead;
            int length = in.read(buf, 0, (int) readThisTime);
            if (length <= 0) {
                done = true;
            } else {
                alreadyCopied += length;
                out.write(buf, 0, length);
            }
        }
        if (alreadyCopied != len) {
            throw new IOException("couldn't copy " + len + " bytes");
        }
    }

    /**
     * copies the given length from in to out and DOESN'T close input stream
     */
    public static void copy(RandomAccessFile in, OutputStream out, long len) throws IOException {
        copy(getReadable(in), getWriteable(out), len);
    }

    public static void bufferedCopy(InputStream in, OutputStream out) throws IOException {
        copy(new BufferedInputStream(in, 65536), out);
    }

    public static void copy(File from, File to) throws IOException {
        OutputStream out = new BufferedOutputStream(new FileOutputStream(to), 65536);
        try {
            copy(new BufferedInputStream(new FileInputStream(from), 65536), out);
        } finally {
            out.close();
        }
    }

    public static void copy(File from, OutputStream out) throws IOException {
        copy(new BufferedInputStream(new FileInputStream(from), 65536), out);
    }

    public static void copy(File from, OutputStream out, long len) throws IOException {
        InputStream in = new BufferedInputStream(new FileInputStream(from), 65536);
        try {
            copy(in, out, len);
        } finally {
            in.close();
        }
    }

    public static void copy(InputStream in, File file, long len) throws IOException {
        OutputStream out = new BufferedOutputStream(new FileOutputStream(file), 65536);
        try {
            copy(in, out, len);
        } finally {
            out.close();
        }
    }

    public static void copy(File from, RandomAccessFile out) throws IOException {
        copy(new BufferedInputStream(new FileInputStream(from), 65536), out);
    }

    public static <A, B> Map<A, B> sortByValues(Map<A, B> map, final Comparator<B> comp) {
        List<Map.Entry<A, B>> entries = new LinkedList<Entry<A, B>>(map.entrySet());
        Collections.sort(entries, new Comparator<Map.Entry<A, B>>() {
            public int compare(Entry<A, B> o1, Entry<A, B> o2) {
                return comp.compare(o1.getValue(), o2.getValue());
            }
        });
        Map<A, B> out = new LinkedHashMap<A, B>();
        for (Map.Entry<A, B> e : entries) {
            out.put(e.getKey(), e.getValue());
        }
        return out;
    }

    public static <A, B extends Comparable<B>> Map<A, B> sortByComparableValues(Map<A, B> map, boolean ascending) {
        List<Map.Entry<A, B>> entries = new LinkedList<Entry<A, B>>(map.entrySet());
        final int sign = ascending ? 1 : -1;
        Collections.sort(entries, new Comparator<Map.Entry<A, B>>() {
            public int compare(Entry<A, B> o1, Entry<A, B> o2) {
                return sign * o1.getValue().compareTo(o2.getValue());
            }
        });
        Map<A, B> out = new LinkedHashMap<A, B>();
        for (Map.Entry<A, B> e : entries) {
            out.put(e.getKey(), e.getValue());
        }
        return out;
    }

    public static interface IComparableRange<T> {
        public SortedSet<T> getRange();
    }

    private static final double LOG10_2 = Math.log10(2);

    public static final double log2(final double x) {
        return Math.log10(x) / LOG10_2;
    }

    public static final double log2(final Number x) {
        return log2(x.doubleValue());
    }

    public static <T> List<List<T>> splitListIntoRoughlyEqualSizeParts(List<T> list, int parts) {
        if (parts < 1) {
            parts = 1;
        }
        List<List<T>> out = new LinkedList<List<T>>();
        int n = list.size();
        int m = n / parts;
        List<T> current = new LinkedList<T>();
        int count = 0;
        for (T t : list) {
            if (m == count) {
                count = 0;
                if (current.size() > 0) {
                    out.add(current);
                }
                current = new LinkedList<T>();
            }
            current.add(t);
            count++;
        }
        if (current.size() > 0) {
            out.add(current);
        }
        return out;
    }

    public static SortedSet<Double> getLogRange(double min, double max, int n) {
        SortedSet<Double> out = new TreeSet<Double>();
        double logMin = Math.log(min);
        double logMax = Math.log(max);
        double dx = (logMax - logMin) / (n - 1);
        for (int i = 0; i < n; i++) {
            double log = logMin + i * dx;
            out.add(Math.exp(log));
        }
        return out;
    }

    public static SortedSet<Double> getUniformRange(double min, double max, int n) {
        SortedSet<Double> out = new TreeSet<Double>();
        double dx = (max - min) / (n - 1);
        for (int i = 0; i < n; i++) {
            double log = min + i * dx;
            out.add(log);
        }
        return out;
    }

    public static interface ICloseableIterator<E> extends Iterator<E> {

        public void close();

    }

    public static ICloseableIterator<String> lineIteratorForTextFile(final File f) throws IOException {
        return lineIteratorForTextFile(f, 2048, false);
    }

    public static ICloseableIterator<String> lineIteratorForTextFile(final File f, final int bufferSize,
            final boolean deleteWhenClosed) throws IOException {

        final BufferedReader reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(
                new FileInputStream(f), bufferSize)));

        return new ICloseableIterator<String>() {

            private String nextLine;

            private boolean done;

            private void done() {
                try {
                    if (!done) {
                        reader.close();
                    }
                } catch (IOException e) {
                    logger.warnf("can't close reader for file %s: %s", f, e);
                } finally {
                    done = true;
                }
            }

            public boolean hasNext() {
                try {
                    if (done) {
                        return false;
                    }
                    if (nextLine == null) {
                        nextLine = reader.readLine();
                        if (nextLine == null) {
                            done();
                            return false;
                        } else {
                            return true;
                        }
                    } else {
                        return true;
                    }
                } catch (IOException e) {
                    done();
                    throw new RuntimeException(e);
                }
            }

            public String next() {
                try {
                    if (done) {
                        throw new NoSuchElementException();
                    }
                    if (nextLine == null) {
                        nextLine = reader.readLine();
                    }
                    if (nextLine == null) {
                        done();
                        throw new NoSuchElementException();
                    } else {
                        try {
                            return nextLine;
                        } finally {
                            nextLine = null;
                        }
                    }
                } catch (IOException e) {
                    done();
                    throw new NoSuchElementException(e.toString());
                }
            }

            public void remove() {
                throw new UnsupportedOperationException("can't remove from file " + f);
            }

            public void close() {
                try {
                    done();
                } finally {
                    if (deleteWhenClosed) {
                        boolean deleted = f.delete();
                        if (!deleted) {
                            logger.warnf("can't delete file %s", f);
                        }
                    }
                }
            }
        };
    }

    public static InputStream getResourceInputStream(Package p, String name) throws IOException {
        return getResourceURI(p, name).toURL().openStream();
    }

    public static String getResourceAsString(Package p, String name) throws IOException {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        BufferedReader reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(
                getResourceInputStream(p, name))));
        boolean done = false;
        while (!done) {
            String line = reader.readLine();
            if (line == null) {
                done = true;
            } else {
                pw.println(line);
            }
        }
        pw.close();
        return sw.toString();
    }

    public static byte[] getResourceAsBytes(Package p, String name) throws IOException {
        InputStream in = new BufferedInputStream(getResourceInputStream(p, name));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        copy(in, out);
        out.close();
        return out.toByteArray();
    }

    public static URI getResourceURI(Package p, String name) {
        try {
            String resource = (p.getName()).replaceAll("\\.", "/") + "/" + name;
            return MraUtils.class.getClassLoader().getResource(resource).toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static void outputMemoryDebuggingInformation(Object msg) {
        Runtime r = Runtime.getRuntime();
        r.gc();
        long used = r.totalMemory() - r.freeMemory();

        logger.debugf("memory debugging (%s): %,15d total, " + "%,15d max," + " %,15d free, " + "%,15d used", msg, r
                .totalMemory(), r.maxMemory(), r.freeMemory(), used);
    }

    public final static long extractLongValue(final byte[] buf) {
        return (((long) buf[0] << 56) + ((long) (buf[1] & 255) << 48) + ((long) (buf[2] & 255) << 40)
                + ((long) (buf[3] & 255) << 32) + ((long) (buf[4] & 255) << 24) + ((buf[5] & 255) << 16)
                + ((buf[6] & 255) << 8) + ((buf[7] & 255) << 0));
    }

    public final static long extractLongValue(final byte[] readBuffer, final int offset) {
        return (((long) readBuffer[0 + offset] << 56) + ((long) (readBuffer[1 + offset] & 255) << 48)
                + ((long) (readBuffer[2 + offset] & 255) << 40) + ((long) (readBuffer[3 + offset] & 255) << 32)
                + ((long) (readBuffer[4 + offset] & 255) << 24) + ((readBuffer[5 + offset] & 255) << 16)
                + ((readBuffer[6 + offset] & 255) << 8) + ((readBuffer[7 + offset] & 255) << 0));
    }

    public static final int compareLongs(final long a, final long b) {
        if (a < b) {
            return -1;
        } else if (a > b) {
            return +1;
        } else {
            return 0;
        }
    }

    public static final int compareIntegers(final int a, final int b) {
        if (a < b) {
            return -1;
        } else if (a > b) {
            return +1;
        } else {
            return 0;
        }
    }

    public static final int compareDoubles(final double a, final double b) {
        if (a < b) {
            return -1;
        } else if (a > b) {
            return +1;
        } else {
            return 0;
        }
    }

    public final static UUID marshalUUIDFromBytes(final byte[] array) {
        final long msb = extractLongValue(array);
        final long lsb = extractLongValue(array, 8);
        return new UUID(msb, lsb);
    }

    public final static byte[] serialiseUUID(final UUID u) {
        final byte[] out = new byte[16];
        writeLong(u.getMostSignificantBits(), out, 0);
        writeLong(u.getLeastSignificantBits(), out, 8);
        return out;
    }

    public final static void writeLong(final long v, final byte[] writeBuffer, final int offset) {
        writeBuffer[0 + offset] = (byte) (v >>> 56);
        writeBuffer[1 + offset] = (byte) (v >>> 48);
        writeBuffer[2 + offset] = (byte) (v >>> 40);
        writeBuffer[3 + offset] = (byte) (v >>> 32);
        writeBuffer[4 + offset] = (byte) (v >>> 24);
        writeBuffer[5 + offset] = (byte) (v >>> 16);
        writeBuffer[6 + offset] = (byte) (v >>> 8);
        writeBuffer[7 + offset] = (byte) (v >>> 0);
    }

    public static double calcEffectiveNumber(Collection<? extends Number> weights) {
        double m1 = 0;
        double m2 = 0;
        for (Number y : weights) {
            double x = y.doubleValue();
            m1 += Math.abs(x);
            m2 += x * x;
        }
        return m1 * m1 / m2;
    }

}