import 'dart:async';

import 'package:nats_message_processor_client/bus/actions.dart';
import 'package:nats_message_processor_client/dto/add_agente_req.dart';
import 'package:nats_message_processor_client/dto/agent.dart';
import 'package:nats_message_processor_client/dto/update_agente_req.dart';
import 'package:nats_message_processor_client/service/base_service.dart';
import 'package:nats_message_processor_client/store/agent_store.dart';
import 'package:nats_message_processor_client/utils/http_utils.dart';

class ProcessorService extends BaseService<AgentStore> {
  ProcessorService(rxBus) : super(rxBus, AgentStore());

  Future<Agent> addOrUpdate(Agent agent) {
    bus().send(ShowHud(text: "Enviando..."));
    if (agent.uuid == null) {
      return Api.doPost(uri: "/agent", bodyParams: AddAgentReq.fromAgent(agent).toJson())
          .then((value) => Agent.fromJson(value))
          .then((value) => bus().send(SetAgent(value)).agent)
          .whenComplete(() => bus().send(HideHud()));
    } else {
      return Api.doPost(uri: "/agent/${agent.uuid}", bodyParams: UpdateAgentReq.fromAgent(agent).toJson())
          .then((value) => Agent.fromJson(value))
          .then((value) => bus().send(SetAgent(value)).agent)
          .whenComplete(() => bus().send(HideHud()));
    }
  }

  @override
  void dispose() {}

  @override
  void onReceiveMessage(msg) {
    if (msg is SetAgents) {
      store().setAgents(msg.agents);
    }
    if (msg is SetAgent) {
      store().setAgent(msg.agent);
    }
  }
}
