package com.github.antennaesdk.ws;

import java.text.SimpleDateFormat;
import java.util.concurrent.*;

import com.google.gson.Gson;
import org.antennae.common.messages.ClientMessage;
import org.antennae.common.messages.ClientMessageWrapper;
import org.antennae.common.messages.ServerMessageWrapper;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

/**
 * Basic Echo Client Socket
 */
@WebSocket(maxTextMessageSize = 64 * 1024)
public class WebSocketServerProcessor
{
    private final CountDownLatch closeLatch;
    @SuppressWarnings("unused")
    private Session session;

    public WebSocketServerProcessor()
    {
        this.closeLatch = new CountDownLatch(1);
    }

    public boolean awaitClose(int duration, TimeUnit unit) throws InterruptedException
    {
        return this.closeLatch.await(duration,unit);
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason)
    {
        System.out.printf("Connection closed: %d - %s%n",statusCode,reason);
        this.session = null;
        this.closeLatch.countDown(); // trigger latch
    }

    @OnWebSocketConnect
    public void onConnect(Session session)
    {
        System.out.printf("Got connect: %s%n",session);
        System.out.printf("Local host:port: %s", session.getLocalAddress());
        this.session = session;
//        try
//        {
//            Future<Void> fut;
//            fut = session.getRemote().sendStringByFuture("Hello");
//            fut.get(2,TimeUnit.SECONDS); // wait for send to complete.
//
//            fut = session.getRemote().sendStringByFuture("Thanks for the conversation.");
//            fut.get(2,TimeUnit.SECONDS); // wait for send to complete.
//
//            session.close(StatusCode.NORMAL,"I'm done");
//        }
//        catch (Throwable t)
//        {
//            t.printStackTrace();
//        }
    }

    @OnWebSocketMessage
    public void onMessage(String msg)
    {
        System.out.printf("Got msg: %s%n",msg);

        processTextMessage(msg);
    }

    public void processTextMessage( String m ){

        if( m != null ){

            ServerMessageWrapper serverMessageWrapper = ServerMessageWrapper.fromJson(m);

            ClientMessageWrapper clientMessageWrapper = new ClientMessageWrapper();
            ClientMessage clientMessage = new ClientMessage();
            clientMessage.setTo( serverMessageWrapper.getServerMessage().getFrom());

            String payload = serverMessageWrapper.getServerMessage().getPayLoad();

            clientMessage.setPayLoad( doBuzinezzLogic(payload) );

            clientMessageWrapper.setClientMessage(clientMessage);
            clientMessageWrapper.setSessionId( serverMessageWrapper.getSessionId());
            clientMessageWrapper.setNodeId( serverMessageWrapper.getNodeId() );

            if( session != null && session.isOpen() ){
                //session.getAsyncRemote().sendText( clientMessageWrapper.toJson() );
                Future<Void> fut= session.getRemote().sendStringByFuture(clientMessageWrapper.toJson());

                try {
                    fut.get(2,TimeUnit.SECONDS); // wait for send to complete.
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (TimeoutException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public String doBuzinezzLogic( String payLoad ){
        Message m = Message.fromJson(payLoad);

        m.body.text = "echo from: " + m.body.text + " : \n" + Timestamper.INSTANCE.getMillis();

        return m.toJson();
    }

    /*
{
  "id": "123e4567-e89b-12d3-a456-426655440013",
  "type": "TEXT",
  "version": "1",
  "sender": {
  "username": "N1"
  },
  "body": {
    "text": "Thanks for call me, Dave!"
   }
}
 */
    public static class Message{
        String id;
        String type;
        String version;
        String sender;
        String username;
        Body body;

        public static class Body{
            String text;
        }

        public String toJson(){
            Gson gson = new Gson();
            String result = gson.toJson(this);
            return result;
        }
        public static Message fromJson( String json ){
            Gson gson = new Gson();
            Message message = gson.fromJson( json, Message.class);
            return message;
        }
    }

    public enum Timestamper
    {   INSTANCE ;

        private SimpleDateFormat dateFormat ;

        private Timestamper()
        {
            this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS") ;
        }

        /*        public String get()
                {
                    long milliSeconds = System.currentTimeMillis();
                    long nanoSeconds = System.nanoTime();
                    long microSeconds = (System.nanoTime() - this.startNanoseconds) / 1000 ;
                    long date = this.startDate + (microSeconds/1000) ;
                    return this.dateFormat.format(date) + String.format("%03d", microSeconds % 1000) ;
                }*/
        public String getMillis(){
            long milliSeconds = System.currentTimeMillis();
            return this.dateFormat.format(milliSeconds);
        }
    }
}