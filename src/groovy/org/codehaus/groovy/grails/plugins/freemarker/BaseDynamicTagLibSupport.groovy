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

import grails.util.GrailsUtil;
import groovy.lang.GroovyObject

import freemarker.core.Environment
import freemarker.ext.beans.SimpleMapModel
import freemarker.template.TemplateHashModelEx
import freemarker.template.utility.DeepUnwrap

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import org.codehaus.groovy.grails.commons.ApplicationHolder
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsTagLibClass
import org.codehaus.groovy.grails.commons.TagLibArtefactHandler
import org.codehaus.groovy.grails.web.pages.GroovyPageOutputStack
import org.codehaus.groovy.grails.web.taglib.GroovyPageAttributes


/**
 * @author Daniel Henrique Alves Lima
 */
@Deprecated
public class BaseDynamicTagLibSupport {

  protected final Log log = LogFactory.getLog(getClass())

  private static final String TAG_LIBS_VARIABLE = BaseDynamicTagLibSupport.class.getName().replace(".", "-") + "-TAG_LIBS"
  private static final String TAG_LIBS_CLASSES_VARIABLE = BaseDynamicTagLibSupport.class.getName().replace(".", "-") + "-TAG_LIBS_CLASSES"
  
  private static final Map RESERVED_WORDS_TRANSLATION = ['as':"_as"]
  
  protected final String tagLibName
  protected final String tagName


  protected BaseDynamicTagLibSupport(String tagLibName, String tagName) {
    GrailsUtil.deprecated "${getClass()} is deprecated; Use ${TagLibToDirectiveAndFunction.class} instead"
    if (log.isDebugEnabled()) {
      log.debug("constructor(): " + tagLibName + "." + tagName)
    }
    this.tagLibName = tagLibName
    this.tagName = tagName
  }

  protected String getOutputEncoding(Environment env) {
    String encoding = env.getOutputEncoding()
    if (!encoding) {
      def config = env.getConfiguration()
      //encoding = config.getEncoding(null)
      //if (!encoding) {
      encoding = config.getDefaultEncoding()
      //}
    }
    
    return encoding
  }

  protected Map unwrapParams(TemplateHashModelEx params, boolean translateReservedWords = true) {
    Map unwrappedParams = new GroovyPageAttributes(new LinkedHashMap())

    def keys = params.keys().iterator()
    while (keys.hasNext()) {
      def oldKey = keys.next().toString()
      def value = params.get(oldKey)
      if (value) {
	value = DeepUnwrap.permissiveUnwrap(value)
      }

      def key = null
      if (translateReservedWords) {
	key = BaseDynamicTagLibSupport.RESERVED_WORDS_TRANSLATION.get(oldKey)
      }
      if (!key) {
	key = oldKey
      }

      unwrappedParams.put(key, value)
    }
    
    return unwrappedParams
  }


  protected Map unwrapParams(Map params, boolean translateReservedWords = true) {
    def unwrappedParams = new GroovyPageAttributes(new LinkedHashMap())

    params.each {
      def value = it.value
      if (value) {
	value = DeepUnwrap.permissiveUnwrap(value)
      }
      
      def key = null
      if (translateReservedWords) {
	key = BaseDynamicTagLibSupport.RESERVED_WORDS_TRANSLATION.get(it.key)
      }
      if (!key) {
	key = it.key
      }
      
      unwrappedParams.put(key, value)
    }

    return unwrappedParams
  }


  protected void logCurrentOutput() {
    if (log.isDebugEnabled()) {
      log.debug("logCurrentOutput(): currentOriginalWriter " + GroovyPageOutputStack.currentStack().getCurrentOriginalWriter())
    }
  }

  protected void restoreOutput() {
    log.debug("restoreOutput()")
    logCurrentOutput()
    GroovyPageOutputStack.currentStack().pop()
    logCurrentOutput()
  }

  protected boolean doesReturnObject(Environment env = null) {
    GrailsTagLibClass tagLibClass = getDynamicTagLibClass(env)
    def tagNamesThatReturnObject = tagLibClass.getTagNamesThatReturnObject()
    def result = tagNamesThatReturnObject.contains(this.tagName)
    if (log.isDebugEnabled()) {
      //log.debug("doesReturnObject(): tagNamesThatReturnObject = " + tagNamesThatReturnObject)
      log.debug("doesReturnObject(): " + result)
    }
    return result
  }

  protected GroovyObject getDynamicTagLib(Environment env) {
    def tagLibs = getTagLibsCache(env)
    GrailsApplication application =  ApplicationHolder.getApplication()
    def appContext = application.getMainContext()
    
    GrailsTagLibClass tagLibClass = getDynamicTagLibClass(env, application)
    GroovyObject tagLib = null
    if (log.isDebugEnabled()) {
      log.debug("getDynamicTagLib(): tagLibClass " + tagLibClass)
    }
    if (tagLibClass) {
      def tagLibFullName = tagLibClass.getFullName()
      if (log.isDebugEnabled()) {
	log.debug("getDynamicTagLib(): tagLibFullName " + tagLibFullName)
      }
      if (tagLibs.containsKey(tagLibFullName)) {
	log.debug("getDynamicTagLib(): hit")
	tagLib = (GroovyObject) tagLibs.get(tagLibFullName)
      }
      else {
	log.debug("getDynamicTagLib(): miss")
	tagLib = (GroovyObject) appContext.getBean(tagLibFullName)
	tagLibs.put(tagLibFullName, tagLib)
      }
    }
    
    if (log.isDebugEnabled()) {
      log.debug("getDynamicTagLib(): tagLib " + tagLib)
    }
    
    return tagLib
  }


  private GrailsTagLibClass getDynamicTagLibClass(Environment env, GrailsApplication application = null) {
    if (!application) {
      application =  ApplicationHolder.getApplication()
    }
    String tagFullName = this.tagLibName + ":" + this.tagName
    
    Map<String, GrailsTagLibClass> classesCache = getTagLibsClassesCache(env)
    GrailsTagLibClass tagLibClass = classesCache? classesCache[tagFullName] : null

    if (tagLibClass) {
      log.debug("getDynamicTagLibClass(): cache hit")
    } else {
      log.debug("getDynamicTagLibClass(): cache miss")
      tagLibClass = (GrailsTagLibClass) application.getArtefactForFeature(
	TagLibArtefactHandler.TYPE, tagFullName)
      classesCache.put(tagFullName, tagLibClass)
    }
    return tagLibClass
  }  

  private Map<String, GroovyObject> getTagLibsCache(Environment env) {
    def cache = env.getVariable(TAG_LIBS_VARIABLE)
    if (!cache) {
      cache = new SimpleMapModel(Collections.synchronizedMap(new HashMap<String, GroovyObject>()), null)
      env.setVariable(TAG_LIBS_VARIABLE, cache)
    }
    cache = cache.getWrappedObject()

    return cache
  }

  private Map<String, GrailsTagLibClass> getTagLibsClassesCache(Environment env) {
    if (env) {
      def cache = env.getVariable(TAG_LIBS_CLASSES_VARIABLE)
      if (!cache) {
	cache = new SimpleMapModel(Collections.synchronizedMap(new HashMap<String, GrailsTagLibClass>()), null)
	env.setVariable(TAG_LIBS_CLASSES_VARIABLE, cache)
      }
      cache = cache.getWrappedObject()
      
      return cache
    } else {
      return null
    }
  }

}