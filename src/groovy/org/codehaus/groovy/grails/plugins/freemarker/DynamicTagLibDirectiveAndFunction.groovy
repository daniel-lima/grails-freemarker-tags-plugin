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

import java.io.IOException
import java.util.List
import java.util.Map

import freemarker.core.Environment
import freemarker.template.TemplateDirectiveBody
import freemarker.template.TemplateDirectiveModel
import freemarker.template.TemplateException
import freemarker.template.TemplateMethodModelEx
import freemarker.template.TemplateModel
import freemarker.template.TemplateModelException
import grails.util.GrailsUtil

/**
 * @author Daniel Henrique Alves Lima
 */
@Deprecated
public class DynamicTagLibDirectiveAndFunction implements TemplateDirectiveModel, TemplateMethodModelEx {

  private DynamicTagLibDirective directiveImpl
  private DynamicTagLibFunction functionImpl

  public DynamicTagLibDirectiveAndFunction(String tagLibName, String tagName) {
      GrailsUtil.deprecated "${getClass()} is deprecated; Use ${TagLibToDirectiveAndFunction.class} instead"
    this.directiveImpl = new DynamicTagLibDirective(tagLibName, tagName)
    this.functionImpl = new DynamicTagLibFunction(tagLibName, tagName)
  }
  
  public void execute(Environment env,
		      Map params, TemplateModel[] loopVars,
		      TemplateDirectiveBody body)
  throws TemplateException, IOException {
    this.directiveImpl.execute(env, params, loopVars, body)
  }

  
  public Object exec(List arguments) throws TemplateModelException {
    this.functionImpl.exec(arguments)
  }

}