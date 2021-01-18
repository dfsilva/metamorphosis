import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:nats_message_processor_client/dto/processed_message.dart';
import 'package:nats_message_processor_client/service/agent_service.dart';
import 'package:nats_message_processor_client/service/service_locator.dart';

class ListProcessedMessages extends StatefulWidget {
  final String agenteUid;

  const ListProcessedMessages({Key key, this.agenteUid}) : super(key: key);

  @override
  _ListProcessedMessagesState createState() => _ListProcessedMessagesState();
}

class _ListProcessedMessagesState extends State<ListProcessedMessages> {
  ProcessorService _processorService = Services.get<ProcessorService>(ProcessorService);

  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(title: Text("Processed Messages")),
        body: Container(
          child: FutureBuilder(
            future: _processorService.processedMessages(widget.agenteUid),
            builder: (_, AsyncSnapshot<List<ProcessedMessage>> snp) {
              if (!snp.hasData) {
                return Center(child: Text("Carregando"));
              }

              if (snp.hasError) {
                return Center(child: Text("Erro ao carregar mensagens"));
              }

              return ListView.separated(
                  separatorBuilder: (__, index) => Padding(
                        padding: const EdgeInsets.symmetric(vertical: 10),
                        child: Container(
                          height: 1,
                          color: Colors.white,
                          child: Container(),
                        ),
                      ),
                  itemCount: snp.data.length,
                  itemBuilder: (__, index) {
                    final topicMessage = snp.data[index];

                    return Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 10),
                      child: Column(crossAxisAlignment: CrossAxisAlignment.start, mainAxisSize: MainAxisSize.min, children: [
                        Text("ID: ${topicMessage.id}"),
                        Text("Created: ${DateFormat('dd/MM/yyyy HH:mm:ss').format(topicMessage.created)}"),
                        Row(
                          mainAxisAlignment: MainAxisAlignment.start,
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text("IN: "),
                            Flexible(child: SelectableText(topicMessage.content, textAlign: TextAlign.left, )),
                          ],
                        ),
                        Row(
                          mainAxisAlignment: MainAxisAlignment.start,
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text("Conditional OUT: "),
                            Flexible(child: SelectableText(topicMessage.ifResult, textAlign: TextAlign.left)),
                          ],
                        ),
                        Row(
                          mainAxisAlignment: MainAxisAlignment.start,
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text("OUT: "),
                            (topicMessage.result != null && topicMessage.result.isNotEmpty)
                                ? Flexible(child: SelectableText(topicMessage.result, textAlign: TextAlign.left))
                                : SizedBox.shrink(),
                          ],
                        ),
                        Text("Processed: ${DateFormat('dd/MM/yyyy HH:mm:ss').format(topicMessage.processed)}"),
                        Row(
                          mainAxisAlignment: MainAxisAlignment.start,
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text("Delivered To: "),
                            Flexible(child: SelectableText(topicMessage.deliveredTo, textAlign: TextAlign.left))
                          ],
                        )
                      ]),
                    );
                  });
            },
          ),
        ),
      );
}
