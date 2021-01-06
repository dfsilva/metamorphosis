import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:graphview/GraphView.dart';
import 'package:nats_message_processor_client/dto/agent.dart';

class GraphAgents extends StatefulWidget {
  final List<Agent> agents;

  const GraphAgents({Key key, this.agents}) : super(key: key);

  @override
  _GraphAgentsState createState() => _GraphAgentsState();
}

class _GraphAgentsState extends State<GraphAgents> {
  final Graph graph = Graph();
  final SugiyamaConfiguration builder = SugiyamaConfiguration();

  @override
  void initState() {
    super.initState();
    _buildNodes();
  }

  @override
  void dispose() {
    super.dispose();
  }

  _buildNodes() {
    if (widget.agents.isNotEmpty) {
      Map<String, Node> nodesFrom = Map.fromIterable(widget.agents, key: (e) => e.from, value: (e) => Node(nodeText(e.from)));

      Map<String, Node> nodesTo = Map.fromIterable(widget.agents,
          key: (e) => e.getTo(),
          value: (e) {
            if (nodesFrom.containsKey(e.getTo())) {
              return nodesFrom[e.getTo()];
            } else {
              return Node(nodeText(e.getTo()));
            }
          });

      Map<String, Node> nodesTo2 = Map.fromIterable(widget.agents.where((element) => element.agentType == "C"),
          key: (e) => e.getTo2(),
          value: (e) {
            if (nodesFrom.containsKey(e.getTo2())) {
              return nodesFrom[e.getTo2()];
            } else if (nodesTo.containsKey(e.getTo2())) {
              return nodesFrom[e.getTo2()];
            } else {
              return Node(nodeText(e.getTo2()));
            }
          });

      Map<String, Node> agentsNode = Map.fromIterable(widget.agents,
          key: (e) => e.uuid,
          value: (e) {
            if (e.agentType == "D") {
              return Node(defaultContainer(e));
            } else {
              return Node(conditionContainer(e));
            }
          });

      widget.agents.forEach((element) {
        graph.addEdge(nodesFrom[element.from], agentsNode[element.uuid],
            paint: Paint()
              ..color = Colors.yellow[600]
              ..strokeCap = StrokeCap.round
              ..strokeWidth = 2.0);

        graph.addEdge(agentsNode[element.uuid], nodesTo[element.getTo()],
            paint: Paint()
              ..color = Colors.green[600]
              ..strokeCap = StrokeCap.round
              ..strokeWidth = 3.0);

        if (element.agentType == "C") {
          graph.addEdge(agentsNode[element.uuid], nodesTo2[element.getTo2()],
              paint: Paint()
                ..color = Colors.deepOrange
                ..strokeCap = StrokeCap.round
                ..strokeWidth = 3.0);
        }
      });

      builder
        ..nodeSeparation = (80)
        ..levelSeparation = (80)
        ..orientation = SugiyamaConfiguration.ORIENTATION_TOP_BOTTOM;
    }
  }

  Widget defaultContainer(Agent agent) {
    return Stack(
      clipBehavior: Clip.none,
      children: [
        Container(
            height: 80,
            width: 80,
            decoration: BoxDecoration(shape: BoxShape.circle, color: _getColor(agent), border: Border.all(color: Colors.white)),
            child: Center(
                child: Column(
              mainAxisSize: MainAxisSize.min,
              mainAxisAlignment: MainAxisAlignment.center,
              crossAxisAlignment: CrossAxisAlignment.center,
              children: [
                Text(
                  agent.title,
                  style: TextStyle(fontSize: 10),
                  textAlign: TextAlign.center,
                )
              ],
            ))),
        Positioned(
            top: 0,
            right: 0,
            child: Container(
              height: 20,
              width: 20,
              decoration: BoxDecoration(color: Colors.yellow[800], shape: BoxShape.circle, border: Border.all(color: Colors.white)),
              child: Center(child: Text("${agent.waiting.length}", style: TextStyle(fontSize: 8))),
            )),
        Positioned(
            top: 19,
            right: -15,
            child: Container(
              height: 20,
              width: 20,
              decoration: BoxDecoration(color: Colors.blue, shape: BoxShape.circle, border: Border.all(color: Colors.white)),
              child: Center(child: Text("${agent.processing.length}", style: TextStyle(fontSize: 8))),
            )),
        Positioned(
            top: 41,
            right: -15,
            child: Container(
              height: 20,
              width: 20,
              decoration: BoxDecoration(color: Colors.red, shape: BoxShape.circle, border: Border.all(color: Colors.white)),
              child: Center(child: Text("${agent.error.length}", style: TextStyle(fontSize: 8))),
            )),
        Positioned(
            top: 60,
            right: 0,
            child: Container(
              height: 20,
              width: 20,
              decoration: BoxDecoration(color: Colors.green, shape: BoxShape.circle, border: Border.all(color: Colors.white)),
              child: Center(child: Text("${agent.success.length}", style: TextStyle(fontSize: 8))),
            )),
      ],
    );
  }

  Widget conditionContainer(Agent agent) {
    return Stack(
      clipBehavior: Clip.none,
      children: [
        Transform.rotate(
          angle: math.pi / 4,
          child: Container(
              height: 80,
              width: 80,
              decoration: BoxDecoration(color: _getColor(agent), border: Border.all(color: Colors.white)),
              child: Center(
                  child: Transform.rotate(
                      angle: -(math.pi / 4),
                      child: Text(
                        agent.title,
                        style: TextStyle(fontSize: 10),
                        textAlign: TextAlign.center,
                      )))),
        ),
        Positioned(
            top: 0,
            right: 0,
            child: Container(
              height: 20,
              width: 20,
              decoration: BoxDecoration(color: Colors.yellow[800], shape: BoxShape.circle, border: Border.all(color: Colors.white)),
              child: Center(child: Text("${agent.waiting.length}", style: TextStyle(fontSize: 8))),
            )),
        Positioned(
            top: 19,
            right: -15,
            child: Container(
              height: 20,
              width: 20,
              decoration: BoxDecoration(color: Colors.blue, shape: BoxShape.circle, border: Border.all(color: Colors.white)),
              child: Center(child: Text("${agent.processing.length}", style: TextStyle(fontSize: 8))),
            )),
        Positioned(
            top: 41,
            right: -15,
            child: Container(
              height: 20,
              width: 20,
              decoration: BoxDecoration(color: Colors.red, shape: BoxShape.circle, border: Border.all(color: Colors.white)),
              child: Center(child: Text("${agent.error.length}", style: TextStyle(fontSize: 8))),
            )),
        Positioned(
            top: 60,
            right: 0,
            child: Container(
              height: 20,
              width: 20,
              decoration: BoxDecoration(color: Colors.green, shape: BoxShape.circle, border: Border.all(color: Colors.white)),
              child: Center(child: Text("${agent.success.length}", style: TextStyle(fontSize: 8))),
            )),
      ],
    );
  }

  Widget nodeText(String text) {
    if (text == "nowhere") {
      return Container(
          height: 70,
          width: 70,
          decoration: BoxDecoration(shape: BoxShape.circle, color: Colors.grey),
          child: Center(child: Text(text, style: TextStyle(fontSize: 8))));
    } else {
      return Container(
          height: 70,
          width: 70,
          decoration: BoxDecoration(shape: BoxShape.circle, color: Colors.blue, border: Border.all(color: Colors.white)),
          child: Center(child: Text(text, style: TextStyle(fontSize: 8))));
    }
  }

  _getColor(Agent agent) {
    if (agent.error.isNotEmpty) {
      return Colors.red[900];
    }
    return Colors.green[900];
  }

  @override
  Widget build(BuildContext context) {
    if (widget.agents.isEmpty) return Center(child: Text("There is nothing to show"));

    return SingleChildScrollView(
      child: Column(
        children: [
          GraphView(
            graph: graph,
            algorithm: SugiyamaAlgorithm(builder),
            paint: Paint()
              ..color = Colors.green
              ..strokeWidth = 1
              ..style = PaintingStyle.fill,
          ),
        ],
      ),
    );
  }
}
