package br.com.diego.processor.proccess

import groovy.lang.{Binding, GroovyShell}

import scala.util.Try

object RuntimeProcessor {
  def apply(code: String) = new RuntimeProcessor(code)
}

class RuntimeProcessor(code: String) {
  def process: Try[Any] = {
        val codigoDinamico =
            """
              def hello_world(String id){
                def get = new URL("https://jsonplaceholder.typicode.com/comments?postId="+id).openConnection();
                def getRC = get.getResponseCode();
                if(getRC.equals(200)) {
                    return (get.getInputStream().getText());
                }
                return '[]'
              };

              return hello_world(parametroDinamico);
              """
    val binding = new Binding()
    binding.setVariable("parametroDinamico", "2")
    val shell = new GroovyShell(binding)
    Try(shell.evaluate(code))
  }

}
