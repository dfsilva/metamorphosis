import 'package:nats_message_processor_client/ws/web_socket_connection_base.dart';
import 'package:web_socket_channel/html.dart';
import 'package:web_socket_channel/src/channel.dart';

class HtmlFactoryWebSocketConnection implements WebSocketConnectionBase {
  @override
  WebSocketChannel connect(url) {
    return HtmlWebSocketChannel.connect(url);
  }
}
WebSocketConnectionBase getConnectionFactory() => HtmlFactoryWebSocketConnection();