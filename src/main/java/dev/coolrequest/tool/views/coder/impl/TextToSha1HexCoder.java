package dev.coolrequest.tool.views.coder.impl;

import dev.coolrequest.tool.views.coder.Coder;
import dev.coolrequest.tool.views.coder.Kind;
import org.apache.commons.codec.digest.DigestUtils;

public class TextToSha1HexCoder implements Coder {
    @Override
    public String transform(String data) {
        return DigestUtils.sha1Hex(data);
    }

    @Override
    public Kind kind() {
        return Kind.of("text", "sha1");
    }
}
