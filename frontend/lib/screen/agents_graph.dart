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
  SugiyamaConfiguration builder = SugiyamaConfiguration();

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
      Map<String, Node> nodesFrom = Map.fromIterable(widget.agents, key: (e) => e.from, value: (e) => Node(nodeText(e.from, e)));
      Map<String, Node> nodesTo = Map.fromIterable(widget.agents,
          key: (e) => e.getTo(),
          value: (e) {
            if (nodesFrom.containsKey(e.getTo())) {
              return nodesFrom[e.getTo()];
            } else {
              return Node(nodeText(e.getTo(), e));
            }
          });

      widget.agents.forEach((element) {
        if (element.to == null || element.to.isEmpty) {
          graph.addEdge(nodesFrom[element.from], nodesTo[element.getTo()], paint: Paint()..color = Colors.red);
        } else {
          graph.addEdge(nodesFrom[element.from], nodesTo[element.getTo()]);
        }
      });

      builder
        ..nodeSeparation = (50)
        ..levelSeparation = (50)
        ..orientation = SugiyamaConfiguration.ORIENTATION_TOP_BOTTOM;
    }
  }

  Widget nodeText(String text, Agent agent) {
    if (text == "nowhere") {
      return Container(
          height: 80,
          width: 80,
          decoration: BoxDecoration(shape: BoxShape.circle, color: Colors.grey),
          child: Center(child: Text(text, style: TextStyle(fontSize: 12))));
    } else {
      return Stack(
        clipBehavior: Clip.none,
        children: [
          Container(
              height: 80,
              width: 80,
              decoration: BoxDecoration(shape: BoxShape.circle, color: _getColor(agent), border: Border.all(color: Colors.white)),
              child: Center(child: Text(text, style: TextStyle(fontSize: 10)))),
          Positioned(
              top: 0,
              right: 0,
              child: Container(
                padding: EdgeInsets.all(5),
                decoration: BoxDecoration(color: Colors.red, shape: BoxShape.circle, border: Border.all(color: Colors.white)),
                child: Text("${agent.error.length}", style: TextStyle(fontSize: 10)),
              )),
          Positioned(
              top: 25,
              right: -15,
              child: Container(
                padding: EdgeInsets.all(5),
                decoration: BoxDecoration(color: Colors.green, shape: BoxShape.circle, border: Border.all(color: Colors.white)),
                child: Text("0", style: TextStyle(fontSize: 10)),
              )),
          Positioned(
              top: 52,
              right: 0,
              child: Container(
                padding: EdgeInsets.all(5),
                decoration: BoxDecoration(color: Colors.blue, shape: BoxShape.circle, border: Border.all(color: Colors.white)),
                child: Text("0", style: TextStyle(fontSize: 10)),
              ))
        ],
      );
    }
  }

  Widget conditionalWidget(String text, Agent agent) {
    if (text == "nowhere") {
      return RotatedBox(
        quarterTurns: 1,
        child: Container(
            clipBehavior: Clip.antiAlias,
            height: 80,
            width: 80,
            decoration: BoxDecoration(color: Colors.grey),
            child: Center(child: Text(text, style: TextStyle(fontSize: 12)))),
      );
    } else {
      return Stack(
        clipBehavior: Clip.none,
        children: [
          Container(
              height: 80,
              width: 80,
              decoration: BoxDecoration(shape: BoxShape.circle, color: _getColor(agent), border: Border.all(color: Colors.white)),
              child: Center(child: Text(text, style: TextStyle(fontSize: 10)))),
          Positioned(
              top: 0,
              right: 0,
              child: Container(
                padding: EdgeInsets.all(5),
                decoration: BoxDecoration(color: Colors.red, shape: BoxShape.circle, border: Border.all(color: Colors.white)),
                child: Text("${agent.error.length}", style: TextStyle(fontSize: 10)),
              )),
          Positioned(
              top: 25,
              right: -15,
              child: Container(
                padding: EdgeInsets.all(5),
                decoration: BoxDecoration(color: Colors.green, shape: BoxShape.circle, border: Border.all(color: Colors.white)),
                child: Text("0", style: TextStyle(fontSize: 10)),
              )),
          Positioned(
              top: 52,
              right: 0,
              child: Container(
                padding: EdgeInsets.all(5),
                decoration: BoxDecoration(color: Colors.blue, shape: BoxShape.circle, border: Border.all(color: Colors.white)),
                child: Text("0", style: TextStyle(fontSize: 10)),
              ))
        ],
      );
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

    return Column(
      mainAxisSize: MainAxisSize.max,
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
    );
  }
}
