import 'package:nats_message_processor_client/dto/topic_message.dart';

class Agent {
  final String uuid;
  final String title;
  final String description;
  final String dataScript;
  final String ifscript;
  final String from;
  final String to;
  final String to2;
  final String status;
  final String agentType;
  final bool ordered;
  final List<TopicMessage> waiting;
  final List<TopicMessage> processing;
  final List<TopicMessage> error;
  final List<String> success;

  Agent(
      {this.uuid,
      this.title,
      this.description,
      this.dataScript,
      this.ifscript,
      this.from,
      this.to,
      this.to2,
      this.status,
      this.agentType = "D",
      this.ordered = false,
      this.waiting = const <TopicMessage>[],
      this.processing = const <TopicMessage>[],
      this.error = const <TopicMessage>[],
      this.success = const <String>[]});

  static Agent fromJson(Map<String, Object> json) {
    return Agent(
      uuid: json["uuid"],
      title: json["title"],
      description: json["description"],
      dataScript: json["dataScript"],
      ifscript: json["ifscript"],
      from: json["from"],
      to: json["to"],
      to2: json["to2"],
      status: json["status"],
      agentType: json["agentType"],
      ordered: json["ordered"] as bool,
      waiting: json["waiting"] != null ? (json["waiting"] as Iterable).map((e) => TopicMessage.fromJson(e)).toList() : const <TopicMessage>[],
      processing: json["processing"] != null ? (json["processing"] as Iterable).map((e) => TopicMessage.fromJson(e)).toList() : const <TopicMessage>[],
      error: json["error"] != null ? (json["error"] as Iterable).map((e) => TopicMessage.fromJson(e)).toList() : const <TopicMessage>[],
      success: json["success"] != null ? (json["success"] as Iterable).map((v) => v.toString()).toList() : const <String>[],
    );
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
          String dataScript,
          String ifscript,
          String from,
          String to,
          String to2,
          String status,
          String agentType,
          bool ordered,
          List<TopicMessage> queue,
          List<String> processing,
          List<String> error,
          List<String> success}) =>
      Agent(
          uuid: this.uuid,
          title: title ?? this.title,
          description: description ?? this.description,
          dataScript: dataScript ?? this.dataScript,
          ifscript: ifscript ?? this.ifscript,
          from: from ?? this.from,
          to: to ?? this.to,
          to2: to2 ?? this.to2,
          status: status ?? this.status,
          agentType: agentType ?? this.agentType,
          ordered: ordered != null ? ordered : this.ordered,
          waiting: queue ?? this.waiting,
          processing: processing ?? this.processing,
          error: error ?? this.error,
          success: success ?? this.success);
}
