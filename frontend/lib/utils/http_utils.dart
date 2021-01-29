import 'dart:async';
import 'dart:convert';

import 'package:http/http.dart' as http;
import 'package:nats_message_processor_client/utils/message.dart';

class Api {
  static getWsUrl() {
    print("ws://${Uri.base.host}:${Uri.base.port}/ws");
    return "ws://${Uri.base.host}:${Uri.base.port}/ws";
    // return "ws://localhost:8081/ws";
  }

  static getApiUrl() {
    print("${Uri.base.scheme}://${Uri.base.host}:${Uri.base.port}/api");
    return "${Uri.base.scheme}://${Uri.base.host}:${Uri.base.port}/api";
    // return "http://localhost:8081/api";
  }

  static handleError(error) {
    showError(error.message);
    throw error;
  }

  static Future<dynamic> doPost({String url, String uri, Map<String, dynamic> bodyParams = const {}}) async {
    try {
      String currentToken = await _getUserToken();
      final response = await http.post(
        (url ?? getApiUrl()) + uri,
        headers: <String, String>{'Content-Type': 'application/json; charset=UTF-8', "Authorization": "Token $currentToken"},
        body: json.encode(bodyParams),
      );

      if (response.statusCode == 200) {
        return json.decode(utf8.decode(response.bodyBytes));
      } else {
        return handleError(Exception(utf8.decode(response.bodyBytes)));
      }
    } catch (e) {
      handleError(e);
    }
  }

  static Future<dynamic> doPut({String url, String uri, Map<String, dynamic> bodyParams = const {}}) async {
    String currentToken = await _getUserToken();
    final response = await http.put(
      (url ?? getApiUrl()) + uri,
      headers: <String, String>{'Content-Type': 'application/json; charset=UTF-8', "Authorization": "Token $currentToken"},
      body: json.encode(bodyParams),
    );

    if (response.statusCode == 200) {
      return json.decode(utf8.decode(response.bodyBytes));
    } else {
      return handleError(Exception(utf8.decode(response.bodyBytes)));
    }
  }

  static Future<dynamic> doGet({String url, String uri, Map<String, dynamic> params = const {}}) async {
    String currentToken = await _getUserToken();
    final response = await http.get(
      (url ?? getApiUrl()) + uri + params.entries
          .where((entry) => (entry.value != null && entry.value.toString().isNotEmpty))
          .fold("?", (previousValue, element) => previousValue + "${element.key}=${element.value}&"),
      headers: <String, String>{'Content-Type': 'application/json; charset=UTF-8', "Authorization": "Token $currentToken"},
    );

    if (response.statusCode == 200) {
      return json.decode(utf8.decode(response.bodyBytes));
    } else {
      return handleError(Exception(utf8.decode(response.bodyBytes)));
    }
  }

  static _getUserToken() async {
    return "";
  }
}
