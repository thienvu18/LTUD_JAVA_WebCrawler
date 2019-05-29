package thienvu;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

public class Main {

    private static final String base = "offline";
    private static Set<String> linksToDownload;
    private static int numberOfThread;

    public static synchronized void addLink(String link) {
        linksToDownload.add(link);
    }

    public static synchronized String removeLink() {
        if (linksToDownload.isEmpty()) return "";
        return (String) linksToDownload.toArray()[0];
    }

    public static synchronized void increaseNumberOfThread() {
        numberOfThread++;
    }

    public static synchronized void decreaseNumberOfThread() {
        numberOfThread--;
    }

    public static boolean downloadFile(String url, String filePath, String fileName) {

        new File(filePath).mkdirs();
        File tmpDir = new File(filePath + "/" + fileName);
        if (tmpDir.exists()) return true;

        ReadableByteChannel readableByteChannel = null;
        try {
            readableByteChannel = Channels.newChannel(new URL(url).openStream());
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(filePath + "/" + fileName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }

        FileChannel fileChannel = fileOutputStream.getChannel();
        try {
            fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);

            fileChannel.close();
            fileOutputStream.close();
            readableByteChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public static void test(String urlString) throws IOException {
        final String urlStringTrimmed = urlString.lastIndexOf('/') == urlString.length() - 1 ? urlString.substring(0, urlString.length() - 1) : urlString;
        final URL url = new URL(urlStringTrimmed);
        final String path = url.getPath();
        final String root = path.isEmpty() || !path.contains(".") ? base + path : base + path.substring(0, path.lastIndexOf('/') + 1);
        final String filename = path.contains(".") ? path.substring(path.lastIndexOf('/') + 1) : "index.html";
        final String domain = url.getHost();
        final int degree = root.split("/", -1).length - 1;
        final String relativeNotation = "../".repeat(degree);

        Document doc = Jsoup.connect(url.toString()).get();
        Elements links = doc.select("a[href]");
        Elements media = doc.select("[src]");
        Elements imports = doc.select("link[href]");

        // Xoá cái base để đường dẫn tương đối bắt đầu từ folder hiện tại
        // https://html.com/attributes/base-href/
        if (doc.head().children().select("base").size() > 0) {
            doc.head().children().select("base").remove();
        }

        for (Element src : media) {
            final String fileUri = src.attr("abs:src");

            String fileRelativeUri;
            try {
                URL u = new URL(src.attr("src"));
                fileRelativeUri = u.getPath();
            } catch (Exception ignored) {
                fileRelativeUri = src.attr("src");
            }

            final String fileRelativeUriTrimmed = fileRelativeUri.substring(0, fileRelativeUri.indexOf('?') == -1 ? fileRelativeUri.length() : fileRelativeUri.indexOf('?'));

            String filePath = fileRelativeUri.substring(0, fileRelativeUri.lastIndexOf('/'));
            String fileName = fileRelativeUri.substring(fileRelativeUri.lastIndexOf('/') + 1, fileRelativeUri.indexOf('?') == -1 ? fileRelativeUri.length() : fileRelativeUri.indexOf('?'));

            if (filePath.equals("")) continue;

            if (filePath.charAt(0) == '/') {
                if (filePath.length() >= 2 && filePath.charAt(1) == '/') {
                    src.attr("src", fileRelativeUriTrimmed.replaceFirst("//", relativeNotation));
                    filePath = base + filePath.replaceFirst("/", "");
                } else {
                    src.attr("src", fileRelativeUriTrimmed.replaceFirst("/", relativeNotation));
                    filePath = base + filePath;
                }
            }

            downloadFile(fileUri, filePath, fileName);
        }

        for (Element link : imports) {
            final String fileUri = link.attr("abs:href");

            String fileRelativeUri;
            try {
                URL u = new URL(link.attr("href"));
                fileRelativeUri = u.getPath();
            } catch (Exception ignored) {
                fileRelativeUri = link.attr("href");
            }

            String filePath = fileRelativeUri.substring(0, fileRelativeUri.lastIndexOf('/'));
            String fileName = fileRelativeUri.substring(fileRelativeUri.lastIndexOf('/') + 1, fileRelativeUri.indexOf('?') == -1 ? fileRelativeUri.length() : fileRelativeUri.indexOf('?'));

            if (filePath.charAt(0) == '/') {
                String newLink;

                if (filePath.length() >= 2 && filePath.charAt(1) == '/') {
                    newLink = fileRelativeUri.replaceFirst("//", relativeNotation);
                    filePath = base + filePath.replaceFirst("/", "");
                } else {
                    newLink = fileRelativeUri.replaceFirst("/", relativeNotation);
                    filePath = base + filePath;
                }

                link.attr("href", newLink);
            }

            downloadFile(fileUri, filePath, fileName);
        }

        for (Element link : links) {
            try {
                URL u = new URL(link.attr("abs:href"));
                if (!u.getHost().equals(domain)) {
                    continue;
                }
            } catch (Exception ignored) {
            }

            if (link.attr("href").equals("") || link.attr("href").charAt(0) == '#')
                continue;

            String fileRelativeUri;
            try {
                URL u = new URL(link.attr("href"));
                fileRelativeUri = u.getPath();
            } catch (Exception ignored) {
                fileRelativeUri = link.attr("href");
            }

            if (fileRelativeUri.contains("#")) {
                addLink(link.attr("abs:href").substring(0, fileRelativeUri.lastIndexOf('#')));
            } else {
                addLink(link.attr("abs:href"));
            }

            String filePath;
            if (!fileRelativeUri.contains(".")) {
                filePath = fileRelativeUri;
            } else {
                filePath = fileRelativeUri.substring(0, fileRelativeUri.lastIndexOf('/'));
            }

            if (filePath.isEmpty()) {
                link.attr("href", "./");
            } else if (filePath.charAt(0) == '/') {
                String newLink;

                if (filePath.length() >= 2 && filePath.charAt(1) == '/') {
                    newLink = fileRelativeUri.replaceFirst("//", relativeNotation);
                } else {
                    newLink = fileRelativeUri.replaceFirst("/", relativeNotation);
                }

                if (!newLink.contains(".")) {
                    newLink += "/index.html";
                }

                link.attr("href", newLink);
            }
        }

        new File(root).mkdirs();
        PrintWriter out = new PrintWriter(root + "/" + filename);
        out.println(doc.toString());
        out.close();
    }

    public static void main(String[] args) throws IOException {
        final int maxThreadCount = 5;

        numberOfThread = 0;
        linksToDownload = new HashSet<>();
        addLink("https://www.hcmus.edu.vn");

        while (true) {
            if (numberOfThread == 0 && linksToDownload.size() == 0) break;
            if (numberOfThread > maxThreadCount) continue;

            String link = removeLink();
            if (link.equals("")) continue;

            increaseNumberOfThread();
            Thread thread = new Thread(() -> {
                System.out.println("Thread start");
                try {
                    System.out.println("Downloading " + link);
                    test(link);
                    System.out.println("Done download " + link);
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("Error download " + link);
                }

                decreaseNumberOfThread();
                System.out.println("Thread done");
            });
            thread.start();
        }
    }

    private static void print(String msg, Object... args) {
        System.out.println(String.format(msg, args));
    }

    private static String trim(String s, int width) {
        if (s.length() > width)
            return s.substring(0, width - 1) + ".";
        else
            return s;
    }

}
