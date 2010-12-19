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
package org.codehaus.groovy.grails.plugins.freemarker.directive

import groovy.lang.Closure
import groovy.lang.GroovyObject

import java.io.IOException
import java.io.Writer
import java.util.Map

import freemarker.core.Environment
import freemarker.ext.beans.SimpleMapModel
import freemarker.template.TemplateDirectiveBody
import freemarker.template.TemplateDirectiveModel
import freemarker.template.TemplateException
import freemarker.template.TemplateModel
import freemarker.template.TemplateModelException

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import org.codehaus.groovy.grails.commons.ApplicationHolder
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsTagLibClass
import org.codehaus.groovy.grails.commons.TagLibArtefactHandler
import org.codehaus.groovy.grails.web.metaclass.TagLibDynamicMethods
import org.codehaus.groovy.grails.web.pages.GroovyPage


/**
 * @author Daniel Henrique Alves Lima
 */
class DynamicTagLibDirective implements TemplateDirectiveModel {

  private final Log log = LogFactory.getLog(getClass())
  private static final String TAG_LIBS_ATTRIBUTE = DynamicTagLibDirective.class.getName().replace(".", "-") + "-TAG_LIBS"

  def private static RESERVED_WORDS_TRANSLATION = [as:"_as"]

  private String tagLibName
  private String tagName

  public DynamicTagLibDirective(String tagLibName, String tagName) {
    if (log.isDebugEnabled()) {
      log.debug("constructor(): " + tagLibName + "." + tagName)
    }
    this.tagLibName = tagLibName
    this.tagName = tagName
  }

  public void execute(Environment env,
		      Map params, TemplateModel[] loopVars,
		      TemplateDirectiveBody body)
  throws TemplateException, IOException {
    if (log.isDebugEnabled()) {
      log.debug("execute(): " + tagLibName + "." + tagName)      
      log.debug("execute(): env=" + env + "; params=" + params + "; body=" + body)
    }
    def tagLib = getDynamicTagLib(env)
    if (!tagLib) {
      throw new TemplateModelException(
	"Could not find tagLib " + this.tagLibName);
    }

    def closure = tagLib[this.tagName]
    if (!closure) {
      throw new TemplateModelException(
	"Could not find tag " + this.tagName);
    }

    tagLib.setProperty(TagLibDynamicMethods.OUT_PROPERTY, env.getOut());

    def objWrapper = env.getObjectWrapper()
    def hasUnwrap = objWrapper.metaClass.respondsTo(objWrapper, "unwrap")
    def unwrappedParams = new LinkedHashMap()

    if (log.isDebugEnabled()) {
      log.debug("execute(): hasUnwrap " + hasUnwrap)
    }
    params.each {
      def value = it.value
      if (hasUnwrap && value) {
	value = objWrapper.unwrap(value)
      }

      def key = RESERVED_WORDS_TRANSLATION.get(it.key)
      if (!key) {
	key = it.key
      }

      unwrappedParams.put(key, value)
    }

    try {
      if (closure.getMaximumNumberOfParameters() == 1) {
	closure(unwrappedParams)
      } else {
	closure(unwrappedParams) {
	  log.debug("executeBody(): " + tagLibName + "." + tagName)
	  if (body) {
	    body.render(env.getOut())
	  } else {
	    throw new TemplateException("missing body", env)
	  }
	}
      }
    } catch (RuntimeException e) {
      throw new TemplateException(e, env)
    }

    if (log.isDebugEnabled()) {
      log.debug("execute(): tag executed")
    }

    /*def exception = tagLib.getProperty(TagLibDynamicMethods.THROW_TAG_ERROR_METHOD)
    if (exception) {
      throw new RuntimeException(exception)
    }*/
  }


  private GroovyObject getDynamicTagLib(Environment env) {
    def tagLibs = getTagLibsCache(env)
    GrailsApplication application =  ApplicationHolder.getApplication()
    def appContext = application.getMainContext()
    
    GrailsTagLibClass tagLibClass = (GrailsTagLibClass) application.getArtefactForFeature(
      TagLibArtefactHandler.TYPE, /*GroovyPage.DEFAULT_NAMESPACE + ':' + */this.tagLibName)
    GroovyObject tagLib = null
    if (log.isDebugEnabled()) {
      log.debug("getDynamicTagLib(): tagLibClass " + tagLibClass)
    }
    if (tagLibClass) {
      def tagLibFullName = tagLibClass.getFullName()
      if (tagLibs.containsKey(tagLibFullName)) {
	tagLib = (GroovyObject) tagLibs.get(tagLibFullName)
      }
      else {
	tagLib = (GroovyObject) appContext.getBean(tagLibFullName)
	tagLibs.put(tagLibFullName, tagLib)
      }
    }

    if (log.isDebugEnabled()) {
      log.debug("getDynamicTagLib(): tagLib " + tagLib)
    }
    
    return tagLib
  }
  

  protected Map<String, GroovyObject> getTagLibsCache(Environment env) {
    def cache = env.getVariable(TAG_LIBS_ATTRIBUTE)
    if (!cache) {
      cache = new SimpleMapModel(new HashMap<String, GroovyObject>(), null)
      env.setVariable(TAG_LIBS_ATTRIBUTE, cache)
    }
    cache = cache.getWrappedObject()

    return cache
  }

}