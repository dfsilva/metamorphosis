import 'package:flutter/foundation.dart';
import 'package:nats_message_processor_client/dto/agent.dart';
import 'package:nats_message_processor_client/dto/websocket.dart';
import 'package:web_socket_channel/web_socket_channel.dart';

@immutable
class WsConnect {}

@immutable
class WsConnected {
  final WebSocketChannel channel;

  WsConnected(this.channel);
}

@immutable
class WsDisconected {}

@immutable
class SendWsMessage<T extends JsonEncodable> {
  final OutcomeMessage<T> message;
  final Function(dynamic data) callback;

  SendWsMessage({this.message, this.callback});
}

@immutable
class ShowHud {
  final String text;

  ShowHud({this.text = "Carregando..."});
}

@immutable
class HideHud {}

@immutable
class SetAgents {
  final List<Agent> agents;

  SetAgents(this.agents);
}

@immutable
class SetAgent {
  final Agent agent;

  SetAgent(this.agent);
}
