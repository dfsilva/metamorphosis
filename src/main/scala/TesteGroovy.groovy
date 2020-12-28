import groovy.json.JsonOutput
import groovy.json.JsonSlurper
def object = new JsonSlurper().parseText(message)
object.propriedadeExtra = "Propriedade Extra Adicionada"
return JsonOutput.toJson(object)



//def message = "{\"nome\": \"diego\"}"

