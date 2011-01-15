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

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import org.springframework.web.servlet.View
import org.springframework.web.servlet.view.AbstractUrlBasedView
import org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver

import org.springframework.grails.freemarker.GrailsFreeMarkerViewResolver

/**
 * @author Daniel Henrique Alves Lima
 */
public class DynamicTagLibViewResolver /*extends GrailsFreeMarkerViewResolver*/ extends FreeMarkerViewResolver {

  private final Log log = LogFactory.getLog(getClass())
  private final Log errorLog = LogFactory.getLog(getClass().getName() + ".ERROR")

  private AutoConfigHelper helper = null

  public DynamicTagLibViewResolver() {
    log.debug("constructor()")
    setViewClass(DynamicTagLibView.class)
  }
  
  @Override
  protected View loadView(String viewName, Locale locale) {
    if (log.isDebugEnabled()) {
      log.debug("loadView(): viewName " + viewName + ", locale " + locale)
    }
    def view = null
    try {
      view = super.loadView(viewName, locale)
    } catch(e) {
      // return null if an exception occurs so the rest of the view
      // resolver chain gets an opportunity to  generate a View
      errorLog.debug("loadView()", e)
    }

    if (log.isDebugEnabled()) {
      log.debug("loadView(): view " + view)
    }
    return view
  }


  @Override
  protected AbstractUrlBasedView buildView(String viewName) throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("buildView(): viewName " + viewName)
    }

    def view = super.buildView(viewName)
    if (view) {
      view.setAutoConfigHelper(helper)
    }

    return view
  }


  @Override
  public void setPrefix(String prefix) {
    if (log.isDebugEnabled()) {
      log.debug("setPrefix(): prefix " + prefix)
    }  

    super.setPrefix(prefix)
    //helper = new AutoConfigHelper(prefix)
  }


  @Override
  public void setSuffix(String suffix) {
    if (log.isDebugEnabled()) {
      log.debug("setSuffix(): suffix " + suffix)
    }  

    super.setSuffix(suffix)
    helper = new AutoConfigHelper(suffix)
  }

}