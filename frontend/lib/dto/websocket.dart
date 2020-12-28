abstract class JsonEncodable {
  Map<String, Object> toJson();
}

class IncomingMessage {
  final String uuid;
  final String action;
  final dynamic message;

  IncomingMessage(this.uuid, this.action, this.message);

  static fromJson(Map<String, Object> json) {
    return IncomingMessage(json["uuid"] ?? "", json["action"] ?? "", json["message"]);
  }

  @override
  String toString() {
    return 'WebSocketMessage{message: $message}';
  }
}

class OutcomeMessage<T extends JsonEncodable> {
  final String uuid;
  final String action;
  final T message;

  OutcomeMessage({this.uuid, this.action, this.message});

  Map<String, Object> toJson() {
    return {"uuid": this.uuid, "action": this.action, "message": this.message != null ? this.message.toJson() : ""};
  }

  @override
  String toString() {
    return 'OutcomeMessage{uuid: $uuid, action: $action, message: $message}';
  }
}
