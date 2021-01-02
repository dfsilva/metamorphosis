import 'package:nats_message_processor_client/dto/agent.dart';

class AddUpdateAgentReq {
  final String title;
  final String description;
  final String dataScript;
  final String ifscript;
  final String from;
  final String to;
  final String to2;
  final String agentType;
  final bool ordered;

  AddUpdateAgentReq(
      {this.title, this.description, this.dataScript, this.ifscript, this.from, this.to, this.to2, this.agentType, this.ordered});

  Map<String, Object> toJson() => {
        "title": this.title,
        "description": this.description,
        "dataScript": this.dataScript,
        "ifscript": this.ifscript,
        "from": this.from,
        "to": this.to,
        "to2": this.to2,
        "agentType": this.agentType,
        "ordered": this.ordered,
      };

  static AddUpdateAgentReq fromAgent(Agent agent) => AddUpdateAgentReq(
      title: agent.title,
      description: agent.description,
      dataScript: agent.dataScript,
      ifscript: agent.ifscript,
      from: agent.from,
      to: agent.to,
      to2: agent.to2,
      agentType: agent.agentType,
      ordered: agent.ordered);
}
