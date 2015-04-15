class SwaggydocGrailsPlugin {
    // the plugin version
    def version = "0.14-SNAPSHOT"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "2.2 > *"
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/views/error.gsp",
            "grails-app/controllers/com/github/rahulsom/swaggydoc/test/*.groovy",
            "grails-app/domain/com/github/rahulsom/swaggydoc/test/*.groovy",
            "src/groovy/com/github/rahulsom/swaggydoc/test/*.groovy",
    ]

    def title = "Swaggydoc Plugin" // Headline display name of the plugin
    def author = "Rahul Somasunderam"
    def authorEmail = "rahul.som@gmail.com"
    def description = '''\
Uses swagger to document Grails Controllers
'''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/swaggydoc"

    // Extra (optional) plugin metadata

    // License: one of 'APACHE', 'GPL2', 'GPL3'
    def license = "APACHE"

    // Details of company behind the plugin (if there is one)
//    def organization = [ name: "My Company", url: "http://www.my-company.com/" ]

    // Any additional developers beyond the author specified above.
//    def developers = [ [ name: "Joe Bloggs", email: "joe@bloggs.net" ]]

    // Location of the plugin's issue tracker.
    def issueManagement = [ system: "Github", url: "https://github.com/rahulsom/swaggydoc" ]

    // Online location of the plugin's browseable source code.
    def scm = [ url: "http://github.com/rahulsom/swaggydoc.git" ]

    def doWithWebDescriptor = { xml ->
        // TODO Implement additions to web.xml (optional), this event occurs before
    }

    def doWithSpring = {
        // TODO Implement runtime spring config (optional)
    }

    def doWithDynamicMethods = { ctx ->
        // TODO Implement registering dynamic methods to classes (optional)
    }

    def doWithApplicationContext = { ctx ->
        // TODO Implement post initialization spring config (optional)
    }

    def onChange = { event ->
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    def onConfigChange = { event ->
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }

    def onShutdown = { event ->
        // TODO Implement code that is executed when the application shuts down (optional)
    }
}
