package br.com.diego.processor.proccess

import groovy.lang.{Binding, GroovyShell}

import scala.util.Try

object RuntimeProcessor {
  def apply(code: String, message: String) = new RuntimeProcessor(code, message)
}

class RuntimeProcessor(code: String, message: String) {
  def process: Try[String] = {
    val binding = new Binding()
    binding.setVariable("message", message)
    val shell = new GroovyShell(binding)
    Try(shell.evaluate(code).toString)
  }
}
