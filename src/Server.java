import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

//chatRoom
public class Server {
    private  Selector selector;

    private ServerSocketChannel listenChannel;

    private static final int port = 6667;

    //初始化
    public Server() {
        
        try {
            //得到选择器
            selector = Selector.open();
            //初始化serverSocketChannel
            listenChannel = ServerSocketChannel.open();
            //绑定端口
            listenChannel.socket().bind(new InetSocketAddress(port));
            //设置非阻塞
            listenChannel.configureBlocking(false);
            //把listen注册给selector
            listenChannel.register(selector,SelectionKey.OP_ACCEPT);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //监听
    public void listen() {
        try {
            while (true) {
                int count = selector.select(2000);
                if (count > 0) {
                    //有事件处理
                    //遍历selectionKey
                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while (iterator.hasNext()) {
                        SelectionKey key = iterator.next();
                        if (key.isAcceptable()) {
                            //请求连接
                            SocketChannel sc = listenChannel.accept();
                            //socket一定要记得设置非阻塞，不然会报错IllegalBlockingModeException
                            sc.configureBlocking(false);
                            //注册
                            sc.register(selector,SelectionKey.OP_READ);
                            System.out.println(sc.getRemoteAddress()+" 上线 ");

                        }
                        if (key.isReadable()) {
                            //可读
                            readData(key);
                        }
                        iterator.remove();//删除防止重复处理

                    }
                } else {
                    //wait
                    System.out.println("wait...");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void readData(SelectionKey key) {
        SocketChannel channel = null;
        try {
            //取出channel
            channel = (SocketChannel)key.channel();
            ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
            int count = channel.read(byteBuffer);
            if (count > 0) {
                //有数据
                String msg = new String(byteBuffer.array());
                System.out.println("客户端 : "+msg);
                //向其他转发
                sendInfoToOtherClients(msg,channel);
            }
        } catch (Exception e) {
            //e.printStackTrace();
            try {
                System.out.println(channel.getRemoteAddress()+" 离线了");
                //取消注册
                key.cancel();
                //关闭通道
                channel.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

    }

    //转发
    private void sendInfoToOtherClients(String msg,SocketChannel self) throws Exception{
        System.out.println("转发消息...");
        //遍历selector的所有，排除自己
        for (SelectionKey key : selector.keys()) {
            SelectableChannel targetChannel = key.channel();
            if (targetChannel instanceof SocketChannel && self != targetChannel) {
                //写入buffer
                ByteBuffer wrap = ByteBuffer.wrap(msg.getBytes());
                //写入通道
                ((SocketChannel) targetChannel).write(wrap);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Server server = new Server();
        server.listen();
    }
}
