import 'package:rxdart/rxdart.dart';

class RxBus {
  BehaviorSubject<dynamic> _bus = BehaviorSubject<dynamic>();

  T send<T>(T msg, {int seconds}) {
    if (seconds != null) {
      Future.delayed(Duration(seconds: seconds), () => _bus.add(msg));
    } else {
      _bus.add(msg);
    }
    return msg;
  }

  BehaviorSubject<dynamic> subscribe() {
    return _bus;
  }

  hasListeners() {
    return _bus.hasListener;
  }

  void close() {
    _bus?.close();
  }
}
