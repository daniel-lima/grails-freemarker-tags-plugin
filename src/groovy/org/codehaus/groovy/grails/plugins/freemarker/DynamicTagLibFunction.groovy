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

import freemarker.core.Environment
import freemarker.ext.beans.StringModel
import freemarker.template.TemplateException
import freemarker.template.TemplateMethodModelEx
import freemarker.template.TemplateModelException

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import org.codehaus.groovy.grails.web.metaclass.TagLibDynamicMethods

/**
 * @author Daniel Henrique Alves Lima
 */
public class DynamicTagLibFunction extends BaseDynamicTagLibSupport implements TemplateMethodModelEx {

  public DynamicTagLibFunction(String tagLibName, String tagName) {
    super(tagLibName, tagName)
  }

  public Object exec(List arguments) throws TemplateModelException {
    def env = Environment.getCurrentEnvironment()
    boolean restoreOut = false
    try {
      
      if (log.isDebugEnabled()) {
	log.debug("exec(): " + tagLibName + "." + tagName)      
	log.debug("exec(): arguments=" + arguments + "; env=" + env)
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
      
      String encoding = getOutputEncoding(env)
      
      ByteArrayOutputStream output = new ByteArrayOutputStream()
      def writer = encoding ? new OutputStreamWriter(output, encoding) : new OutputStreamWriter(output)
      logCurrentOutput()
      if (log.isDebugEnabled()) {
	log.debug("exec(): writer " + writer)
      }
      tagLib.setProperty(TagLibDynamicMethods.OUT_PROPERTY, writer);
      logCurrentOutput()
      //tagLib.out = writer
      restoreOut = true
      
      def params = arguments && arguments.size() > 0? arguments[0] : [:]
      def body = arguments && arguments.size() > 1? arguments[1] : null
      def unwrappedParams = unwrapParams(params)
      
      if (log.isDebugEnabled()) {
	log.debug("exec(): unwrappedParams " + unwrappedParams + "; body " + body)
      }

      def result = null
      if (closure.getMaximumNumberOfParameters() == 1 && !body) {
	result = closure(unwrappedParams)
      } else {
	result = closure(unwrappedParams) {
          if (body) {
	    log.debug("execBody(): " + tagLibName + "." + tagName)
	    body
          }
	}
      }
      writer.close()
      output.close()
      
      log.debug("exec(): tag executed")

      def textResult = encoding? output.toString(encoding) : output.toString()

      if (log.isDebugEnabled()) {
	log.debug("exec(): result " + result)
	log.debug("exec(): textResult " + textResult)
      }

      def returnsObject = doesReturnObject(env)
      if (!returnsObject) {
	result = textResult
      }

      if (log.isDebugEnabled()) {
	log.debug("exec(): result " + result)
      }

      def objectWrapper = env.getObjectWrapper()
      return objectWrapper.wrap(result? result: "")
    } catch (Exception e) {
      if (! (e instanceof TemplateException)) {
	e = new TemplateModelException(e)
      }
      throw e
    } finally {
      if (restoreOut) {
	restoreOutput()
      }
    }
  }


}