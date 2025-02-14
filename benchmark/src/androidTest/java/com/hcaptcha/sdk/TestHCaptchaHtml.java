package com.hcaptcha.sdk;

import androidx.annotation.NonNull;

class TestHCaptchaHtml implements IHCaptchaHtmlProvider {

    @Override
    @NonNull
    public String getHtml() {
        return "<html>\n"
                + "<head>\n"
                + "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1, user-scalable=no\"/>\n"
                + "</head>\n"
                + "<body>\n"
                + "    <div id=\"hcaptcha-container\"></div>\n"
                + "    <script type=\"text/javascript\">\n"
                + "        console.assert(JSON.parse(window.JSDI.getDebugInfo()) instanceof Array);\n"
                + "        console.assert(typeof JSON.parse(window.JSDI.getSysDebug()) === 'object');\n"
                + "        console.assert(typeof JSON.parse(window.JSInterface.getConfig()) === 'object');\n"
                + "        var BridgeObject = window.JSInterface;\n"
                + "        function resetAndExecute(arg) {\n"
                + "            BridgeObject.onPass('token');\n"
                + "        }\n"
                + "        BridgeObject.onLoaded();\n"
                + "    </script>\n"
                + "</body>\n"
                + "</html>\n";
    }
}
