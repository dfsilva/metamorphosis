import 'package:nats_message_processor_client/ws/web_socket_connection_base.dart';
import 'package:web_socket_channel/io.dart';
import 'package:web_socket_channel/src/channel.dart';

class IOFactoryWebSocketConnection implements WebSocketConnectionBase {
  @override
  WebSocketChannel connect(url) {
    return IOWebSocketChannel.connect(url);
  }
}

WebSocketConnectionBase getConnectionFactory() => IOFactoryWebSocketConnection();
