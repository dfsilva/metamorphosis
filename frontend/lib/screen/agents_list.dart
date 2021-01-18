import 'package:code_editor/code_editor.dart';
import 'package:flutter/material.dart';
import 'package:nats_message_processor_client/dto/agent.dart';
import 'package:nats_message_processor_client/screen/add_agent.dart';
import 'package:nats_message_processor_client/screen/list_processed_messages.dart';
import 'package:nats_message_processor_client/screen/list_not_processed_messages.dart';

class AgentsList extends StatefulWidget {
  final List<Agent> agents;

  const AgentsList({Key key, this.agents}) : super(key: key);

  @override
  _AgentsListState createState() => _AgentsListState();
}

class _AgentsListState extends State<AgentsList> {
  @override
  Widget build(BuildContext context) {
    if (widget.agents.isEmpty) {
      return Center(child: Text("There is nothing to show"));
    }

    return ListView.builder(
        itemCount: widget.agents.length,
        itemBuilder: (___, index) {
          final agent = widget.agents[index];
          return Card(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                ListTile(
                  leading: Icon(Icons.album),
                  title: Row(
                    children: [Text(agent.title), Text(agent.agentType == "D" ? " - (Default)" : " - (Conditional)")],
                  ),
                  subtitle: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    mainAxisSize: MainAxisSize.min,
                    children: [Text(agent.description, style: TextStyle(fontSize: 10)), Text(agent.uuid, style: TextStyle(fontSize: 10)), Text("Ordered: ${agent.ordered}")],
                  ),
                  trailing: PopupMenuButton<int>(
                    onSelected: (selected) {
                      switch (selected) {
                        case 1:
                          Navigator.of(context).push(MaterialPageRoute(builder: (__) => AddAgentScreen(agent: agent)));
                      }
                    },
                    child: Icon(Icons.more_vert),
                    itemBuilder: (context) => [
                      PopupMenuItem(
                        value: 1,
                        child: Text("Alterar"),
                      )
                    ],
                  ),
                ),
                ...(agent.agentType == "C")
                    ? [
                        Text("Conditional Script"),
                        Container(
                          height: 100,
                          child: SingleChildScrollView(
                            child: CodeEditor(
                              model: EditorModel(
                                files: [FileEditor(name: "codigo", language: "java", code: agent.ifscript)],
                                styleOptions: EditorModelStyleOptions(
                                  fontSize: 10,
                                ),
                              ),
                              disableNavigationbar: true,
                              edit: false,
                            ),
                          ),
                        )
                      ]
                    : [],
                Text("Transformer Script"),
                Container(
                  height: 200,
                  child: SingleChildScrollView(
                    child: CodeEditor(
                      model: EditorModel(
                        files: [FileEditor(name: "codigo", language: "java", code: agent.dataScript)],
                        styleOptions: EditorModelStyleOptions(
                          fontSize: 13,
                        ),
                      ),
                      disableNavigationbar: true,
                      edit: false,
                    ),
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    mainAxisSize: MainAxisSize.max,
                    children: <Widget>[
                      InkWell(
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [Icon(Icons.pause_circle_filled, color: Colors.yellow[800]), Text("${agent.waiting.length}")],
                        ),
                        onTap: () {
                          showDialog(
                              context: context,
                              builder: (_) => Dialog(
                                    child: ListNotProcessedMessages(topicMessages: agent.waiting),
                                  ));
                        },
                      ),
                      InkWell(
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [Icon(Icons.run_circle, color: Colors.blue), Text("${agent.processing.length}")],
                        ),
                        onTap: () {
                          showDialog(
                              context: context,
                              builder: (_) => Dialog(
                                    child: ListNotProcessedMessages(topicMessages: agent.processing),
                                  ));
                        },
                      ),
                      InkWell(
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [Icon(Icons.error, color: Colors.red), Text("${agent.error.length}")],
                        ),
                        onTap: () {
                          showDialog(
                              context: context,
                              builder: (_) => Dialog(
                                    child: ListNotProcessedMessages(topicMessages: agent.error),
                                  ));
                        },
                      ),
                      InkWell(
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [Icon(Icons.verified, color: Colors.green), Text("${agent.success.length}")],
                        ),
                        onTap: () {
                          showDialog(
                              context: context,
                              builder: (_) => Dialog(
                                    child: ListProcessedMessages(agenteUid: agent.uuid),
                                  ));
                        },
                      ),
                    ],
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    crossAxisAlignment: CrossAxisAlignment.center,
                    mainAxisSize: MainAxisSize.max,
                    children: <Widget>[
                      Text("FROM: ${agent.from}"),
                      Icon(Icons.arrow_forward),
                      (agent.agentType == "C")
                          ? Column(
                              children: [
                                Text("TO (in case of true): ${agent.to}"),
                                Text("TO (in case of false): ${agent.to2}"),
                              ],
                            )
                          : Text("TO: ${agent.to}")
                    ],
                  ),
                ),
              ],
            ),
          );
        });
  }
}
