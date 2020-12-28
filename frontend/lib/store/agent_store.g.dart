// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'agent_store.dart';

// **************************************************************************
// StoreGenerator
// **************************************************************************

// ignore_for_file: non_constant_identifier_names, unnecessary_brace_in_string_interps, unnecessary_lambdas, prefer_expression_function_bodies, lines_longer_than_80_chars, avoid_as, avoid_annotating_with_dynamic

mixin _$AgentStore on _AgentStore, Store {
  final _$agentsAtom = Atom(name: '_AgentStore.agents');

  @override
  ObservableMap<String, Agent> get agents {
    _$agentsAtom.reportRead();
    return super.agents;
  }

  @override
  set agents(ObservableMap<String, Agent> value) {
    _$agentsAtom.reportWrite(value, super.agents, () {
      super.agents = value;
    });
  }

  final _$_AgentStoreActionController = ActionController(name: '_AgentStore');

  @override
  dynamic setAgents(List<Agent> agents) {
    final _$actionInfo = _$_AgentStoreActionController.startAction(
        name: '_AgentStore.setAgents');
    try {
      return super.setAgents(agents);
    } finally {
      _$_AgentStoreActionController.endAction(_$actionInfo);
    }
  }

  @override
  dynamic setAgent(Agent agent) {
    final _$actionInfo =
        _$_AgentStoreActionController.startAction(name: '_AgentStore.setAgent');
    try {
      return super.setAgent(agent);
    } finally {
      _$_AgentStoreActionController.endAction(_$actionInfo);
    }
  }

  @override
  String toString() {
    return '''
agents: ${agents}
    ''';
  }
}
