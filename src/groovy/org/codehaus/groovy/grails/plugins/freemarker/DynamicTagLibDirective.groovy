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

import groovy.lang.Closure
import groovy.lang.GroovyObject

import java.io.IOException
import java.io.Writer
import java.util.Map

import freemarker.core.Environment
import freemarker.template.TemplateDirectiveBody
import freemarker.template.TemplateDirectiveModel
import freemarker.template.TemplateException
import freemarker.template.TemplateModel
import freemarker.template.TemplateModelException

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import org.codehaus.groovy.grails.commons.ApplicationHolder
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.web.metaclass.TagLibDynamicMethods


/**
 * @author Daniel Henrique Alves Lima
 */
public class DynamicTagLibDirective extends BaseDynamicTagLibSupport implements TemplateDirectiveModel {

  public DynamicTagLibDirective(String tagLibName, String tagName) {
    super(tagLibName, tagName)
  }

  public void execute(Environment env,
		      Map params, TemplateModel[] loopVars,
		      TemplateDirectiveBody body)
  throws TemplateException, IOException {
    if (log.isDebugEnabled()) {
      log.debug("execute(): " + tagLibName + "." + tagName)      
      log.debug("execute(): env=" + env + "; params=" + params + "; body=" + body)
    }
    try {
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
      
      def unwrappedParams = unwrapParams(params)
      
      if (log.isDebugEnabled()) {
	log.debug("execute(): unwrappedParams " + unwrappedParams)
      }

      def result = null
      if (closure.getMaximumNumberOfParameters() == 1) {
	result = closure(unwrappedParams)
      } else {
	result = closure(unwrappedParams) {
	  log.debug("executeBody(): " + tagLibName + "." + tagName)
	  if (body) {
	    body.render(env.getOut())
	  } else {
	    throw new TemplateException("missing body", env)
	  }
	}
      }

      log.debug("execute(): tag executed")
      if (result && !(result instanceof String)) {
	result = null
      }

      if (log.isDebugEnabled()) {
	log.debug("execute(): result " + result)
      }

      if (result) {
	env.getOut() << result
      }
    } catch (Exception e) {
      if (!(e instanceof TemplateException)) {
	throw new TemplateModelException(e)
      }
    }
    
  }

}