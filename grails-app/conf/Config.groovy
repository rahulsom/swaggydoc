// configuration for plugin testing - will not be included in the plugin zip

log4j = {
    // Example of changing the log pattern for the default console
    // appender:
    //
    //appenders {
    //    console name:'stdout', layout:pattern(conversionPattern: '%c{2} %m%n')
    //}

    error 'org.codehaus.groovy.grails.web.servlet',  //  controllers
            'org.codehaus.groovy.grails.web.pages', //  GSP
            'org.codehaus.groovy.grails.web.sitemesh', //  layouts
            'org.codehaus.groovy.grails.web.mapping.filter', // URL mapping
            'org.codehaus.groovy.grails.web.mapping', // URL mapping
            'org.codehaus.groovy.grails.commons', // core / classloading
            'org.codehaus.groovy.grails.plugins', // plugins
            'org.codehaus.groovy.grails.orm.hibernate', // hibernate integration
            'org.springframework',
            'org.hibernate',
            'net.sf.ehcache.hibernate'
}

swaggydoc {
    contact = "rahul.som@gmail.com"
    description = """\
        | This is a sample server Petstore server.  You can find out more about Swagger
        | at <a href="http://swagger.wordnik.com">http://swagger.wordnik.com</a> or on irc.freenode.net, #swagger.
        | For this sample,
        | you can use the api key "special-key" to test the authorization filters""".stripMargin()
    license = "Apache 2.0"
    licenseUrl = "http://www.apache.org/licenses/LICENSE-2.0.html"
    termsOfServiceUrl = "http://helloreverb.com/terms/"
    title = "Swaggydoc Demo App"
    apiVersion = "1.0"
}