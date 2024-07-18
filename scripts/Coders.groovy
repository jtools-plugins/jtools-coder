import dev.coolrequest.tool.views.coder.Coder
import dev.coolrequest.tool.views.coder.Kind

class TextToHexCoder implements Coder{

    String transform(String data){
        return data
    }

    Kind kind() {
        return Kind.of("text","hex")
    }
}

class TextToHex2Coder implements Coder {
    String transform(String data){
        return data
    }

    Kind kind() {
        return Kind.of("text","hex2")
    }
}