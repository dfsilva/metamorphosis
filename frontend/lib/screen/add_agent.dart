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
    this._agent = widget.agent ?? Agent(code: "def messageToBeProcessed = message");
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text("Add Agent"),
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
                            return "Provide a title";
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
                      child: TextFormField(
                        keyboardType: TextInputType.text,
                        initialValue: _agent.from,
                        enabled: _agent.uuid == null,
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
                    Padding(
                        padding: const EdgeInsets.symmetric(vertical: 5, horizontal: 10),
                        child: CodeEditor(
                            model: EditorModel(
                              files: [FileEditor(name: "Groovy Script", language: "java", code: _agent.code)],
                              styleOptions: new EditorModelStyleOptions(
                                fontSize: 13,
                              ),
                            ),
                            disableNavigationbar: false,
                            onSubmit: (String language, String value) {
                              this._agent = this._agent.copyWith(code: value);
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
