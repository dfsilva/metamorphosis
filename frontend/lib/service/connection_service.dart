import 'dart:convert';

import 'package:nats_message_processor_client/bus/actions.dart';
import 'package:nats_message_processor_client/dto/agent.dart';
import 'package:nats_message_processor_client/dto/websocket.dart';
import 'package:nats_message_processor_client/service/base_service.dart';
import 'package:nats_message_processor_client/store/connection_store.dart';
import 'package:nats_message_processor_client/utils/http_utils.dart';
import 'package:nats_message_processor_client/utils/uuid_generator.dart';
import 'package:nats_message_processor_client/ws/web_socket_connection_base.dart'
    if (dart.library.io) 'package:nats_message_processor_client/ws/io_web_socket_connection.dart'
    if (dart.library.html) 'package:nats_message_processor_client/ws/html_web_socket_connection.dart';
import 'package:web_socket_channel/web_socket_channel.dart';

class ConnectionService extends BaseService<ConnectionStore> {
  WebSocketChannel _channel;

  ConnectionService(rxBus) : super(rxBus, ConnectionStore());

  Future<void> _connect() {
    if (!store().connected) {
      try {
        _channel?.sink?.close();
        _channel = getConnectionFactory().connect('ws://${Api.HOST}/ws');
        _channel.stream.listen((data) {
          print(data);
          dynamic result = json.decode(data);
          IncomingMessage wsMessage = IncomingMessage.fromJson(result);
          switch (wsMessage.action) {
            case "connected":
              bus().send(WsConnected(_channel));
              break;
            case "set-agent-detail":
              bus().send(SetAgent(Agent.fromJson(wsMessage.message)));
              break;
            default:
              print("default: $data");
          }
        }, onDone: () {
          bus().send(WsDisconected());
          Future.delayed(Duration(seconds: 10), () => _connect());
        });
      } catch (e) {
        bus().send(WsDisconected());
        Future.delayed(Duration(seconds: 10), () => _connect());
      }
    }
  }

  void close() {
    _channel?.sink?.close();
  }

  @override
  void dispose() {
    close();
  }

  @override
  bool filter(event) => [WsDisconected, WsConnected, WsConnect, SendWsMessage].contains(event.runtimeType);

  @override
  void onReceiveMessage(msg) {
    if (msg is WsDisconected) {
      store().setConnected(false);
    }

    if (msg is WsConnected) {
      store().setConnected(true);
      bus().send(SendWsMessage(message: OutcomeMessage(uuid: newUuid(), action: "get-processes")), seconds: 1);
    }

    if (msg is SendWsMessage) {
      _channel.sink.add(json.encode(msg.message.toJson()));
    }

    if (msg is WsConnect) {
      _connect();
    }
  }
}
