import 'package:flutter/material.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_mobx/flutter_mobx.dart';
import 'package:nats_message_processor_client/dto/agent.dart';
import 'package:nats_message_processor_client/screen/add_agent.dart';
import 'package:nats_message_processor_client/screen/agents_graph.dart';
import 'package:nats_message_processor_client/screen/agents_list.dart';
import 'package:nats_message_processor_client/service/agent_service.dart';
import 'package:nats_message_processor_client/service/service_locator.dart';

class ListProcessorsScreen extends StatefulWidget {
  @override
  _ListProcessorsScreenState createState() => _ListProcessorsScreenState();
}

class _ListProcessorsScreenState extends State<ListProcessorsScreen> {
  ProcessorService _processorService = Services.get<ProcessorService>(ProcessorService);

  final _pageController = PageController(keepPage: true, initialPage: 0);
  int _currentPage = 0;

  @override
  void initState() {
    super.initState();
    _pageController.addListener(() {
      setState(() {
        _currentPage = _pageController.page.round();
      });
    });
  }

  @override
  void dispose() {
    _pageController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(title: Text("Agents")),
        body: Observer(
          builder: (_) {
            List<Agent> agents = _processorService.store().agents.values.toList();

            return PageView(
              controller: _pageController,
              children: [AgentsList(agents: agents), GraphAgents(agents: agents)],
            );
          },
        ),
        bottomNavigationBar: AnimatedBuilder(
          animation: _pageController,
          builder: (_, __) => BottomNavigationBar(
              onTap: (index) {
                _pageController.jumpToPage(index);
              },
              currentIndex: _currentPage,
              items: [
                BottomNavigationBarItem(icon: Icon(Icons.list), label: "List"),
                BottomNavigationBarItem(icon: Icon(Icons.bubble_chart_rounded), label: "Graph")
              ]),
        ),
        floatingActionButton: FloatingActionButton(
          onPressed: () {
            Navigator.of(context).push(MaterialPageRoute(builder: (_) => AddAgentScreen()));
          },
          child: Icon(Icons.add),
        ),
      );
}
