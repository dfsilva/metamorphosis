import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:nats_message_processor_client/dto/topic_message.dart';

class ListNotProcessedMessages extends StatefulWidget {
  final List<TopicMessage> topicMessages;

  const ListNotProcessedMessages({Key key, this.topicMessages = const []}) : super(key: key);

  @override
  _ListNotProcessedMessagesState createState() => _ListNotProcessedMessagesState();
}

class _ListNotProcessedMessagesState extends State<ListNotProcessedMessages> {
  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(title: Text("Topic Messages")),
        body: Container(
          child: ListView.separated(
              separatorBuilder: (__, index) => Padding(
                    padding: const EdgeInsets.symmetric(vertical: 10),
                    child: Container(
                      height: 1,
                      color: Colors.white,
                      child: Container(),
                    ),
                  ),
              itemCount: widget.topicMessages.length,
              itemBuilder: (__, index) {
                final topicMessage = widget.topicMessages[index];

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
                          Flexible(
                              child: SelectableText(
                            topicMessage.content,
                            textAlign: TextAlign.left,
                          )),
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
                        children: [Text("Delivered To: "), Flexible(child: SelectableText(topicMessage.deliveredTo, textAlign: TextAlign.left))],
                      )
                    ]));
              }),
        ),
      );
}
