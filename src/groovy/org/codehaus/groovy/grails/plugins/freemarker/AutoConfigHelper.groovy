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
import freemarker.ext.beans.SimpleMapModel

import freemarker.cache.MultiTemplateLoader
import freemarker.cache.TemplateLoader
import freemarker.cache.StringTemplateLoader

import grails.util.Environment

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import org.codehaus.groovy.grails.commons.ApplicationHolder
import org.codehaus.groovy.grails.commons.GrailsApplication

/**
 * @author Daniel Henrique Alves Lima
 */
public class AutoConfigHelper {

  //public static final String GRAILS_CONFIG_NAMESPACE = "grails.plugins.freemarkertags"

  //public static final String CONFIGURED_ATTRIBUTE_NAME = "_" + AutoConfigHelper.class.getName() + ".configured"

  private final Log log = LogFactory.getLog(getClass())
  
  private final String ftlExtension
  private StringTemplateLoader stringLoader  = null
  private Map sharedVariables = null
  private ConfigObject grailsConfig

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
      String dynamicDirectiveClassName = null
      String dynamicFunctionClassName = null
      def oldLoader = configuration.templateLoader
      def autoImport = [:]
      if (!stringLoader || reload) {
	synchronized (this) {
	  def lf = System.getProperty("line.separator")
	  
	  GrailsApplication application = ApplicationHolder.getApplication()
	  def grailsConfig = mergeConfig(application)
	  this.grailsConfig = grailsConfig
	  if (log.isDebugEnabled()) {
	    log.debug("autoConfigure(): grailsConfig " + grailsConfig)
	  }	  
	  if (grailsConfig.autoImport && grailsConfig.asSharedVariables) {
	    throw new RuntimeException("autoImport should be false when asSharedVariables is true");
	  }

	  if (grailsConfig.defineLegacyFunctions) {
	    dynamicDirectiveClassName = DynamicTagLibDirective.class.getName()  
	    dynamicFunctionClassName = DynamicTagLibFunction.class.getName()
	  } else {
	    dynamicDirectiveClassName = DynamicTagLibDirectiveAndFunction.class.getName()
	  }
	  
	  def dynamicDirectiveConstructor = null
	  def dynamicFunctionConstructor = null
	  
	  if (grailsConfig.asSharedVariables) {
	    def cl = Thread.currentThread().contextClassLoader
	    dynamicDirectiveConstructor = cl.loadClass(dynamicDirectiveClassName).getConstructor([String, String] as Class[])
	    if (grailsConfig.defineLegacyFunctions) {
	      dynamicFunctionConstructor = cl.loadClass(dynamicFunctionClassName).getConstructor([String, String] as Class[])
	    }
	  }
	  
	  def templates = [:]
	  if (grailsConfig.asSharedVariables) {
	    sharedVariables = [:]
	  }
	  application.tagLibClasses.each {
	    tagLibClass ->
	      def template = templates.get(tagLibClass.namespace)
	      if (!template) {
		template = new StringBuilder("[#ftl/]")
		template.append(lf)
		templates.put(tagLibClass.namespace, template)
	      }
	      
	      def sharedVar = null
	      if (sharedVariables != null) {
		sharedVar = sharedVariables[tagLibClass.namespace]
		if (sharedVar == null) {
		  sharedVar = [:]
		  sharedVariables.put(tagLibClass.namespace, sharedVar)
		}
	      }
	      
	      tagLibClass.tagNames.each {
		tagName ->
		  template.append('[#assign ' + tagName + ' =' +
				  '"' + dynamicDirectiveClassName + '"?new("' + tagLibClass.namespace + '", "' + tagName + '")]')
		  template.append(lf)
		  
		  if (sharedVar != null) {
		    sharedVar[tagName] = dynamicDirectiveConstructor.newInstance(tagLibClass.namespace, tagName)
		  }
		  
		  if (grailsConfig.defineLegacyFunctions) {
		    template.append('[#assign _' + tagName + ' =' +
				    '"' + dynamicFunctionClassName + '"?new("' + tagLibClass.namespace + '", "' + tagName + '")]')
		    template.append(lf)
		    
		    if (sharedVar != null) {
		      sharedVar["_" + tagName] = dynamicFunctionConstructor.newInstance(tagLibClass.namespace, tagName)
		    }
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
	  
	  if (log.isDebugEnabled()) {
	    log.debug("autoConfigure(): sharedVariables " + (sharedVariables != null? sharedVariables.keySet(): null))
	  }
	  if (sharedVariables != null) {
	    sharedVariables.entrySet().each {
	      entry ->
		log.debug("autoConfigure(): entry.key " + entry.key)
		entry.value = new SimpleMapModel(entry.value, null)
	    }
	  }
	}
      }
      
      Boolean isConfigured = configuration.getCustomAttribute(CONFIGURED_ATTRIBUTE_NAME)
      if (log.isDebugEnabled()) {
	log.debug("autoConfigure(): isConfigured " + isConfigured)
      }

      if (!isConfigured || reload) {
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
	
	if (log.isDebugEnabled()) {
	  log.debug("autoConfigure(): sharedVariables " + sharedVariables)
	}
	if (sharedVariables != null) {
	  def replacedSharedVars = new HashSet();
	  sharedVariables.entrySet().each {
	    entry ->
	      def sharedVar = configuration.getSharedVariable(entry.key)
	      if (sharedVar != entry.value) {	      
		replacedSharedVars << entry.key
		configuration.setSharedVariable(entry.key, entry.value)
	      }
	  }
	  
	  if (log.isDebugEnabled()) {
	    log.debug("autoConfigure(): replacedSharedVars " + replacedSharedVars)
	  }
	}

	configuration.setCustomAttribute(CONFIGURED_ATTRIBUTE_NAME, Boolean.TRUE)
      }
    }

    return configuration
  }


  protected ConfigObject mergeConfig(application) {
    def customConfig = application.config.grails.plugins.freemarkertags
    if (!customConfig) {
      customConfig = application.config['grails.plugins.freemarkertags']
    }
    
    if (customConfig) {
      if (customConfig instanceof Map && !(customConfig instanceof ConfigObject)) {
	ConfigObject c = new ConfigObject()
	c.putAll(customConfig)
	customConfig = c
      }
    } 

    ConfigObject defaultConfig = loadDefaultConfig(application)
    
    def mergedConfig = defaultConfig.merge(customConfig)
    customConfig.putAll(mergedConfig)
    
    return customConfig
  }
  
  
  protected ConfigObject loadDefaultConfig(application) {
    def config = new ConfigSlurper(Environment.current.name).parse(application.classLoader.loadClass("FreemarkerTagsDefaultConfig"))
    return config.freemarkertags
  } 

  public ConfigObject getGrailsConfig() {
    return grailsConfig
  }
}