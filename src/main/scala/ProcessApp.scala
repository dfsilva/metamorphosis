import groovy.lang.{Binding, GroovyShell}

import scala.util.{Failure, Success, Try}

object ProcessApp extends App {

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

  val shell = new GroovyShell(binding);
  Try(shell.evaluate(codigoDinamico)) match {
    case Success(value) => print(s"Sucesso na execucao: ${value}")
    case Failure(exception) => print(s"Problema na execucao ${exception.getMessage}")
  }

}
