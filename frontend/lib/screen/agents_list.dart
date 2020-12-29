import 'package:code_editor/code_editor.dart';
import 'package:flutter/material.dart';
import 'package:nats_message_processor_client/dto/agent.dart';
import 'package:nats_message_processor_client/screen/add_agent.dart';
import 'package:nats_message_processor_client/screen/list_topic_messages.dart';

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
            children: <Widget>[
              ListTile(
                leading: Icon(Icons.album),
                title: Text(agent.title),
                subtitle: Text(agent.description),
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
              Container(
                height: 100,
                child: SingleChildScrollView(
                  child: CodeEditor(
                    model: EditorModel(
                      files: [FileEditor(name: "codigo", language: "java", code: agent.transformerScript)],
                      styleOptions: new EditorModelStyleOptions(
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
                        children: [Icon(Icons.directions_run, color: Colors.blue), Text("${agent.waiting.length}")],
                      ),
                      onTap: () {
                        showDialog(
                            context: context,
                            builder: (_) => Dialog(
                              child: ListTopicMessages(topicMessages: agent.waiting),
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
                              child: ListTopicMessages(topicMessages: agent.error),
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
                              child: ListTopicMessages(topicMessages: agent.success.values.toList()),
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
                  mainAxisSize: MainAxisSize.max,
                  children: <Widget>[Text("FROM:   ${agent.from}"), Icon(Icons.arrow_forward), Text("TO:    ${agent.to}")],
                ),
              ),
            ],
          ),
        );
      });}
}
