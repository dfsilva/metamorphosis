import 'package:mobx/mobx.dart';

part 'connection_store.g.dart';

class ConnectionStore = _ConnectionStore with _$ConnectionStore;

abstract class _ConnectionStore with Store {
  @observable
  bool connected = false;

  @action
  setConnected(bool connected) {
    this.connected = connected;
  }
}
