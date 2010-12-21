class FreemarkerTagsGrailsPlugin {
    // the plugin version
    def version = "0.5.1"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "1.3.3 > *"
    // the other plugins this plugin depends on
    def dependsOn = [freemarker: "0.3 > *"]
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
      "grails-app/views/*",
      "grails-app/i18n/*"
    ]

    // TODO Fill in these fields
    def author = "Daniel Henrique Alves Lima"
    def authorEmail = "email_daniel_h@yahoo.com.br"
    def title = "Plugin to use Grails Dynamic Tag Libraries as Freemarker directives"
    def description = '''\\
Plugin to use Grails Dynamic Tag Libraries as Freemarker directives.
'''

    // URL to the plugin's documentation
    //def documentation = "http://grails.org/plugin/freemarker-tags"
    def documentation = "http://code.google.com/p/grails-freemarker-tags-plugin"

    def doWithWebDescriptor = { xml ->
        // TODO Implement additions to web.xml (optional), this event occurs before 
    }

    def doWithSpring = {
      // Redefinition
      freemarkerViewResolver(org.codehaus.groovy.grails.plugins.freemarker.DynamicTagLibViewResolver) {
	prefix = ''
	suffix = '.ftl'
	order = 10
      }
    }

    def doWithDynamicMethods = { ctx ->
        // TODO Implement registering dynamic methods to classes (optional)
    }

    def doWithApplicationContext = { applicationContext ->
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
}
