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

import org.springframework.grails.freemarker.GrailsFreeMarkerView
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
public class DynamicTagLibView extends GrailsFreeMarkerView {

  private final Log log = LogFactory.getLog(getClass())

  private StringTemplateLoader stringLoader  = null

  public DynamicTagLibView() {
    log.debug("constructor()")
  }

  @Override
  public void setConfiguration(Configuration configuration) {
    if (log.isDebugEnabled()) {
      log.debug("setConfiguration(): configuration " + configuration)
    }
    if (configuration) {
      def oldLoader = configuration.templateLoader
      if (!stringLoader) {
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
	    log.debug("setConfiguration(): template " + key)
	    log.debug("setConfiguration():          " + value)
	  }

	  stringLoader.putTemplate(key, value)
	}
      }

      
      def loaders = [stringLoader, oldLoader]
      if (log.isDebugEnabled()) {
	log.debug("setConfiguration(): loaders " + loaders)
      }

      def loader = new MultiTemplateLoader(loaders as TemplateLoader[])
      configuration.setTemplateLoader(loader)
      if (log.isDebugEnabled()) {
	log.debug("setConfiguration(): loader " + loader)
      }
    }
    super.setConfiguration(configuration)
  }

}