import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:nats_message_processor_client/dto/topic_message.dart';

class ListTopicMessages extends StatefulWidget {
  final List<TopicMessage> topicMessages;

  const ListTopicMessages({Key key, this.topicMessages = const []}) : super(key: key);

  @override
  _ListTopicMessagesState createState() => _ListTopicMessagesState();
}

class _ListTopicMessagesState extends State<ListTopicMessages> {
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
                        Flexible(child: Text(topicMessage.content, textAlign: TextAlign.left)),
                      ],
                    ),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.start,
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text("OUT: "),
                        (topicMessage.result != null && topicMessage.result.isNotEmpty)
                            ? Flexible(child: Text(topicMessage.result, textAlign: TextAlign.left))
                            : SizedBox.shrink(),
                      ],
                    )
                  ]),
                );
              }),
        ),
      );
}
