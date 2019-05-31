package thienvu;

import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public class Crawler {

    private String base = "offline";
    private Set<String> linksToDownload;
    private Set<String> downloadedLinks;
    private Set<String> errorLinks;
    private int numberOfThread;
    private Ui ui = null;
    private Thread mainThread;

    void start(String baseUrl, String url, int maxThread) {

        mainThread = new Thread(() -> {
            int maxThreadCount;
            String linkToDownload;

            if (url.equals("")) return;
            linkToDownload = url;
            if (!baseUrl.equals("")) {
                base = baseUrl;
            }
            if (maxThread <= 0) {
                maxThreadCount = 1;
            } else maxThreadCount = maxThread;

            numberOfThread = 0;
            linksToDownload = new LinkedHashSet<>();
            downloadedLinks = new HashSet<>();
            errorLinks = new HashSet<>();
            addLink(linkToDownload);

            while (numberOfThread != 0 || getLinksSize() != 0) {
                if (numberOfThread >= maxThreadCount) continue;

                String link = removeLink();
                if (link == null) continue;

                increaseNumberOfThread();
                Thread thread = new Thread(() -> {
                    try {
                        System.out.println("Downloading " + link);
                        crawler(link);
                        System.out.println("Done download " + link);
                    } catch (Exception e) {
//                    e.printStackTrace();
                        setErrorLink(link);
                        System.out.println("Error download " + link);
                    }

                    decreaseNumberOfThread();
                });
                thread.start();
            }

            Iterator iterator = errorLinks.iterator();
            System.out.println("Error link:");
            while (iterator.hasNext()) {
                System.out.println("\t" + iterator.next());
            }
        });
        mainThread.start();
    }

    void stop() {
        try {
            mainThread.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void setUi(Ui ui) {
        this.ui = ui;
    }

    private synchronized void addLink(String link) {
        linksToDownload.add(link);
    }

    private synchronized boolean setDownloadedLink(String link) {
        boolean suc = downloadedLinks.add(link);
        if (suc) {
            ui.updateDownloadedList(link);
        }
        return suc;
    }

    private synchronized boolean setErrorLink(String link) {
        boolean suc = errorLinks.add(link);
        if (suc) {
            ui.updateErrorList(link);
        }
        return suc;
    }

    private synchronized String removeLink() {
        if (linksToDownload.isEmpty()) return null;

        Iterator iterator = linksToDownload.iterator();
        String res = (String) iterator.next();
        iterator.remove();

        return res;
    }

    private synchronized int getLinksSize() {
        return linksToDownload.size();
    }

    private synchronized void increaseNumberOfThread() {
        numberOfThread++;
    }

    private synchronized void decreaseNumberOfThread() {
        numberOfThread--;
    }

    private boolean downloadFile(String url, String filePath, String fileName) {

        new File(filePath).mkdirs();
        File file = new File(filePath + "/" + fileName);
        if (file.exists()) return true;

        ReadableByteChannel readableByteChannel = null;
        try {
            readableByteChannel = Channels.newChannel(new URL(url).openStream());
        } catch (IOException e) {
//            e.printStackTrace();
            return false;
        }

        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(filePath + "/" + fileName);
        } catch (FileNotFoundException e) {
//            e.printStackTrace();
            return false;
        }

        FileChannel fileChannel = fileOutputStream.getChannel();
        try {
            fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);

            fileChannel.close();
            fileOutputStream.close();
            readableByteChannel.close();
        } catch (IOException e) {
//            e.printStackTrace();
            return false;
        }

        return true;
    }

    //Retrun path like "/abc/xyz"
    private String getPathWithoutFilename(String pathWithFilename) {
        if (pathWithFilename.equals("")) return "/";
        if (!pathWithFilename.contains(".")) return pathWithFilename;
        if (pathWithFilename.lastIndexOf("/") == pathWithFilename.indexOf("/")) return "/";
        return pathWithFilename.substring(0, pathWithFilename.lastIndexOf("/"));
    }

    //Retrun filename like "abc.xyz"
    private String getFileName(String pathWithFilename) {
        if (pathWithFilename.equals("") || !pathWithFilename.contains(".")) return "index.html";
        return pathWithFilename.substring(pathWithFilename.lastIndexOf("/") + 1);
    }

    //Delete redundant "/" and "#"
    private String trimUrlString(String urlString) {
        String url = urlString.trim();
        if (url.equals("")) return url;
        if (url.equals("#")) return "";
        if (url.equals("/")) return url;

        if (url.lastIndexOf('#') == url.length() - 1) {
            url = url.substring(0, url.length() - 1);
        }
        if (url.lastIndexOf('/') == url.length() - 1) {
            url = url.substring(0, url.length() - 1);
        }

        return url;
    }

    //Return absolute path like offline/abc/xyz
    private String getAbsoluteLocalFilePath(String basePath, String pathWithoutFilename) {
        return basePath + pathWithoutFilename;
    }

    private String getRelativeNotation(String absolutePath) {
        if (absolutePath.equals("/")) return "./";

        final int degree = absolutePath.split("/", -1).length - 1;
        String ret;

        ret = "../".repeat(degree);

        return ret;
    }

    private void ProcessMedium(String root, Elements medium) {
        final String relativeNotation = getRelativeNotation(root);

        for (Element media : medium) {
            final String mediaUrl = media.attr("abs:src");

            //If mediaUrl is whole url, we just get the path
            String mediaRelativeUrl;
            try {
                URL u = new URL(media.attr("src"));
                mediaRelativeUrl = u.getPath();
            } catch (Exception ignored) {
                final String rawSrc = trimUrlString(media.attr("src"));
                mediaRelativeUrl = rawSrc.substring(0, rawSrc.indexOf('?') == -1 ? rawSrc.length() : rawSrc.indexOf('?'));
            }
            mediaRelativeUrl = mediaRelativeUrl.substring(0, mediaRelativeUrl.indexOf('#') == -1 ? mediaRelativeUrl.length() : mediaRelativeUrl.indexOf('#'));

            String filePath = getPathWithoutFilename(mediaRelativeUrl);
            String fileName = getFileName(mediaRelativeUrl);

            if (filePath.charAt(0) == '/') {
                if (filePath.length() >= 2 && filePath.charAt(1) == '/') {
                    media.attr("src", mediaRelativeUrl.replaceFirst("//", relativeNotation));
                    filePath = getAbsoluteLocalFilePath(base, filePath.replaceFirst("/", ""));
                } else {
                    media.attr("src", mediaRelativeUrl.replaceFirst("/", relativeNotation));
                    filePath = getAbsoluteLocalFilePath(base, filePath);
                }
            }

            downloadFile(mediaUrl, filePath, fileName);
        }
    }

    private void ProcessImports(String root, Elements imports) {
        final String relativeNotation = getRelativeNotation(root);

        for (Element file : imports) {
            final String fileUrl = file.attr("abs:href");

            String fileRelativeUrl;
            try {
                URL u = new URL(file.attr("href"));
                fileRelativeUrl = u.getPath();
            } catch (Exception ignored) {
                final String rawSrc = trimUrlString(file.attr("href"));
                fileRelativeUrl = rawSrc.substring(0, rawSrc.indexOf('?') == -1 ? rawSrc.length() : rawSrc.indexOf('?'));
            }
            fileRelativeUrl = fileRelativeUrl.substring(0, fileRelativeUrl.indexOf('#') == -1 ? fileRelativeUrl.length() : fileRelativeUrl.indexOf('#'));

            String filePath = getPathWithoutFilename(fileRelativeUrl);
            String fileName = getFileName(fileRelativeUrl);

            if (filePath.charAt(0) == '/') {
                String newLink;

                if (filePath.length() >= 2 && filePath.charAt(1) == '/') {
                    newLink = fileRelativeUrl.replaceFirst("//", relativeNotation);
                    filePath = getAbsoluteLocalFilePath(base, filePath.replaceFirst("/", ""));
                } else {
                    newLink = fileRelativeUrl.replaceFirst("/", relativeNotation);
                    filePath = getAbsoluteLocalFilePath(base, filePath);
                }

                file.attr("href", newLink);
            }

            downloadFile(fileUrl, filePath, fileName);
        }
    }

    private void ProcessLinks(String root, String originDomain, Elements links) {
        final String relativeNotation = getRelativeNotation(root);

        for (Element link : links) {
            try {
                URL u = new URL(link.attr("abs:href"));
                if (!u.getHost().equals(originDomain)) {
                    continue;
                }
            } catch (Exception ignored) {
                continue;
            }

            if (link.attr("href").equals("") || link.attr("href").charAt(0) == '#')
                continue;

            final String absoluteLink = trimUrlString(link.attr("abs:href"));

            String relativeLink;
            try {
                URL u = new URL(link.attr("href"));
                relativeLink = u.getPath();
            } catch (Exception ignored) {
                final String rawSrc = trimUrlString(link.attr("href"));
                relativeLink = rawSrc.substring(0, rawSrc.indexOf('?') == -1 ? rawSrc.length() : rawSrc.indexOf('?'));
            }
            relativeLink = relativeLink.substring(0, relativeLink.indexOf('#') == -1 ? relativeLink.length() : relativeLink.indexOf('#'));

            String filePath = getPathWithoutFilename(relativeLink);

            if (filePath.isEmpty()) {
                link.attr("href", "./");
            } else if (filePath.charAt(0) == '/') {
                String newLink = relativeLink;

                if (!newLink.contains(".")) {
                    newLink += "/index.html";
                }

                if (filePath.length() >= 2 && filePath.charAt(1) == '/') {
                    newLink = newLink.replaceFirst("//", relativeNotation);
                } else {
                    newLink = newLink.replaceFirst("/", relativeNotation);
                }

                link.attr("href", newLink);
            }

            if (setDownloadedLink(absoluteLink)) {
                addLink(absoluteLink);
            }
        }
    }

    private void crawler(String urlString) throws IOException {
        final URL url = new URL(urlString);
        final String filepath = getPathWithoutFilename(url.getPath());
        final String filename = getFileName(url.getPath());
        final String root = getAbsoluteLocalFilePath(base, filepath);
        final String domain = url.getHost();

        try {
            Document doc = Jsoup.connect(url.toString()).get();
            Elements links = doc.select("a[href]");
            Elements medium = doc.select("[src]");
            Elements imports = doc.select("link[href]");

            // Xoá cái base để đường dẫn tương đối bắt đầu từ folder hiện tại
            // https://html.com/attributes/base-href/
            if (doc.head().children().select("base").size() > 0) {
                doc.head().children().select("base").remove();
            }

            ProcessMedium(filepath, medium);
            ProcessImports(filepath, imports);
            ProcessLinks(filepath, domain, links);

            new File(root).mkdirs();
            PrintWriter out = new PrintWriter(root + "/" + filename);
            out.println(doc.toString());
            out.close();
        } catch (UnsupportedMimeTypeException e) {
            downloadFile(url.toString(), root, filename);
        }
    }
}
