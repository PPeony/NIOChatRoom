import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;

public class Client {

    private final String host = "127.0.0.1";

    private final int port = 6667;

    private Selector selector;

    private SocketChannel socketChannel;

    private String userName;

    public Client() throws Exception{
        selector = Selector.open();
        socketChannel = SocketChannel.open(new InetSocketAddress(host,port));
        socketChannel.configureBlocking(false);
        //注册
        socketChannel.register(selector, SelectionKey.OP_READ);
        userName = socketChannel.getLocalAddress().toString().substring(1);
        System.out.println(userName+" is ok");
    }

    public void sendInfo(String info) {
        info = userName+" : "+info;
        try {
            socketChannel.write(ByteBuffer.wrap(info.getBytes()));

        } catch (Exception e) {

        }
    }

    public void readInfo() {
        try{
            int count = selector.select();
            if (count > 0) {
                //有可用通道
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    if (key.isReadable()) {
                        SocketChannel sc = (SocketChannel)key.channel();
                        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                        sc.read(byteBuffer);
                        String s = new String(byteBuffer.array());
                        System.out.println(s);
                    }
                    iterator.remove();

                }

            } else {
                System.out.println("没有可用的通道");
            }
        } catch (Exception e) {

        }
    }

    public static void main(String[] args) throws Exception{
        Client client = new Client();
        //启动一个线程
        new Thread() {
            @Override
            public void run() {
                while (true) {
                    client.readInfo();
                    try {
                        //每隔三秒,读取数据
                        Thread.currentThread().sleep(3000);

                    } catch (Exception e) {

                    }
                }
            }
        }.start();
        //发送数据
        Scanner sc = new Scanner(System.in);
        while (sc.hasNext()) {
            String in = sc.nextLine();
            client.sendInfo(in);
        }
    }
}
