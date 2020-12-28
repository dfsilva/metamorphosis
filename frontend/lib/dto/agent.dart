import 'package:nats_message_processor_client/dto/topic_message.dart';

class Agent {
  final String uuid;
  final String title;
  final String description;
  final String code;
  final String from;
  final String to;
  final String status;
  final List<TopicMessage> waiting;
  final Map<String, TopicMessage> processing;
  final List<TopicMessage> error;
  final Map<String, TopicMessage> success;

  Agent(
      {this.uuid,
      this.title,
      this.description,
      this.code,
      this.from,
      this.to,
      this.status,
      this.waiting = const <TopicMessage>[],
      this.processing = const <String, TopicMessage>{},
      this.error = const <TopicMessage>[],
      this.success = const <String, TopicMessage>{}});

  static Agent fromJson(Map<String, Object> json) {
    return Agent(
      uuid: json["uuid"],
      title: json["title"],
      description: json["description"],
      code: json["code"],
      from: json["from"],
      to: json["to"],
      status: json["status"],
      waiting: json["waiting"] != null ? (json["waiting"] as Iterable).map((e) => TopicMessage.fromJson(e)).toList() : const <TopicMessage>[],
      processing: json["processing"] != null
          ? Map.from(json["processing"]).map<String, TopicMessage>((key, value) => MapEntry(key, TopicMessage.fromJson(value)))
          : const <String, TopicMessage>{},
      error: json["error"] != null ? (json["error"] as Iterable).map((e) => TopicMessage.fromJson(e)).toList() : const <TopicMessage>[],
      success: json["success"] != null
          ? Map.from(json["success"]).map<String, TopicMessage>((key, value) => MapEntry(key, TopicMessage.fromJson(value)))
          : const <String, TopicMessage>{},
    );
  }

  Map<String, Object> toJson() {
    return {
      "title": this.title,
      "description": this.description,
      "code": this.code,
      "from": this.from,
      "to": this.to
    };
  }

  String getToWithDefault(){
    if(this.to == null || this.to.isEmpty)
      return "nowhere";
    return this.to;
  }

  Agent copyWith(
          {String title,
          String description,
          String code,
          String from,
          String to,
          String status,
          List<TopicMessage> waiting,
          Map<String, TopicMessage> processing,
          List<TopicMessage> error,
          Map<String, TopicMessage> success}) =>
      Agent(
          uuid: this.uuid,
          title: title ?? this.title,
          description: description ?? this.description,
          code: code ?? this.code,
          from: from ?? this.from,
          to: to ?? this.to,
          status: status ?? this.status,
          waiting: waiting ?? this.waiting,
          processing: processing ?? this.processing,
          error: error ?? this.error,
          success: success ?? this.success);
}
