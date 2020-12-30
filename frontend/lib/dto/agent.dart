import 'package:nats_message_processor_client/dto/topic_message.dart';

class Agent {
  final String uuid;
  final String title;
  final String description;
  final String transformerScript;
  final String conditionScript;
  final String from;
  final String to;
  final String to2;
  final String status;
  final String agentType;
  final bool ordered;
  final List<TopicMessage> waiting;
  final Map<String, TopicMessage> processing;
  final List<TopicMessage> error;
  final Map<String, TopicMessage> success;

  Agent(
      {this.uuid,
      this.title,
      this.description,
      this.transformerScript,
      this.conditionScript,
      this.from,
      this.to,
      this.to2,
      this.status,
      this.agentType = "D",
      this.ordered = false,
      this.waiting = const <TopicMessage>[],
      this.processing = const <String, TopicMessage>{},
      this.error = const <TopicMessage>[],
      this.success = const <String, TopicMessage>{}});

  static Agent fromJson(Map<String, Object> json) {
    return Agent(
      uuid: json["uuid"],
      title: json["title"],
      description: json["description"],
      transformerScript: json["transformerScript"],
      conditionScript: json["conditionScript"],
      from: json["from"],
      to: json["to"],
      to2: json["to2"],
      status: json["status"],
      agentType: json["agentType"],
      ordered: json["ordered"] as bool,
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
    return {"title": this.title, "description": this.description, "code": this.transformerScript, "from": this.from, "to": this.to};
  }

  String getTo() {
    if (this.to == null || this.to.isEmpty) return "nowhere";
    return this.to;
  }

  String getTo2() {
    if (this.to2 == null || this.to2.isEmpty) return "nowhere";
    return this.to2;
  }

  Agent copyWith(
          {String title,
          String description,
          String transformerScript,
          String conditionScript,
          String from,
          String to,
          String to2,
          String status,
          String agentType,
          bool ordered,
          List<TopicMessage> waiting,
          Map<String, TopicMessage> processing,
          List<TopicMessage> error,
          Map<String, TopicMessage> success}) =>
      Agent(
          uuid: this.uuid,
          title: title ?? this.title,
          description: description ?? this.description,
          transformerScript: transformerScript ?? this.transformerScript,
          conditionScript: conditionScript ?? this.conditionScript,
          from: from ?? this.from,
          to: to ?? this.to,
          to2: to2 ?? this.to2,
          status: status ?? this.status,
          agentType: agentType ?? this.agentType,
          ordered: ordered != null ? ordered : this.ordered,
          waiting: waiting ?? this.waiting,
          processing: processing ?? this.processing,
          error: error ?? this.error,
          success: success ?? this.success);
}
