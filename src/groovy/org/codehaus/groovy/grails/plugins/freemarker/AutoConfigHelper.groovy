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
package org.codehaus.groovy.grails.plugins.freemarker

import freemarker.template.Configuration

import freemarker.cache.MultiTemplateLoader
import freemarker.cache.TemplateLoader
import freemarker.cache.StringTemplateLoader

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import org.codehaus.groovy.grails.commons.ApplicationHolder
import org.codehaus.groovy.grails.commons.GrailsApplication

/**
 * @author Daniel Henrique Alves Lima
 */
public class AutoConfigHelper {

  //public static final String GRAILS_CONFIG_NAMESPACE = "grails.plugins.freemarkertags"

  private final Log log = LogFactory.getLog(getClass())
  
  private final String ftlExtension
  private StringTemplateLoader stringLoader  = null

  public AutoConfigHelper() {
    this(".ftl")
  }

  public AutoConfigHelper(String ftlExtension) {
    this.ftlExtension = ftlExtension
    if (log.isDebugEnabled()) {
      log.debug("constructor(): ftlExtension '" + ftlExtension + "'")
    }
  }

  public Configuration autoConfigure(boolean reload = false, Configuration configuration) {
    if (log.isDebugEnabled()) {
      log.debug("autoConfigure(): reload " + reload + ", configuration " + configuration)
    }
    if (configuration) {
      String dynamicDirectiveClassName = DynamicTagLibDirective.class.getName()
      String dynamicFunctionClassName = DynamicTagLibFunction.class.getName()
      def oldLoader = configuration.templateLoader
      def autoImport = [:]
      if (!stringLoader || reload) {
	def lf = System.getProperty("line.separator")
	
	GrailsApplication application = ApplicationHolder.getApplication()
	def grailsConfig = [
	  autoImport: true,
	  defineFunctions: true
	]
	def grailsReconfig = application.config.grails.plugins.freemarkertags
	if (grailsReconfig instanceof ConfigObject) {
	  grailsReconfig = grailsReconfig.toProperties()
	}
	if (log.isDebugEnabled()) {
	  log.debug("autoConfigure(): grailsConfig " + grailsConfig)
	  log.debug("autoConfigure(): grailsReconfig " + grailsReconfig)
	}
	grailsConfig.putAll(grailsReconfig)
	if (log.isDebugEnabled()) {
	  log.debug("autoConfigure(): grailsConfig " + grailsConfig)
	}	

	def templates = [:]
	application.tagLibClasses.each {
	  tagLibClass ->
	    def template = templates.get(tagLibClass.namespace)
	    if (!template) {
	      template = new StringBuilder("[#ftl/]")
	      template.append(lf)
	      templates.put(tagLibClass.namespace, template)
	    }
	    
	    tagLibClass.tagNames.each {
	      tagName ->
	      template.append('[#assign ' + tagName + ' =' +
			      '"' + dynamicDirectiveClassName + '"?new("' + tagLibClass.namespace + '", "' + tagName + '")]')
	      template.append(lf)
	      if (grailsConfig.defineFunctions) {
		template.append('[#assign _' + tagName + ' =' +
				'"' + dynamicFunctionClassName + '"?new("' + tagLibClass.namespace + '", "' + tagName + '")]')
		template.append(lf)
	      }
	    }
	}
	
	stringLoader = new StringTemplateLoader()
	templates.each {
	  def key = /*"/" +*/ it.key + this.ftlExtension
	  def value = it.value.toString()
	  if (log.isDebugEnabled()) {
	    log.debug("autoConfigure(): template " + key)
	    log.debug("autoConfigure():          " + value)
	  }

	  stringLoader.putTemplate(key, value)
	  if (grailsConfig.autoImport) {
	    autoImport.put(it.key, key)
	  }
	}

	templates = null
      }
      
      def loaders = [stringLoader, oldLoader]
      if (log.isDebugEnabled()) {
	log.debug("autoConfigure(): loaders " + loaders)
      }

      def loader = new MultiTemplateLoader(loaders as TemplateLoader[])
      configuration.setTemplateLoader(loader)
      if (log.isDebugEnabled()) {
	log.debug("autoConfigure(): loader " + loader)
      }

      autoImport.each {
	if (log.isDebugEnabled()) {
	  log.debug("autoConfigure(): autoImporting " + it.key + " " + it.value)
	}
	configuration.addAutoImport(it.key, it.value)
      }
    }

    return configuration
  }


}