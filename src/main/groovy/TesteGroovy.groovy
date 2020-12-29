/**
 * Script  lflasdjflaksjdflkaj sd
 * lfajk slkdfj alsdj
 * ljkf alsjdf a
 *
 */
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

//def message = "{\"nome\": \"diego\"}"

def object = new JsonSlurper().parseText(message)
object.propriedadeExtra = "Propriedade Extra Adicionada"
return JsonOutput.toJson(object)


