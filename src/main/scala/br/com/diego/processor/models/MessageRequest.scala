package br.com.diego.processor.models

import br.com.diego.processor.CborSerializable

case class MessageRequest(code: String, fromQueue: String, toQueue: String) extends CborSerializable

