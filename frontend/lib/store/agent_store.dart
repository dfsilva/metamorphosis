import 'package:mobx/mobx.dart';
import 'package:nats_message_processor_client/dto/agent.dart';

part 'agent_store.g.dart';

class AgentStore = _AgentStore with _$AgentStore;

abstract class _AgentStore with Store {
  @observable
  ObservableMap<String, Agent> agents = Map<String, Agent>().asObservable();

  @action
  setAgents(List<Agent> agents) {
    this.agents = Map<String, Agent>.fromIterable(agents, key: (p) => p.uuid, value: (p) => p).asObservable();
  }

  @action
  setAgent(Agent agent) {
    this.agents[agent.uuid] = agent;
  }
}
