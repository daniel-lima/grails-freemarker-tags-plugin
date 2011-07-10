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
import org.springframework.grails.freemarker.GrailsFreeMarkerView

import freemarker.template.Configuration
import grails.util.GrailsUtil

/**
 * @author Daniel Henrique Alves Lima
 */
@Deprecated
public class DynamicTagLibView extends GrailsFreeMarkerView {

  private final Log log = LogFactory.getLog(getClass())

  public DynamicTagLibView() {
      GrailsUtil.deprecated "${getClass()} is deprecated; Use ${TagLibAwareView.class} instead"
    log.debug("constructor()")
  }

  @Override
  public void setConfiguration(Configuration configuration) {
    if (log.isDebugEnabled()) {
      log.debug("setConfiguration(): configuration " + configuration)
    }

    Boolean isConfigured = configuration.getCustomAttribute(AutoConfigHelper.CONFIGURED_ATTRIBUTE_NAME)
    if (!isConfigured) {
      def message = "FreeMarker Tags configuration is missing: A " + DynamicTagLibConfigurer.class.simpleName + 
      " bean should be defined or " + AutoConfigHelper.class.simpleName + ".autoConfigure() should be called manually."
      log.error("setConfiguration(): " + message)
      throw new RuntimeException(message)
    }

    super.setConfiguration(configuration)
  }

}