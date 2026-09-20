package com.hcaptcha.sdk;

import androidx.annotation.NonNull;

class HCaptchaTestHtml implements IHCaptchaHtmlProvider {

    private final boolean callBridgeOnLoaded;
    private final boolean failOnExecute;
    private final boolean failOnExecuteAfterReset;

    HCaptchaTestHtml() {
        this(true);
    }

    HCaptchaTestHtml(boolean callBridgeOnLoaded) {
        this(callBridgeOnLoaded, false);
    }

    HCaptchaTestHtml(boolean callBridgeOnLoaded, boolean failOnExecute) {
        this(callBridgeOnLoaded, failOnExecute, false);
    }

    HCaptchaTestHtml(boolean callBridgeOnLoaded, boolean failOnExecute, boolean failOnExecuteAfterReset) {
        this.callBridgeOnLoaded = callBridgeOnLoaded;
        this.failOnExecute = failOnExecute;
        this.failOnExecuteAfterReset = failOnExecuteAfterReset;
    }

    @Override
    @NonNull
    public String getHtml() {
        return "<html>\n"
                + "<head>\n"
                + "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1, user-scalable=no\"/>\n"
                + "    <style>\n"
                + "        #on-target-blank {\n"
                + "            position: fixed;\n"
                + "            bottom: 0px;\n"
                + "            right: 0px;\n"
                + "        }\n"
                + "    </style>\n"
                + "</head>\n"
                + "<body>\n"
                + "    <div id=\"hcaptcha-container\"></div>\n"
                + "    <input id=\"input-text\" />\n"
                + "    <button id=\"on-error\" onclick=\"onError()\">Error</button>\n"
                + "    <button id=\"on-pass\" onclick=\"onPass()\">Pass</button>\n"
                // The shape the live MFA "inbound SMS" challenge emits: a pooled hCaptcha number
                // and a sentence carrying a hyphenated one-time code.
                + "    <a id=\"on-sms\" href=\"sms:+46769439873?body=Return%20to%20the%20app%20and"
                + "%20press%20Confirm%20after%20sending%20this%20message.%20Do%20not%20edit%20or"
                + "%20share%20the%20code%3A%20gsuc-djcd-wd6z\">Send SMS</a>\n"
                + "    <a id=\"on-target-blank\" href=\"https://example.com\" target=\"_blank\">Open in Browser</a>\n"
                + "    <script type=\"text/javascript\">\n"
                + "        console.assert(window.JSDI.getDebugInfo() instanceof Array);\n"
                + "        console.assert(typeof window.JSDI.getSysDebug() === 'object');\n"
                + "        var BridgeObject = window.JSInterface;\n"
                + "        var bridgeConfig = JSON.parse(BridgeObject.getConfig());\n"
                + "        var resetCount = 0;\n"
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
                + "        function setData(arg) {\n"
                + "            TestObject.setData(JSON.stringify(arg));\n"
                + "        }\n"
                + "        function reset() {\n"
                + "            resetCount += 1;\n"
                + "            document.getElementById(\"input-text\").value = \"reset\";\n"
                + "        }\n"
                + "        function execute() {\n"
                + (failOnExecute ? "            BridgeObject.onError(29);\n" : "")
                + (failOnExecuteAfterReset
                        ? "            if (resetCount > 1) { BridgeObject.onError(29); }\n" : "")
                + "        }\n"
                + (callBridgeOnLoaded ? "onHcaptchaLoaded();\n" : "")
                + "    </script>\n"
                + "</body>\n"
                + "</html>\n";
    }
}
