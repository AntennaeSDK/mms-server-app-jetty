/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.antennaesdk.server;

import java.net.URI;

import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

/**
 * Simple WS Client that echoes the original message with a timestamp.
 */
public class WebSocketClientStarter {

    private static Object waitLock = new Object();

    public static void main(String[] args) {

        String destUri = "ws://localhost:8080/server";

        if (args.length > 0) {
            destUri = args[0];
        }

        WebSocketClient client = new WebSocketClient();
        WebSocketServerProcessor socket = new WebSocketServerProcessor();

        try {
            client.start();

            URI echoUri = new URI(destUri);
            ClientUpgradeRequest request = new ClientUpgradeRequest();
            client.connect(socket, echoUri, request);
            System.out.printf("Connecting to : %s%n", echoUri);

            // wait for closed socket connection.
            //socket.awaitClose(5,TimeUnit.SECONDS);
            wait4TerminateSignal();

        } catch (Throwable t) {
            t.printStackTrace();

        } finally {
            try {
                client.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void wait4TerminateSignal() {
        synchronized (waitLock) {

            try {
                waitLock.wait();
            } catch (InterruptedException e) {

            }
        }
    }
}
