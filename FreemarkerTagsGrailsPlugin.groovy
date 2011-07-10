/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import grails.util.Environment
import groovy.util.ConfigObject
import groovy.util.ConfigSlurper

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsClass
import org.codehaus.groovy.grails.commons.TagLibArtefactHandler
import org.codehaus.groovy.grails.plugins.freemarker.AbstractTagLibAwareConfigurer
import org.codehaus.groovy.grails.plugins.freemarker.TagLibPostProcessor
import org.springframework.context.ApplicationContext

 
class FreemarkerTagsGrailsPlugin {
    // the plugin version
    def version = "0.7.0"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "1.2.5 > *"
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
    def title = "Plugin to use Grails Tag Libraries in FreeMarker templates"
    def description = '''\\
Plugin to use Grails Tag Libraries in FreeMarker templates.
'''
    
    // monitor all resources that end with TagLib.groovy
    //def watchedResources = ['file:./plugins/*/grails-app/taglib/**/*TagLib.groovy',
    //    'file:./grails-app/taglib/**/*TagLib.groovy']

    def observe = ['groovyPages']
    
    def loadAfter = ['groovyPages']

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/freemarker-tags"
//def documentation = "http://code.google.com/p/grails-freemarker-tags-plugin"

    def doWithWebDescriptor = { xml ->
        // TODO Implement additions to web.xml (optional), this event occurs before 
    }

    def doWithSpring = {
      //mergeConfig(application)

      // Redefinition
      def viewResolverBeanDef = delegate.getBeanDefinition("freemarkerViewResolver")
      def viewResolverPropValues = viewResolverBeanDef.propertyValues
      def suffixPropValue = viewResolverPropValues.getPropertyValue("suffix") 
      viewResolverBeanDef.beanClass = org.codehaus.groovy.grails.plugins.freemarker.TagLibAwareViewResolver

      def configBeanDef = delegate.getBeanDefinition("freemarkerConfig")
      def configPropValues = configBeanDef.propertyValues
      suffixPropValue = new org.springframework.beans.PropertyValue(suffixPropValue)
      configPropValues.addPropertyValue(suffixPropValue)
      configBeanDef.beanClass = org.codehaus.groovy.grails.plugins.freemarker.TagLibAwareConfigurer
      
      // Now go through tag libraries and configure them in spring too. With AOP proxies and so on
      for (taglib in application.tagLibClasses) {
          "${taglib.fullName}_fm"(taglib.clazz) { bean ->
              bean.autowire = true
              bean.lazyInit = true
              // Taglib scoping support could be easily added here. Scope could be based on a static field in the taglib class.
              //bean.scope = 'request'
          }
      }
      
      "${TagLibPostProcessor.class.name}"(TagLibPostProcessor) {
          grailsApplication = ref('grailsApplication')
      }
    }

    def doWithDynamicMethods = { ctx ->
        // TODO
        MetaClass mc = GrailsApplication.class.metaClass
        
         
        mc.getFreeMarkerTagsConfig = {
            
            println "this ${this}"
            println "delegate ${delegate}"
            println "owner ${owner}"
            
            GrailsApplication app = delegate
            String propName = '_freeMarkerTagsConfig'
            
            ConfigObject newCfg = null
            if (mc.hasProperty(app, propName)) {
                newCfg = app[propName]
            } else {
               mc[propName] = null
            }
            
            if (newCfg == null) {
                ConfigObject cfg = app.config
                ConfigObject defaultConfig = new ConfigSlurper(Environment.current.name).parse(FreemarkerTagsDefaultConfig.class)
                println "defaultConfig ${defaultConfig}"
                println "cfg ${cfg}"
                newCfg = new ConfigObject()
                newCfg.putAll((Map) defaultConfig)
                newCfg.putAll((Map) cfg)
                app[propName] = newCfg.grails.plugins.freemarkertags
            }
            
            return config
        }
    }

    def doWithApplicationContext = { applicationContext ->
        // TODO Implement post initialization spring config (optional)
    }

    def onChange = { event ->
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
        if (application.isArtefactOfType(TagLibArtefactHandler.TYPE, event.source)) {
            GrailsClass taglibClass = application.addArtefact(TagLibArtefactHandler.TYPE, event.source)
            if (taglibClass) {
                // replace tag library bean
                def beanName = taglibClass.fullName
                def beans = beans {
                    "${beanName}_fm"(taglibClass.clazz) { bean ->
                        bean.autowire = true
                        //bean.scope = 'request'
                    }
                    
                    "${TagLibPostProcessor.class.name}"(TagLibPostProcessor) {
                        grailsApplication = ref('grailsApplication')
                    }
                }
                beans.registerBeans(event.ctx)
                
                //event.manager?.getGrailsPlugin('groovyPages')?.doWithDynamicMethods(event.ctx)

                def ApplicationContext springContext = application.mainContext
                for (configurerBeanName in springContext.getBeanNamesForType(AbstractTagLibAwareConfigurer.class)) {
                    def configurer = springContext.getBean(configurerBeanName)
                    configurer.reconfigure()
                }
            }
        }
    }

    def onConfigChange = { event ->
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }
  
}
