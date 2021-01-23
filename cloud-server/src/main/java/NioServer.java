import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

public class NioServer {

    private final ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
    private final Selector selector = Selector.open();
    private final ByteBuffer buffer = ByteBuffer.allocate(5);
    private Path serverPath = Paths.get("cloud_server_dir");

    public static void main(String[] args) throws IOException {

        new NioServer();

    }

    public NioServer() throws IOException {

        serverSocketChannel.bind(new InetSocketAddress(8189));
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        while (serverSocketChannel.isOpen()) {
            selector.select();
            Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = keys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();
                if (key.isAcceptable()) {
                    handleAccept(key);
                }
                if (key.isReadable()) {
                    handleRead(key);
                }
            }

        }
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        int read = 0;
        StringBuilder msg = new StringBuilder();
        byte[] msg_byte = new byte[1024];


        while ((read = channel.read(buffer))>0) {
            buffer.flip();
            while (buffer.hasRemaining()) {
                msg.append((char) buffer.get());
            }
            buffer.clear();
        }
        String command = msg.toString().replaceAll("\r\n","");

        if (command.equals("ls")) {
            String files = Files.list(serverPath)
                    .map(path -> path.getFileName().toString())
                    .collect(Collectors.joining(", "));
            files += "\r\n";
            channel.write(ByteBuffer.wrap(files.getBytes(StandardCharsets.UTF_8)));
        }

        if (command.startsWith("cd")) {
            String[] args = command.split(" ");
            if (args.length != 2) {
                channel.write(ByteBuffer.wrap("Wrong cd command\r\n".getBytes(StandardCharsets.UTF_8)));
            } else {
                String targetPath = args[1];
                Path serverDirBefore = serverPath;
                serverPath = serverPath.resolve(targetPath);
                if (!Files.isDirectory(serverPath) && !Files.exists(serverPath)) {
                    channel.write(ByteBuffer.wrap("Wrong directory\r\n".getBytes(StandardCharsets.UTF_8)));
                    serverPath = serverDirBefore;
                }
            }
        }

        if (command.startsWith("cat")) {
            String[] args = command.split(" ");
            if (args.length != 2) {
                channel.write(ByteBuffer.wrap("Wrong cat command\r\n".getBytes(StandardCharsets.UTF_8)));
            } else {
                String fileName = args[1];
                Path pathFile = Paths.get(serverPath+"\\" + fileName);
                if (!Files.isDirectory(pathFile) && Files.exists(pathFile)){
                    byte[] bytes = Files.readAllBytes(pathFile);
                channel.write(ByteBuffer.wrap(bytes));
               } else {
                    channel.write(ByteBuffer.wrap("File isn't exist or it's name of directory.\r\n".getBytes(StandardCharsets.UTF_8))); }
            }
        }

        if (command.startsWith("touch")) {
            String[] args = command.split(" ");
            if (args.length != 2) {
                channel.write(ByteBuffer.wrap("Wrong touch command\r\n".getBytes(StandardCharsets.UTF_8)));
            } else {
                String fileName = args[1];
                Path pathFile = Paths.get(serverPath+"\\" + fileName);
                if (!Files.isDirectory(pathFile) && !Files.exists(pathFile)){
                    Files.createFile(pathFile);
                    channel.write(ByteBuffer.wrap(("File " + fileName + " created successfully\r\n").getBytes(StandardCharsets.UTF_8)));
                } else {
                    channel.write(ByteBuffer.wrap("File isn't exist or it's name of directory.\r\n".getBytes(StandardCharsets.UTF_8))); }
            }
        }
    }

    private void handleAccept(SelectionKey key) throws IOException {

        SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_READ);


    }





}
