import 'package:mobx/mobx.dart';

part 'hud_store.g.dart';

class HudStore = _HudStore with _$HudStore;

abstract class _HudStore with Store {
  @observable
  bool loading = false;
  @observable
  String text = "Carregando...";

  @action
  showHud(String text) {
    this.loading = true;
    this.text = text ?? "Carregando...";
  }

  @action
  hideHud() {
    this.loading = false;
  }
}
