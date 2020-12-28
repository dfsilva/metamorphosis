
import 'package:web_socket_channel/web_socket_channel.dart';

abstract class WebSocketConnectionBase{
  WebSocketChannel connect(String url);
}