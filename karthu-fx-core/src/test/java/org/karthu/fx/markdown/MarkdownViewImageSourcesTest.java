package org.karthu.fx.markdown;

import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.text.TextFlow;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.karthu.fx.markdown.MarkdownTestSupport.image;
import static org.karthu.fx.markdown.MarkdownTestSupport.images;
import static org.karthu.fx.markdown.MarkdownTestSupport.layOut;
import static org.karthu.fx.markdown.MarkdownTestSupport.longParagraphs;
import static org.karthu.fx.markdown.MarkdownTestSupport.paragraphs;
import static org.karthu.fx.markdown.MarkdownTestSupport.relayOut;
import static org.karthu.fx.markdown.MarkdownTestSupport.sceneBounds;
import static org.karthu.fx.markdown.MarkdownTestSupport.scrollToTop;
import static org.karthu.fx.markdown.TestImages.BLUE;
import static org.karthu.fx.markdown.TestImages.RED;
import static org.karthu.fx.markdown.TestImages.colors;
import static org.karthu.fx.markdown.TestImages.png;
import static org.karthu.fx.markdown.TestImages.serving;

/**
 * Where the Markdown view's images come from, seen from outside: the view's own loader, the initial value of its
 * image loader, with the base address and the internet switch, and a change of any of them reloading the shown images
 * in place.
 *
 * <p>The loader is checked through the public API, {@code view.getImageLoader().load(address)}, and through the
 * images the view shows, found by their documented style classes. The files lie in a temporary folder of the test.
 * No test opens a connection to the internet: the internet switch is checked against a minimal HTTP responder on a
 * local socket of the test.
 */
class MarkdownViewImageSourcesTest {

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        MarkdownTestSupport.initToolkit();
    }

    @Test
    @DisplayName("a relative address resolves against a file: base, and the view shows the file's image")
    void relativeAddressAgainstAFileBase(@TempDir Path folder) throws IOException {
        Files.createDirectories(folder.resolve("shots"));
        Files.write(folder.resolve("shots").resolve("a.png"), png(30, 20, RED));
        MarkdownView view = view();
        view.setImageBase(folder.toUri().toString());
        view.setMarkdown("![A shot](shots/a.png)");
        layOut(view);

        assertEquals(30, image(view).getLayoutBounds().getWidth(), "the file's image at its natural size");
    }

    @Test
    @DisplayName("without a base, a relative address is missing")
    void relativeAddressWithoutABase() {
        MarkdownView view = view();
        view.setMarkdown("![A shot](shots/a.png)");
        layOut(view);

        assertNull(view.getImageBase(), "no base by default");
        assertThrows(IOException.class, () -> view.getImageLoader().load("shots/a.png"));
        assertNotNull(view.lookup(".markdown-image-error"), "the missing box");
    }

    @Test
    @DisplayName("a relative address resolves against a jar: base as against a page, also up out of its folder")
    void relativeAddressAgainstAJarBase(@TempDir Path folder) throws IOException {
        byte[] shot = png(30, 20, RED);
        byte[] logo = png(40, 10, BLUE);
        Path jar = folder.resolve("docs.jar");
        writeJar(jar, Map.of("docs/shots/a.png", shot, "images/logo.png", logo));
        MarkdownView view = view();
        view.setImageBase("jar:" + jar.toUri() + "!/docs/readme.md");

        assertArrayEquals(shot, view.getImageLoader().load("shots/a.png"), "beside the page");
        assertArrayEquals(logo, view.getImageLoader().load("../images/logo.png"), "up out of the page's folder");
        assertThrows(IOException.class, () -> view.getImageLoader().load("shots/none.png"), "no such entry");
        // The temporary folder is deleted after the test: the loader must not keep the archive open, which on
        // Windows would lock it.
    }

    @Test
    @DisplayName("absolute file: and jar: addresses and an absolute file path of the platform load as they are")
    void absoluteAddresses(@TempDir Path folder) throws IOException {
        byte[] shot = png(30, 20, RED);
        byte[] logo = png(40, 10, BLUE);
        Path file = folder.resolve("a.png");
        Files.write(file, shot);
        Path jar = folder.resolve("images.jar");
        writeJar(jar, Map.of("images/logo.png", logo));
        MarkdownView view = view();
        view.setImageBase(folder.resolve("elsewhere").toUri().toString());

        assertArrayEquals(shot, view.getImageLoader().load(file.toUri().toString()), "a file: address");
        assertArrayEquals(logo, view.getImageLoader().load("jar:" + jar.toUri() + "!/images/logo.png"),
                "a jar: address");
        assertArrayEquals(shot, view.getImageLoader().load(file.toString()), "an absolute file path");
    }

    @Test
    @DisplayName("characters a URI does not allow, such as spaces, are encoded before resolving, escapes stay")
    void charactersAUriDoesNotAllow(@TempDir Path folder) throws IOException {
        byte[] shot = png(30, 20, RED);
        Path shots = Files.createDirectories(folder.resolve("my shots"));
        Files.write(shots.resolve("a b.png"), shot);
        Files.write(shots.resolve("\u00dcbersicht 100%.png"), shot);
        MarkdownView view = view();
        view.setImageBase(folder.toUri().toString());

        assertArrayEquals(shot, view.getImageLoader().load("my shots/a b.png"), "spaces");
        assertArrayEquals(shot, view.getImageLoader().load("my%20shots/a%20b.png"), "escapes as written");
        assertArrayEquals(shot, view.getImageLoader().load("my shots/\u00dcbersicht 100%.png"),
                "a letter beyond ASCII and a percent sign that starts no escape");
        Files.write(shots.resolve("shot [1].png"), shot);
        assertArrayEquals(shot, view.getImageLoader().load("my shots/shot [1].png"), "square brackets in a path");
        view.setMarkdown("![A shot](<my shots/a b.png>)");
        layOut(view);
        assertEquals(30, image(view).getLayoutBounds().getWidth(), "the view shows it");
    }

    @Test
    @DisplayName("a file or an archive's entry is loaded without the address's query and fragment, as a browser does")
    void queryAndFragmentOfALocalFile(@TempDir Path folder) throws IOException {
        byte[] shot = png(30, 20, RED);
        Files.write(folder.resolve("a.png"), shot);
        Path jar = folder.resolve("docs.jar");
        writeJar(jar, Map.of("docs/a.png", shot));
        MarkdownView view = view();
        view.setImageBase(folder.toUri().toString());

        assertArrayEquals(shot, view.getImageLoader().load("a.png?raw=true"), "a query");
        assertArrayEquals(shot, view.getImageLoader().load("a.png#top"), "a fragment");
        view.setImageBase("jar:" + jar.toUri() + "!/docs/");
        assertArrayEquals(shot, view.getImageLoader().load("a.png?raw=true#top"), "in an archive");
    }

    @Test
    @DisplayName("a data: address with base64 content of an image/ type is decoded, any other is missing")
    void dataAddresses() throws IOException {
        byte[] shot = png(30, 20, RED);
        String content = Base64.getEncoder().encodeToString(shot);
        MarkdownView view = view();
        ImageLoader loader = view.getImageLoader();

        assertArrayEquals(shot, loader.load("data:image/png;base64," + content), "an image");
        assertArrayEquals(shot, loader.load("DATA:Image/PNG;BASE64," + content), "in capitals");
        assertArrayEquals(shot, loader.load("data:image/png;name=a.png;base64," + content), "with a parameter");
        assertThrows(IOException.class, () -> loader.load("data:text/plain;base64," + content), "no image type");
        assertThrows(IOException.class, () -> loader.load("data:;base64," + content), "no type");
        assertThrows(IOException.class, () -> loader.load("data:image/png," + content), "not base64");
        assertThrows(IOException.class, () -> loader.load("data:image/png;base64,***"), "damaged base64");
        assertThrows(IOException.class, () -> loader.load("data:image/png;base64"), "no content");
        view.setMarkdown("![A shot](data:image/png;base64," + content + ")");
        layOut(view);
        assertEquals(30, image(view).getLayoutBounds().getWidth(), "the view shows it");
    }

    @Test
    @DisplayName("while remoteImages is off, the default, http: and https: addresses are missing and no request leaves")
    void remoteImagesOff() throws IOException {
        try (LocalHttpResponder server = new LocalHttpResponder(Map.of("/shots/a.png", png(30, 20, RED)))) {
            MarkdownView view = view();
            ImageLoader loader = view.getImageLoader();

            assertFalse(view.isRemoteImages(), "off by default");
            assertThrows(IOException.class, () -> loader.load(server.base() + "shots/a.png"), "http:");
            assertThrows(IOException.class, () -> loader.load(server.base().replace("http:", "https:") + "a.png"),
                    "https:");
            view.setImageBase(server.base());
            view.setMarkdown("![A shot](shots/a.png)");
            layOut(view);
            assertNotNull(view.lookup(".markdown-image-error"), "relative to an http: base, the image is missing");
            assertEquals(0, server.connections(), "no connection reached the server");
        }
    }

    @Test
    @DisplayName("while remoteImages is on, http: addresses load, absolute or relative to an http: base")
    void remoteImagesOn() throws IOException {
        byte[] shot = png(30, 20, RED);
        try (LocalHttpResponder server = new LocalHttpResponder(Map.of("/shots/a.png", shot))) {
            MarkdownView view = view();
            view.setRemoteImages(true);
            ImageLoader loader = view.getImageLoader();

            assertArrayEquals(shot, loader.load(server.base() + "shots/a.png"), "an absolute address");
            assertEquals(1, server.connections(), "one request");
            assertThrows(IOException.class, () -> loader.load(server.base() + "shots/none.png"), "a 404");
            view.setImageBase(server.base() + "docs/readme.md");
            view.setMarkdown("![A shot](../shots/a.png)");
            layOut(view);
            assertEquals(30, image(view).getLayoutBounds().getWidth(), "relative to an http: base, the view shows it");
        }
    }

    @Test
    @DisplayName("a jar: address loads from an archive file only, never from an archive on the internet")
    void jarAddressesReadArchiveFilesOnly() throws IOException {
        try (LocalHttpResponder server = new LocalHttpResponder(Map.of("/images.jar", new byte[] {1, 2, 3}))) {
            MarkdownView view = view();
            ImageLoader loader = view.getImageLoader();
            String address = "jar:" + server.base() + "images.jar!/a.png";

            assertThrows(IOException.class, () -> loader.load(address), "switched off");
            view.setRemoteImages(true);
            assertThrows(IOException.class, () -> loader.load(address), "switched on");
            assertEquals(0, server.connections(), "no connection reached the server");
        }
    }

    @Test
    @DisplayName("other schemes are missing, absolute or as the base's")
    void otherSchemes() {
        MarkdownView view = view();
        view.setRemoteImages(true);
        ImageLoader loader = view.getImageLoader();

        for (String address : List.of("ftp://127.0.0.1/a.png", "jrt:/java.base/a.png", "mailto:a@example.com",
                "about:blank", "file:a.png")) {
            assertThrows(IOException.class, () -> loader.load(address), address);
        }
        view.setImageBase("ftp://127.0.0.1/docs/");
        assertThrows(IOException.class, () -> loader.load("a.png"), "relative to an ftp: base");
    }

    @Test
    @DisplayName("a new base reloads the shown images in place: the same blocks, the text untouched")
    void newBaseReloadsInPlace(@TempDir Path folder) throws IOException {
        Path one = Files.createDirectories(folder.resolve("one"));
        Path two = Files.createDirectories(folder.resolve("two"));
        Files.write(one.resolve("a.png"), png(30, 20, RED));
        Files.write(two.resolve("a.png"), png(50, 10, BLUE));
        MarkdownView view = view();
        view.setImageBase(one.toUri().toString());
        view.setMarkdown("Before.\n\n![A shot](a.png)\n\nAfter.");
        layOut(view);
        Node block = view.lookup(".markdown-image-block");
        List<TextFlow> paragraphs = paragraphs(view);

        view.setImageBase(two.toUri().toString());
        relayOut(view);

        assertEquals(50, image(view).getLayoutBounds().getWidth(), "the image of the new base");
        assertEquals(List.of(BLUE), colors(image(view).getImage(), 25, 5));
        assertSame(block, view.lookup(".markdown-image-block"), "the same block");
        assertEquals(paragraphs, paragraphs(view), "the same paragraphs");
    }

    @Test
    @DisplayName("a new loader reloads the shown images in place, and no loader shows them missing")
    void newLoaderReloadsInPlace() {
        MarkdownView view = view();
        view.setImageLoader(serving(Map.of("a.png", png(30, 20, RED))));
        view.setMarkdown("![A shot](a.png)");
        layOut(view);
        Node block = view.lookup(".markdown-image-block");

        view.setImageLoader(serving(Map.of("a.png", png(50, 10, BLUE))));
        relayOut(view);
        assertEquals(50, image(view).getLayoutBounds().getWidth(), "the new loader's image");
        assertSame(block, view.lookup(".markdown-image-block"), "the same block");

        view.setImageLoader(null);
        relayOut(view);
        assertNotNull(block.lookup(".markdown-image-error"), "the missing box");
        assertTrue(images(view).isEmpty(), "no image");
    }

    @Test
    @DisplayName("switching remoteImages reloads the shown images in place: on, the image arrives, off, it is missing")
    void remoteImagesSwitchReloadsInPlace() throws IOException {
        try (LocalHttpResponder server = new LocalHttpResponder(Map.of("/shots/a.png", png(30, 20, RED)))) {
            MarkdownView view = view();
            view.setMarkdown("![A shot](" + server.base() + "shots/a.png)");
            layOut(view);
            Node block = view.lookup(".markdown-image-block");
            assertNotNull(block.lookup(".markdown-image-error"), "missing while off");

            view.setRemoteImages(true);
            relayOut(view);
            assertEquals(30, image(view).getLayoutBounds().getWidth(), "the image arrives");
            assertSame(block, view.lookup(".markdown-image-block"), "in the same block");
            assertEquals(1, server.connections(), "with one request");

            view.setRemoteImages(false);
            relayOut(view);
            assertNotNull(block.lookup(".markdown-image-error"), "missing again");
            assertEquals(1, server.connections(), "no further request");
        }
    }

    @Test
    @DisplayName("while reloading, a block keeps its image, and a result that arrives after a newer one is dropped")
    void reloadKeepsTheImageAndTheLatestSourceWins() {
        MarkdownView view = view();
        view.setImageLoader(serving(Map.of("a.png", png(30, 20, RED))));
        view.setMarkdown("![A shot](a.png)");
        layOut(view);
        HeldLoads work = new HeldLoads();
        view.setImageWork(work);

        view.setImageLoader(serving(Map.of("a.png", png(50, 10, BLUE))));
        view.setImageLoader(serving(Map.of("a.png", png(40, 40, RED))));
        relayOut(view);
        assertEquals(30, image(view).getLayoutBounds().getWidth(), "the image shown so far, until a result arrives");

        work.runNewestFirst();
        relayOut(view);
        assertEquals(40, image(view).getLayoutBounds().getWidth(), "the latest loader's image");
    }

    @Test
    @DisplayName("a reload keeps the reader's place: the line at the top edge stays there")
    void reloadKeepsTheReadersPlace(@TempDir Path folder) throws IOException {
        Path one = Files.createDirectories(folder.resolve("one"));
        Path two = Files.createDirectories(folder.resolve("two"));
        Files.write(one.resolve("above.png"), png(60, 40, RED));
        Files.write(two.resolve("above.png"), png(60, 40, BLUE));
        Files.write(one.resolve("below.png"), png(60, 40, RED));
        Files.write(two.resolve("below.png"), png(80, 120, BLUE));
        MarkdownView view = view();
        view.setImageBase(one.toUri().toString());
        view.setMarkdown("![above](above.png)\n\n" + longParagraphs(4) + "![below](below.png)\n\n" + longParagraphs(4));
        layOut(view);
        TextFlow paragraph = paragraphs(view).get(1);
        scrollToTop(view, sceneBounds(paragraph), 0.5);
        double top = sceneBounds(paragraph).getMinY();

        view.setImageBase(two.toUri().toString());
        relayOut(view);

        assertEquals(List.of(BLUE), colors(images(view).get(0).getImage(), 30, 20), "the image above is reloaded");
        assertEquals(120, images(view).get(1).getLayoutBounds().getHeight(), "the image below is reloaded");
        assertEquals(top, sceneBounds(paragraph).getMinY(), 1e-9, "the line at the top edge stays there");
    }

    @Test
    @DisplayName("an application wraps the view's own loader, which reads the view's base at each call")
    void wrappingTheViewsOwnLoader(@TempDir Path folder) throws IOException {
        Path one = Files.createDirectories(folder.resolve("one"));
        Path two = Files.createDirectories(folder.resolve("two"));
        Files.write(one.resolve("a.png"), png(30, 20, RED));
        Files.write(two.resolve("a.png"), png(50, 10, BLUE));
        byte[] logo = png(10, 10, BLUE);
        MarkdownView view = view();
        ImageLoader own = view.getImageLoader();
        view.setImageLoader(address -> address.startsWith("bundle:") ? logo : own.load(address));
        view.setImageBase(one.toUri().toString());
        view.setMarkdown("![logo](bundle:logo.png)\n\n![A shot](a.png)");
        layOut(view);
        assertEquals(List.of(10.0, 30.0), widths(images(view)), "the application's image and the file");

        view.setImageBase(two.toUri().toString());
        relayOut(view);
        assertEquals(List.of(10.0, 50.0), widths(images(view)), "the file of the new base");
    }

    // --- Helpers ---------------------------------------------------------------------------------------------------

    /**
     * The widths of image nodes.
     */
    private static List<Double> widths(List<ImageView> images) {
        List<Double> widths = new ArrayList<>();
        for (ImageView image : images) {
            widths.add(image.getLayoutBounds().getWidth());
        }
        return widths;
    }

    /**
     * Work on images that holds the loads until the test runs them, newest first, as loads on several threads may
     * finish; decodes, changes of the scene and recomputes run at once.
     */
    private static final class HeldLoads implements ImageWork {

        private final List<Runnable> loads = new ArrayList<>();

        @Override
        public void load(Runnable task) {
            loads.add(task);
        }

        @Override
        public void decode(ImageNode block, Runnable task) {
            task.run();
        }

        @Override
        public void show(Runnable task) {
            task.run();
        }

        @Override
        public void compute(Runnable task) {
            task.run();
        }

        @Override
        public Settling settling(Runnable task) {
            return task::run;
        }

        /**
         * Runs the held loads, the newest first.
         */
        void runNewestFirst() {
            while (!loads.isEmpty()) {
                loads.removeLast().run();
            }
        }
    }

    /**
     * A minimal HTTP responder on a local socket, 127.0.0.1 and a free port: it answers each request for a path it
     * has with the file's bytes, any other with 404, and counts the connections it accepts.
     */
    private static final class LocalHttpResponder implements AutoCloseable {

        private final ServerSocket socket;
        private final Map<String, byte[]> files;
        private final AtomicInteger connections = new AtomicInteger();

        LocalHttpResponder(Map<String, byte[]> files) throws IOException {
            this.files = Map.copyOf(files);
            socket = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"));
            Thread thread = new Thread(() -> serve(), "test-http-responder");
            thread.setDaemon(true);
            thread.start();
        }

        /**
         * The responder's base address, {@code http://127.0.0.1:<port>/}.
         */
        String base() {
            return "http://127.0.0.1:" + socket.getLocalPort() + "/";
        }

        /**
         * How many connections the responder has accepted.
         */
        int connections() {
            return connections.get();
        }

        private void serve() {
            while (!socket.isClosed()) {
                try (Socket client = socket.accept()) {
                    connections.incrementAndGet();
                    answer(client);
                } catch (IOException closedOrBroken) {
                    // The test closed the responder, or a client went away: nothing to answer.
                }
            }
        }

        /**
         * Reads a request's head and answers it, then closes the connection.
         */
        private void answer(Socket client) throws IOException {
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(),
                    StandardCharsets.ISO_8859_1));
            String requestLine = in.readLine();
            String line = requestLine;
            while (line != null && !line.isEmpty()) {
                line = in.readLine();
            }
            String[] parts = requestLine == null ? new String[0] : requestLine.split(" ");
            byte[] body = parts.length > 1 ? files.get(parts[1]) : null;
            String status = body == null ? "404 Not Found" : "200 OK";
            byte[] content = body == null ? new byte[0] : body;
            OutputStream out = client.getOutputStream();
            out.write(("HTTP/1.1 " + status + "\r\nContent-Type: application/octet-stream\r\nContent-Length: "
                    + content.length + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.ISO_8859_1));
            out.write(content);
            out.flush();
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }
    }

    /**
     * Writes a JAR file of entries.
     */
    private static void writeJar(Path jar, Map<String, byte[]> entries) throws IOException {
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(jar))) {
            for (Map.Entry<String, byte[]> entry : new TreeMap<>(entries).entrySet()) {
                out.putNextEntry(new ZipEntry(entry.getKey()));
                out.write(entry.getValue());
                out.closeEntry();
            }
        }
    }

    /**
     * A view with its own loader, whose images' work runs at once on the calling thread and which assumes an output
     * scale of 1.0.
     */
    private static MarkdownView view() {
        MarkdownView view = new MarkdownView();
        view.setImageWork(new ImmediateWork());
        view.setOutputScale(1.0);
        return view;
    }
}
