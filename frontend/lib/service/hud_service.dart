import 'package:nats_message_processor_client/bus/actions.dart';
import 'package:nats_message_processor_client/service/base_service.dart';
import 'package:nats_message_processor_client/store/hud_store.dart';

class HudService extends BaseService<HudStore> {
  HudService(rxBus) : super(rxBus, HudStore());

  @override
  void dispose() {}

  @override
  bool filter(event) => [ShowHud, HideHud].contains(event.runtimeType);

  @override
  void onReceiveMessage(msg) {
    if (msg is ShowHud) {
      store().showHud(msg.text);
    }
    if (msg is HideHud) {
      store().hideHud();
    }
  }
}
