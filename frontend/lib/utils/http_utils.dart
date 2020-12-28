import 'dart:async';
import 'dart:convert';

import 'package:http/http.dart' as http;
import 'package:nats_message_processor_client/utils/message.dart';

class Api {
  static const String HOST = "127.0.0.1:8081";
  static const String _URL = "http://$HOST/api";

  static handleError(error) {
    // if (error is DioError) {
    //   if (error.response != null) {
    //     showError(error.response.data);
    //   } else {
    //     if (error.error.toString().isNotEmpty)
    //       showError(error.error.toString());
    //     else
    //       showError("Erro inesperado");
    //   }
    //   throw error;
    // } else {
    showError(error.message);
    throw error;
    // }
  }

  static Future<dynamic> doPost({String url = _URL, String uri, Map<String, dynamic> bodyParams = const {}}) async {
    try{
      String currentToken = await _getUserToken();
      final response = await http.post(
        url + uri,
        headers: <String, String>{'Content-Type': 'application/json; charset=UTF-8', "Authorization": "Token $currentToken"},
        body: json.encode(bodyParams),
      );

      if (response.statusCode == 200) {
        return json.decode(response.body);
      } else {
        handleError(Exception(response.body));
      }
    }catch(e){
      handleError(e);
    }
  }

  static Future<dynamic> doPut({String url = _URL, String uri, Map<String, dynamic> bodyParams = const {}}) async {
    String currentToken = await _getUserToken();
    final response = await http.put(
      url + uri,
      headers: <String, String>{'Content-Type': 'application/json; charset=UTF-8', "Authorization": "Token $currentToken"},
      body: json.encode(bodyParams),
    );

    if (response.statusCode == 200) {
      return json.decode(response.body);
    } else {
      handleError(Exception(response.body));
    }
  }

  // static Future<dynamic> doPut({String url = _URL, String uri, Map<String, dynamic> bodyParams = const {}}) async {
  //   print(url + uri);
  //   String currentToken = await _getUserToken();
  //   return _dio
  //       .put(url + uri, data: bodyParams, options: Options(headers: {"Content-Type": "application/json", "Authorization": "Token $currentToken"}))
  //       .then((response) {
  //     if (response.statusCode == 200) {
  //       return response.data;
  //     } else {
  //       throw Exception(response.statusMessage);
  //     }
  //   }).catchError(handleError);
  // }

  static Future<dynamic> doGet({String url = _URL, String uri, Map<String, dynamic> params = const {}}) async {
    print(url + uri);
    String currentToken = await _getUserToken();
    final response = await http.get(
      url + uri + params.entries.fold("?", (previousValue, element) => previousValue + "${element.key}=${element.value}"),
      headers: <String, String>{'Content-Type': 'application/json; charset=UTF-8', "Authorization": "Token $currentToken"},
    );

    if (response.statusCode == 200) {
      return json.decode(response.body);
    } else {
      handleError(Exception(response.body));
    }
  }

  // static Future<dynamic> doDelete({String url = _URL, String uri, Map<String, dynamic> params = const {}}) async {
  //   String currentToken = await _getUserToken();
  //   return _dio
  //       .delete(url + uri,
  //           queryParameters: params, options: Options(headers: {"Content-Type": "application/json", "Authorization": "Token $currentToken"}))
  //       .then((response) {
  //     if (response.statusCode == 200) {
  //       return response.data;
  //     } else {
  //       throw Exception(response.statusMessage);
  //     }
  //   }).catchError(handleError);
  // }

  static _getUserToken() async {
    return "";
  }
}
