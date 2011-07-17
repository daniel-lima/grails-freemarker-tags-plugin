/*
 * Copyright 2011 the original author or authors.
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
package org.grails.plugins.freemarker

import freemarker.template.Configuration
import freemarker.template.Template
import grails.test.*


/**
 * @author Daniel Henrique Alves Lima
 */
class TagLibAwareViewResolverTests extends GroovyTestCase {

    def grailsApplication
    def freemarkerViewResolver

    void testViewResolverReference() {
        assertNotNull freemarkerViewResolver
        assertTrue freemarkerViewResolver instanceof TagLibAwareViewResolver
    }

    void testFreemarkerTagsConfig() {
        assertEquals false, freemarkerViewResolver.isHideException()
        println "grailsApplication.freemarkerTagsConfig ${grailsApplication.freemarkerTagsConfig}"
        try {
            freemarkerViewResolver.@hideException = null
            grailsApplication.freemarkerTagsConfig.viewResolver.legacyHideExceptions = Boolean.TRUE
            println "grailsApplication.freemarkerTagsConfig ${grailsApplication.freemarkerTagsConfig}"
            assertEquals true, freemarkerViewResolver.isHideException()
        } finally {
            grailsApplication.freemarkerTagsConfig.viewResolver.legacyHideExceptions = Boolean.FALSE
        }
    }
}
