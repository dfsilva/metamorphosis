import 'package:code_editor/code_editor.dart';
import 'package:flutter/material.dart';
import 'package:nats_message_processor_client/dto/agent.dart';
import 'package:nats_message_processor_client/service/agent_service.dart';
import 'package:nats_message_processor_client/service/service_locator.dart';

class AddAgentScreen extends StatefulWidget {
  final Agent agent;

  AddAgentScreen({Key key, this.agent}) : super(key: key);

  @override
  _AddAgentScreenState createState() => _AddAgentScreenState();
}

class _AddAgentScreenState extends State<AddAgentScreen> {
  ProcessorService _processorService = Services.get<ProcessorService>(ProcessorService);

  final _formKey = GlobalKey<FormState>();
  Agent _agent;

  @override
  void initState() {
    super.initState();
    this._agent = widget.agent ?? Agent(dataScript: "def messageToBeProcessed = message");
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text("Agent"),
      ),
      body: Column(
        children: [
          Expanded(
            child: Form(
                key: _formKey,
                child: ListView(
                  children: [
                    Padding(
                      padding: const EdgeInsets.symmetric(vertical: 5, horizontal: 10),
                      child: TextFormField(
                        keyboardType: TextInputType.text,
                        autofocus: true,
                        initialValue: _agent.title,
                        validator: (from) {
                          if (from.isEmpty) {
                            return "Please provide a title";
                          }
                          return null;
                        },
                        onSaved: (value) {
                          this._agent = this._agent.copyWith(title: value);
                        },
                        decoration: InputDecoration(hintText: "title", labelText: "title"),
                      ),
                    ),
                    Padding(
                      padding: const EdgeInsets.symmetric(vertical: 5, horizontal: 10),
                      child: TextFormField(
                        keyboardType: TextInputType.multiline,
                        maxLines: 5,
                        textInputAction: TextInputAction.newline,
                        initialValue: _agent.description,
                        onSaved: (value) {
                          this._agent = this._agent.copyWith(description: value);
                        },
                        decoration: InputDecoration(hintText: "description", labelText: "description"),
                      ),
                    ),

                    Padding(
                      padding: const EdgeInsets.symmetric(vertical: 5, horizontal: 10),
                      child: Row(
                        children: [
                          Switch(
                            value: this._agent.ordered,
                            onChanged: (value) {
                              setState(() {
                                this._agent = this._agent.copyWith(ordered: value);
                              });
                            },
                          ),
                          Text("ordered")
                        ],
                      ),
                    ),
                    Padding(
                      padding: const EdgeInsets.symmetric(vertical: 5, horizontal: 10),
                      child: DropdownButtonFormField(
                          value: this._agent.agentType,
                          items: <DropdownMenuItem>[
                            DropdownMenuItem(value: "D", child: Text("Default")),
                            DropdownMenuItem(value: "C", child: Text("Conditional"))
                          ],
                          onChanged: (value) {
                            setState(() {
                              this._agent = this._agent.copyWith(agentType: value);
                            });
                          },
                          onSaved: (type) {
                            this._agent = this._agent.copyWith(agentType: type);
                          }
                      ),
                    ),
                    Padding(
                      padding: const EdgeInsets.symmetric(vertical: 5, horizontal: 10),
                      child: TextFormField(
                        keyboardType: TextInputType.text,
                        initialValue: _agent.from,
                        validator: (from) {
                          if (from.isEmpty) {
                            return "Provide from queue";
                          }
                          return null;
                        },
                        onSaved: (from) {
                          this._agent = this._agent.copyWith(from: from);
                        },
                        decoration: InputDecoration(hintText: "from topic", labelText: "from"),
                      ),
                    ),
                    Padding(
                      padding: const EdgeInsets.symmetric(vertical: 5, horizontal: 10),
                      child: TextFormField(
                        keyboardType: TextInputType.text,
                        autofocus: true,
                        initialValue: _agent.to,
                        onSaved: (to) {
                          this._agent = this._agent.copyWith(to: to);
                        },
                        decoration: InputDecoration(hintText: "to topic", labelText: "to"),
                      ),
                    ),
                    (_agent.agentType == "C")
                        ? Padding(
                      padding: const EdgeInsets.symmetric(vertical: 5, horizontal: 10),
                      child: CodeEditor(
                          model: EditorModel(
                            files: [FileEditor(name: "Conditional Script", language: "java", code: _agent.ifscript)],
                            styleOptions: new EditorModelStyleOptions(
                              fontSize: 13,
                            ),
                          ),
                          disableNavigationbar: false,
                          onSubmit: (String language, String value) {
                            this._agent = this._agent.copyWith(ifscript: value);
                          }),
                    )
                        : SizedBox.shrink(),
                    (_agent.agentType == "C")
                        ? Padding(
                      padding: const EdgeInsets.symmetric(vertical: 5, horizontal: 10),
                      child: TextFormField(
                        keyboardType: TextInputType.text,
                        autofocus: true,
                        initialValue: _agent.to2,
                        onSaved: (to2) {
                          this._agent = this._agent.copyWith(to2: to2);
                        },
                        decoration: InputDecoration(hintText: "to topic 1", labelText: "to topic 1"),
                      ),
                    )
                        : SizedBox.shrink(),
                    Padding(
                        padding: const EdgeInsets.symmetric(vertical: 5, horizontal: 10),
                        child: CodeEditor(
                            model: EditorModel(
                              files: [FileEditor(name: "Transformation Script", language: "java", code: _agent.dataScript)],
                              styleOptions: new EditorModelStyleOptions(
                                fontSize: 13,
                              ),
                            ),
                            disableNavigationbar: false,
                            onSubmit: (String language, String value) {
                              this._agent = this._agent.copyWith(dataScript: value);
                            })),
                  ],
                )),
          ),
          Padding(
            padding: const EdgeInsets.only(bottom: 20, top: 10),
            child: RaisedButton(
                child: Text("Salvar"),
                onPressed: () {
                  if (_formKey.currentState.validate()) {
                    _formKey.currentState.save();
                  }
                  _processorService.addOrUpdate(this._agent).then((value) => Navigator.of(context).pop());
                }),
          )
        ],
      ),
    );
  }
}
