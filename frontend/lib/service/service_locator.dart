class Services {
  static Map<Type, dynamic> _instances = new Map();

  static add(dynamic service) {
    _instances[service.runtimeType] = service;
  }

  static T get<T>(Type type) {
    return _instances[type];
  }
}
