class TopicMessage {
  final String id;
  final String content;
  final String ifResult;
  final String result;
  final DateTime created;
  final DateTime processed;
  final String deliveredTo;

  TopicMessage({this.id, this.content, this.ifResult, this.result, this.created, this.processed, this.deliveredTo});

  static TopicMessage fromJson(Map<String, Object> json) {
    return TopicMessage(
        id: json["id"],
        content: json["content"],
        ifResult: json["ifResult"],
        result: json["result"],
        created: json["created"] != null ? DateTime.fromMillisecondsSinceEpoch(json["created"]) : null,
        processed: json["processed"] != null ? DateTime.fromMillisecondsSinceEpoch(json["processed"]) : null,
        deliveredTo: json["deliveredTo"]);
  }
}
