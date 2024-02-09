// worker.js

let websocket;

function initializeWebSocket() {
    websocket = new WebSocket('wss://balancews.nanoriver.io');

    websocket.onopen = () => {
        postMessage({ type: 'open' });
    };

    websocket.addEventListener('message', (event) => {
        postMessage({ type: 'message', data: JSON.parse(event.data) });
    });

    websocket.addEventListener('close', (event) => {
        postMessage({ type: 'close' });
        setTimeout(() => {
            initializeWebSocket(); // Attempt to reconnect
        }, 5000);
    });
}

initializeWebSocket();

// Set up an interval to check the WebSocket connection status
setInterval(() => {
    postMessage({ type: 'checkConnection', isConnected: websocket.readyState === WebSocket.OPEN });
}, 5000);

// Message handler for reconnection attempt
self.onmessage = function (event) {
    if (event.data.type === 'reconnect') {
        initializeWebSocket();
    }
};

