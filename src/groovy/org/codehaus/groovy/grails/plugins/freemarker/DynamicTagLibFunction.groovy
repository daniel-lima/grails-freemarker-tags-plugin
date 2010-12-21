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
      
      String encoding = env.getOutputEncoding()
      if (!encoding) {
	def config = env.getConfiguration()
	//encoding = config.getEncoding(null)
	//if (!encoding) {
	encoding = config.getDefaultEncoding()
	//}
      }
      
      ByteArrayOutputStream output = new ByteArrayOutputStream()
      def writer = encoding ? new OutputStreamWriter(output, encoding) : new OutputStreamWriter(output)
      tagLib.setProperty(TagLibDynamicMethods.OUT_PROPERTY, writer);
      
      def params = arguments && arguments.size() > 0? arguments[0] : [:]
      def unwrappedParams = unwrapParams(params)
      
      if (log.isDebugEnabled()) {
	log.debug("exec(): unwrappedParams " + unwrappedParams)
      }

      def result = closure(unwrappedParams)
      writer.close()
      output.close()
      
      log.debug("exec(): tag executed")

      def textResult = encoding? output.toString(encoding) : output.toString()
      if (result && !(result instanceof String)) {
	result = null
      }

      if (log.isDebugEnabled()) {
	log.debug("exec(): result " + result)
	log.debug("exec(): textResult " + textResult)
      }

      if (result) {
	result = textResult + result
      } else {
	result = textResult
      }

      if (log.isDebugEnabled()) {
	log.debug("exec(): result " + result)
      }

      def objectWrapper = env.getObjectWrapper()
      return objectWrapper.wrap(result)
    } catch (Exception e) {
      if (! (e instanceof TemplateException)) {
	e = new TemplateModelException(e)
      }
      throw e
    }
  }


}