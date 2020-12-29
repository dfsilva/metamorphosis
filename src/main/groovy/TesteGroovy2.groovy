import groovy.json.JsonOutput
import groovy.json.JsonSlurper

//def message = "{\"nome\": \"diego\"}"

def getPosts(){
    def get = new URL("https://jsonplaceholder.typicode.com/comments?postId="+1).openConnection()
    def getRC = get.getResponseCode()
    if(getRC == 200) {
        return (get.getInputStream().getText());
    }
    return '[]'
};

def posts = new JsonSlurper().parseText(getPosts())
def object = new JsonSlurper().parseText(message)
object.posts = posts
return JsonOutput.toJson(object)


