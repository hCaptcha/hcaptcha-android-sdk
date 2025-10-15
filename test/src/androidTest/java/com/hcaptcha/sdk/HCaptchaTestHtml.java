package com.hcaptcha.sdk;

import androidx.annotation.NonNull;

class HCaptchaTestHtml implements IHCaptchaHtmlProvider {

    private final boolean callBridgeOnLoaded;

    HCaptchaTestHtml() {
        this(true);
    }

    HCaptchaTestHtml(boolean callBridgeOnLoaded) {
        this.callBridgeOnLoaded = callBridgeOnLoaded;
    }

    @Override
    @NonNull
    public String getHtml() {
        return "<html>\n"
                + "<head>\n"
                + "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1, user-scalable=no\"/>\n"
                + "</head>\n"
                + "<body>\n"
                + "    <div id=\"hcaptcha-container\"></div>\n"
                + "    <input id=\"input-text\" />\n"
                + "    <button id=\"on-error\" onclick=\"onError()\">Error</button>\n"
                + "    <button id=\"on-pass\" onclick=\"onPass()\">Pass</button>\n"
                + "    <a id=\"on-sms\" href=\"sms:+123-456-789?body=Hello%20World\">Send SMS</a>\n"
                + "    <script type=\"text/javascript\">\n"
                + "        console.assert(window.JSDI.getDebugInfo() instanceof Array);\n"
                + "        console.assert(typeof window.JSDI.getSysDebug() === 'object');\n"
                + "        var BridgeObject = window.JSInterface;\n"
                + "        var bridgeConfig = JSON.parse(BridgeObject.getConfig());\n"
                + "        function onHcaptchaLoaded() {\n"
                + "            try {\n"
                + "                BridgeObject.onLoaded();\n"
                + "            } catch (e) {\n"
                + "                BridgeObject.onError(29);\n"
                + "            }\n"
                + "            if (!bridgeConfig.hideDialog) {\n"
                + "                setTimeout(function() {\n"
                + "                    BridgeObject.onOpen();\n"
                + "                }, 200);\n"
                + "            }\n"
                + "        }\n"
                + "        function onPass(arg) {\n"
                + "            const token = arg || document.getElementById(\"input-text\").value;\n"
                + "            BridgeObject.onPass(token);\n"
                + "        }\n"
                + "        function onError(arg) {\n"
                + "            const errorCode = arg || parseInt(document.getElementById(\"input-text\").value);\n"
                + "            BridgeObject.onError(errorCode);\n"
                + "        }\n"
                + "        function setVerifyParams(arg) {\n"
                + "            TestObject.setVerifyParams(JSON.stringify(arg));\n"
                + "        }\n"
                + "        function reset() {\n"
                + "            document.getElementById(\"input-text\").value = \"reset\";\n"
                + "        }\n"
                + (callBridgeOnLoaded ? "onHcaptchaLoaded();\n" : "")
                + "    </script>\n"
                + "</body>\n"
                + "</html>\n";
    }
}
