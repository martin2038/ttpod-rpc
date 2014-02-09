package test.netty.query;

import com.ttpod.netty.Client;
import com.ttpod.netty.rpc.InnerBindUtil;
import com.ttpod.netty.rpc.RequestBean;
import com.ttpod.netty.rpc.ResponseBean;
import com.ttpod.netty.rpc.client.ClientHandler;
import com.ttpod.netty.rpc.client.DefaultClientHandler;
import com.ttpod.netty.rpc.client.OutstandingContainer;
import com.ttpod.netty.rpc.codec.RequestEncoder;
import com.ttpod.netty.rpc.codec.ResponseDecoder;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * date: 14-1-28 下午2:16
 *
 * @author: yangyang.cong@ttpod.com
 */
public class QueryClient {
    public static void main(String[] args) throws Exception {
        final ChannelHandler requestEncoder = new RequestEncoder();
        final ChannelHandler responseDecoder = new ResponseDecoder();
        Client client = new Client(new InetSocketAddress("127.0.0.1", 6666),
            new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ChannelPipeline p = ch.pipeline();
                    p.addLast("frameDecoder", new ProtobufVarint32FrameDecoder());
                    p.addLast("responseDecoder", responseDecoder);
                    p.addLast("requestEncoder", requestEncoder);
                    p.addLast(new DefaultClientHandler());
                }
            });

        // Read commands from the stdin.
        ChannelFuture lastWriteFuture = null;
        final ClientHandler handler = client.getChannel().pipeline().get(DefaultClientHandler.class);
        final int THREADS = OutstandingContainer.UNSIGN_SHORT_OVER_FLOW;
        ExecutorService exe = Executors.newFixedThreadPool(Math.min(1024,THREADS));
        for (int i = THREADS; i > 0; i--) {
            exe.execute(new Runnable() {
                public void run() {
                    String q =  Thread.currentThread().getName();
                    RequestBean req = new RequestBean(RequestBean.QueryServie.SONG, (short) 1, (short) 50, q);
                    ResponseBean msg = handler.rpc(req);
                    if (!msg.toString().contains(q)) {
                        System.err.println(q + "\t" + InnerBindUtil.id(req) + "\t" + msg);
                    }
                }
            });
        }
        System.out.println("Pls Input a  word ..");
//      final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        for (; ; ) {
            String line = in.readLine();
            RequestBean req = new RequestBean(RequestBean.QueryServie.SONG, (short) 1, (short) 50, line);
            ResponseBean res = handler.rpc(req);
            System.out.println(line + "  ->  rpc["+ InnerBindUtil.id(req) +"] -> " +res );
            if ("bye".equals(line)) {
                client.close();
                break;
            }
        }
        exe.shutdown();

    }
}