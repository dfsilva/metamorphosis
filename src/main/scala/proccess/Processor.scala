package proccess

import groovy.lang.{Binding, GroovyShell}

import scala.util.Try

object Processor {
  def apply(code: String) = new Processor(code)
}

class Processor(code: String) {
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

    //    match {
    //      case Success(value) => value.toString
    //      case Failure(exception) => exception.getMessage
    //    }
  }

}
