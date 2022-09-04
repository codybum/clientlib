package crescoclient;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import java.net.URI;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;

@WebSocket
public class LogStreamer
{
    private final Logger LOG = Log.getLogger(LogStreamer.class);
    private HttpClient http;
    private WebSocketClient client;
    private Session session;

    private String host;
    private int port;

    private LogStreamerCallback logStreamerCallback;


    public LogStreamer(String host, int port, LogStreamerCallback logStreamerCallback) {
        this.logStreamerCallback = logStreamerCallback;
        this.host = host;
        this.port = port;
    }

    public LogStreamer(String host, int port) {
        this.logStreamerCallback = new LogPrinter();
        this.host = host;
        this.port = port;
    }

    public boolean connect() {

        //String url = "wss://qa.sockets.stackexchange.com/";
        //String url = "ws://localhost:8282/api/apisocket";
        String url = "ws://" + host + ":" + port + "/api/logstreamer";

        //SslContextFactory ssl = new SslContextFactory.Client();
        //ssl.setEndpointIdentificationAlgorithm("HTTPS");
        //HttpClient http = new HttpClient(ssl);
        http = new HttpClient();
        client = new WebSocketClient(http);
        try
        {
            http.start();
            client.start();
            LogStreamerImpl socket = new LogStreamerImpl(logStreamerCallback);
            Future<Session> fut = client.connect(socket, URI.create(url));

            //Session session = fut.get();
            session = fut.get();

            int connectionTimeout = 10;
            for(int i=0; i<connectionTimeout; i++) {
                try {
                    if(socket.isActive) {
                        i = 10;
                    } else {
                        Thread.sleep(1000);
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            if(!socket.isActive) {
                LOG.warn("Logger not active!");
                close();
            }

        }
        catch (Throwable t)
        {
            LOG.warn(t);
        }
        finally
        {
            //stop(http);
            //stop(client);
        }
        return true;
    }

    public boolean connected() {

        return session.isOpen();

    }

    public void update_config(String dst_region, String dst_agent) {
        send(dst_region + ',' + dst_agent + ",Trace,default");
    }

    private void send(String message) {

        try {

            session.getRemote().sendString(message);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void close() {
        stop(client);
        stop(http);
    }

    private void stop(LifeCycle lifeCycle) {
        try
        {
            lifeCycle.stop();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    class LogPrinter implements LogStreamerCallback {
        @Override
        public void onMessage(String msg) {
            System.out.println(msg);
        }
    }


}
