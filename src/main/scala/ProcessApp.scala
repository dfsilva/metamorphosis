import groovy.lang.{Binding, GroovyShell}

import scala.util.{Failure, Success, Try}

object ProcessApp extends App {

  val codigoDinamico =
    """
      def hello_world(String name){
        def get = new URL("https://httpbin.org/get").openConnection();
        def getRC = get.getResponseCode();
        println(getRC);
        if(getRC.equals(200)) {
            println(get.getInputStream().getText());
        }
        println 'Hello '+name
      };

      hello_world(parametroDinamico);
      """
  val binding = new Binding()
  binding.setVariable("parametroDinamico", "Diego Ferreira da Silva")

  val shell = new GroovyShell(binding);
  Try(shell.evaluate(codigoDinamico)) match {
    case Success(value) => print("Sucesso na execucao")
    case Failure(exception) => print(s"Problema na execucao ${exception.getMessage}")
  }

}
