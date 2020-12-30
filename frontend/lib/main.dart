import 'package:bot_toast/bot_toast.dart';
import 'package:flutter/material.dart';
import 'package:nats_message_processor_client/bus/actions.dart';
import 'package:nats_message_processor_client/bus/rx_bus.dart';
import 'package:nats_message_processor_client/parent.dart';
import 'package:nats_message_processor_client/routes.dart';
import 'package:nats_message_processor_client/screen/home.dart';
import 'package:nats_message_processor_client/service/connection_service.dart';
import 'package:nats_message_processor_client/service/hud_service.dart';
import 'package:nats_message_processor_client/service/agent_service.dart';
import 'package:nats_message_processor_client/service/service_locator.dart';
import 'package:nats_message_processor_client/utils/navigator.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();

  final _rxBus = new RxBus();
  Services.add(ConnectionService(_rxBus));
  Services.add(HudService(_rxBus));
  Services.add(ProcessorService(_rxBus));

  _rxBus.send(WsConnect());

  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  @override
  Widget build(BuildContext context) => MaterialApp(
        title: 'Events Message Transformer',
        debugShowCheckedModeBanner: false,
        navigatorObservers: [BotToastNavigatorObserver()],
        navigatorKey: NavigatorUtils.nav,
        theme: ThemeData(
          brightness: Brightness.light,
        ),
        darkTheme: ThemeData(
          brightness: Brightness.dark,
        ),
        themeMode: ThemeMode.dark,
        routes: {Routes.HOME: (context) => HomeScreen()},
        builder: (ctx, widget) => BotToastInit()(ctx, ParentWidget(widget)),
        initialRoute: Routes.HOME,
      );
}
