import groovy.json.JsonOutput
import groovy.json.JsonSlurper
def object = new JsonSlurper().parseText(message)
object.sobrenome = "Ferreira da Silva"
return JsonOutput.toJson(object)



//def message = "{\"nome\": \"diego\"}"

