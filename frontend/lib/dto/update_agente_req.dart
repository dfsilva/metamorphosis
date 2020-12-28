import 'package:nats_message_processor_client/dto/agent.dart';

class UpdateAgentReq {
  final String uuid;
  final String title;
  final String description;
  final String code;
  final String to;

  UpdateAgentReq({this.uuid, this.title, this.description, this.code, this.to});

  Map<String, Object> toJson() {
    return {"uuid": this.uuid, "title": this.title, "description": this.description, "code": this.code, "to": this.to};
  }

  static UpdateAgentReq fromAgent(Agent agent) =>
      UpdateAgentReq(uuid: agent.uuid, title: agent.title, description: agent.description, code: agent.code, to: agent.to);
}
