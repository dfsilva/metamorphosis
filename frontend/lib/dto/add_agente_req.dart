import 'package:nats_message_processor_client/dto/agent.dart';

class AddAgentReq {
  final String title;
  final String description;
  final String code;
  final String from;
  final String to;

  AddAgentReq({this.title, this.description, this.code, this.from, this.to});

  Map<String, Object> toJson() {
    return {"title": this.title, "description": this.description, "code": this.code, "from": this.from, "to": this.to};
  }

  static AddAgentReq fromAgent(Agent agent) => AddAgentReq(title: agent.title, description: agent.description, code: agent.code, from: agent.from, to: agent.to);
}
