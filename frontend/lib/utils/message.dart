// import 'package:bot_toast/bot_toast.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
// import 'package:smshared/localization.dart';

showInfo(String text) {
  // BotToast.showText(
  //     text: text ?? "",
  //     contentColor: Colors.green[800],
  //     duration: Duration(seconds: 5),
  //     clickClose: true,
  //     textStyle: TextStyle(color: Colors.white));
}

showError(String text) {
  // BotToast.showText(
  //     text: text ?? "",
  //     contentColor: Colors.red,
  //     duration: Duration(seconds: 5),
  //     clickClose: true,
  //     textStyle: TextStyle(color: Colors.white));
}

showErrorException(dynamic error, {String message}) {
  if (error is PlatformException)
    showError("$message ${error.message}");
  else if (error is Exception)
    showError("$message ${error.toString()}");
  else
    showError(message ?? "Ocorreu um erro");
}

showConfirm({BuildContext context, String text, Function onCancel, Function onConfirm}) {
  showDialog(
    context: context,
    builder: (BuildContext context) {
      return AlertDialog(
        title: Text("Confirmação"),
        content: Text(text),
        actions: <Widget>[
          FlatButton(
            child: Text("Cancelar", style: Theme.of(context).textTheme.button.copyWith(color: Colors.red)),
            onPressed: () {
              Navigator.of(context).pop();
              onCancel();
            },
          ),
          FlatButton(
            child:
            Text("Confirmar", style: Theme.of(context).textTheme.button.copyWith(color: Colors.green)),
            onPressed: () {
              Navigator.of(context).pop();
              onConfirm();
            },
          ),
        ],
      );
    },
  );
}