import 'package:flutter/foundation.dart';
import 'package:nats_message_processor_client/bus/rx_bus.dart';

abstract class BaseService<S> {
  final RxBus _rxBus;
  final S _store;

  BaseService(this._rxBus, this._store) {
    this._rxBus.subscribe().where(filter).listen((event) {
      onReceiveMessage(event);
    });
  }

  S store() => _store;

  bool filter(dynamic event) => true;

  @protected
  RxBus bus() => _rxBus;

  void onReceiveMessage(dynamic msg);

  void dispose();
}
