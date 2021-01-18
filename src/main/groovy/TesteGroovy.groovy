/**
 * Script  lflasdjflaksjdflkaj sd
 * lfajk slkdfj alsdj
 * ljkf alsjdf a
 *
 */
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

//def message = "{\"nome\": \"Auto de teste\"}"

def object = new JsonSlurper().parseText(message)
object.propriedadeExtra = "Propriedade Extra Adicionada"
Thread.sleep(2000)

return JsonOutput.toJson(object)

//print Math.round(Math.random() * 100) % 2


