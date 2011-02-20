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

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer

/**
 * @author Daniel Henrique Alves Lima
 */
class DynamicTagLibConfigurer extends FreeMarkerConfigurer {

  private final Log log = LogFactory.getLog(getClass())

  private String suffix = null

  private AutoConfigHelper helper = null

  @Override
  public void setConfiguration(Configuration configuration) {
    if (log.isDebugEnabled()) {
      log.debug("setConfiguration(): configuration " + configuration)
    }
    this.helper.autoConfigure(false, configuration)
    super.setConfiguration(configuration)
  }

  @Override
  public Configuration getConfiguration() {
    Configuration configuration = super.getConfiguration()
    if (log.isDebugEnabled()) {
      log.debug("getConfiguration(): configuration " + configuration)
    }
    this.helper.autoConfigure(false, configuration)
    return configuration
  }
  
  public void setSuffix(String suffix) {
    if (log.isDebugEnabled()) {
      log.debug("setSuffix(): suffix " + suffix)
    }    
    this.suffix = suffix
    this.helper = new AutoConfigHelper(suffix)
  }

}