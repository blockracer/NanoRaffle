package raffle;

import uk.oczadly.karl.jnano.websocket.WsObserver;
import raffle.Ws;
import java.net.URISyntaxException;
import java.lang.InterruptedException;

public class BalanceObserver implements WsObserver {
    @Override
    public void onOpen(int httpStatus) {
        System.out.println("Balance WebSocket connected!");
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Balance WebSocket disconnected!");
    }

    @Override
    public void onSocketError(Exception ex) {
        ex.printStackTrace();

    }
}
