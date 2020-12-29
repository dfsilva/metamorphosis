
import 'package:web_socket_channel/web_socket_channel.dart';

abstract class WebSocketConnectionBase{
  WebSocketChannel connect(String url);
}

WebSocketConnectionBase getConnectionFactory() => throw UnsupportedError(
    'Cannot create a keyfinder without the packages dart:html or package:shared_preferences');