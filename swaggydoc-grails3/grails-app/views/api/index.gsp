<%@ page contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <title>Swagger UI</title>
    <link href='//fonts.googleapis.com/css?family=Droid+Sans:400,700' rel='stylesheet' type='text/css'/>

    <asset:stylesheet href="reset.css"/>
    <asset:stylesheet href="screen.css"/>

    <asset:javascript src="swagger-lib/shred.bundle.js" /> 
    <asset:javascript src="swagger-lib/jquery-1.8.0.min.js" /> 
    <asset:javascript src="swagger-lib/jquery.slideto.min.js" /> 
    <asset:javascript src="swagger-lib/jquery.wiggle.min.js" /> 
    <asset:javascript src="swagger-lib/jquery.ba-bbq.min.js" /> 
    <asset:javascript src="swagger-lib/handlebars-1.0.0.js" /> 
    <asset:javascript src="swagger-lib/underscore-min.js" /> 
    <asset:javascript src="swagger-lib/backbone-min.js" /> 
    <asset:javascript src="swagger-lib/swagger.js" /> 
    <asset:javascript src="swagger-ui.js" />
    <asset:javascript src="swagger-lib/highlight.7.3.pack.js" /> 

    <!-- enabling this will enable oauth2 implicit scope support -->
    <asset:javascript src="swagger-lib/swagger-oauth.js" /> 

    <script type="text/javascript">
        $(function () {
            window.swaggerUi = new SwaggerUi({
                url: "${g.createLink(controller: 'api', action: 'resources')}",
                dom_id: "swagger-ui-container",
                supportedSubmitMethods: ['get', 'post', 'put', 'delete'],
                onComplete: function (swaggerApi, swaggerUi) {
                    log("Loaded SwaggerUI");

                    if (typeof initOAuth == "function") {
                        /*
                         initOAuth({
                         clientId: "your-client-id",
                         realm: "your-realms",
                         appName: "your-app-name"
                         });
                         */
                    }
                    $('pre code').each(function (i, e) {
                        hljs.highlightBlock(e)
                    });
                },
                onFailure: function (data) {
                    log("Unable to Load SwaggerUI");
                },
                docExpansion: "none",
                sorter: "alpha"
            });

            $('#input_apiKey').change(function () {
                var key = $('#input_apiKey')[0].value;
                log("key: " + key);
                if (key && key.trim() != "") {
                    log("added key " + key);
                    window.authorizations.add("key", new ApiKeyAuthorization("api_key", key, "query"));
                }
            });
            window.swaggerUi.load();
        });
    </script>
</head>

<body class="swagger-section">
<div id='header'>
    <div class="swagger-ui-wrap">
        <a id="logo" href="${g.createLink(controller: 'api')}">swagger</a>

        <form id='api_selector'>
            <div class='input'>
                <input placeholder="http://example.com/api" id="input_baseUrl" name="baseUrl"
                       type="text" readonly="true"/>
            </div>

            <div class='input'>
                <input placeholder="api_key" id="input_apiKey" name="apiKey" type="text"/>
            </div>

            <div class='input'><a id="explore" href="#">Explore</a></div>
        </form>
    </div>
</div>

<div id="message-bar" class="swagger-ui-wrap">&nbsp;</div>

<div id="swagger-ui-container" class="swagger-ui-wrap"></div>
</body>
</html>
