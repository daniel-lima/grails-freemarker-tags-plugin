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

  private final Log log = LogFactory.getLog(getClass())
  
  private StringTemplateLoader stringLoader  = null

  public Configuration autoConfigure(boolean reload = false, Configuration configuration) {
    if (log.isDebugEnabled()) {
      log.debug("autoConfigure(): reload " + reload + ", configuration " + configuration)
    }
    if (configuration) {
      def oldLoader = configuration.templateLoader
      def autoImport = [:]
      if (!stringLoader || reload) {
	def lf = System.getProperty("line.separator")
	GrailsApplication application =  ApplicationHolder.getApplication()
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
			      '"org.codehaus.groovy.grails.plugins.freemarker.directive.DynamicTagLibDirective"?new("' + tagLibClass.namespace + '", "' + tagName + '")]')
	      template.append(lf)
	    }
	}
	
	stringLoader = new StringTemplateLoader()
	templates.each {
	  def key = /*"/" +*/ it.key + ".ftl"
	  def value = it.value.toString()
	  if (log.isDebugEnabled()) {
	    log.debug("autoConfigure(): template " + key)
	    log.debug("autoConfigure():          " + value)
	  }

	  stringLoader.putTemplate(key, value)
	  autoImport.put(it.key, key)
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