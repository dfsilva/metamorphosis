class TopicMessage {
  final String id;
  final String content;
  final String result;

  TopicMessage({this.id, this.content, this.result});

  static TopicMessage fromJson(Map<String, Object> json) {
    return TopicMessage(id: json["id"], content: json["content"], result: json["result"]);
  }
}
