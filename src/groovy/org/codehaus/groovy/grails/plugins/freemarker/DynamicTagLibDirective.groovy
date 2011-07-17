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

import freemarker.core.Environment
import freemarker.template.TemplateDirectiveBody
import freemarker.template.TemplateDirectiveModel
import freemarker.template.TemplateException
import freemarker.template.TemplateModel
import freemarker.template.TemplateModelException
import grails.util.GrailsUtil
import groovy.lang.GroovyObject

import java.io.IOException
import java.util.Map

import org.apache.commons.logging.Log


/**
 * @author Daniel Henrique Alves Lima
 */
@Deprecated
public class DynamicTagLibDirective extends BaseDynamicTagLibSupport implements TemplateDirectiveModel {

  public DynamicTagLibDirective(String tagLibName, String tagName) {
    super(tagLibName, tagName)
    GrailsUtil.deprecated "${getClass()} is deprecated; Use ${TagLibToDirectiveAndFunction.class} instead"
  }

  public void execute(Environment env,
		      Map params, TemplateModel[] loopVars,
		      TemplateDirectiveBody body)
  throws TemplateException, IOException {
    if (log.isDebugEnabled()) {
      log.debug("execute(): " + tagLibName + "." + tagName)      
      log.debug("execute(): env=" + env + "; params=" + params + "; body=" + body)
    }
    boolean restoreOut = false
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
      
      logCurrentOutput()
      if (log.isDebugEnabled()) {
	log.debug("execute(): out = " + env.getOut())
      }
      tagLib.setProperty('out', env.getOut());
      //tagLib.out = env.getOut()
      restoreOut = true
      logCurrentOutput()
      
      def unwrappedParams = unwrapParams(params)
      
      if (log.isDebugEnabled()) {
	log.debug("execute(): unwrappedParams " + unwrappedParams)
      }

      def result = null
      if (closure.getMaximumNumberOfParameters() == 1) {
	result = closure(unwrappedParams)
      } else {
	result = closure(unwrappedParams) {
	  it ->
	    def oldItVariable = null
	    def objectWrapper = null
	    if (it) {
	      objectWrapper = env.getObjectWrapper()
	      oldItVariable = env.getVariable("it")
	      env.setVariable("it", it?objectWrapper.wrap(it): null)
	    }
	    try {
	      log.debug("executeBody(): " + tagLibName + "." + tagName)
	      if (log.isDebugEnabled()) {
		log.debug("executeBody(): it " + it)
	      }
	      String nestedResult = null
	      if (body) {
		//StreamCharBuffer charBuffer = new StreamCharBuffer()
		//def writer = charBuffer.getWriter()
		String encoding = getOutputEncoding(env)
		ByteArrayOutputStream nestedOut = new ByteArrayOutputStream()
		def nestedWriter = encoding ? new OutputStreamWriter(nestedOut, encoding) : new OutputStreamWriter(nestedOut)
		body.render(nestedWriter)
		//writer.flush()
		//return charBuffer

		nestedWriter.close()
		return encoding? nestedOut.toString(encoding) : nestedOut.toString()
	      } /*else {
		throw new TemplateException("missing body", env)
		}*/
	      

	      return ""
	      //return charBuffer
	    } finally {
	      if (it) {
		env.setVariable("it", oldItVariable)
	      }
	      
	      // Restore the previous output
	      //if (log.isDebugEnabled()) {
	      //	log.debug("execute(): restored out = " + env.getOut())
	      //}
	      //tagLib.setProperty(TagLibDynamicMethods.OUT_PROPERTY, env.getOut());
	      //tagLib.out = env.getOut()
	      /*if (restoreOut) {
		restoreOutput()
		}*/
	      
	    }
	}
      }

      log.debug("execute(): tag executed")

      if (log.isDebugEnabled()) {
	log.debug("execute(): result " + result)
      }

      def returnsObject = doesReturnObject(env)
      if (returnsObject && result) {
	env.getOut() << result
      }
    } catch (Exception e) {
      if (!(e instanceof TemplateException)) {
	throw new TemplateModelException(e)
      }
    } finally {
      if (restoreOut) {
	restoreOutput()
      }
    }
    
  }

}