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
import freemarker.template.Template
import grails.test.*


/**
 * @author Daniel Henrique Alves Lima
 */
class TagLibAwareConfigurerTests extends GroovyTestCase {

    def freemarkerConfig
    private StringWriter sWriter = new StringWriter()
    private Exception threadException
    private ThreadGroup myThreadGroup = new ThreadGroup('x') {public void uncaughtException(Thread t,Throwable e) {super.uncaughtException(t, e); threadException = e}}
    
    protected void setUp() {
        super.setUp(); threadException = null
    }

    protected void tearDown() {
        super.tearDown()
    }

    void testConfigReference() {
        assertNotNull freemarkerConfig
        assertTrue freemarkerConfig instanceof AbstractTagLibAwareConfigurer        
    }
    
    void testParseRegularTemplate() {
        String result = parseFtlTemplate('[#ftl/]${s}', [s: 'ok']);
        assertEquals 'ok', result
        
        result = parseFtlTemplate('<#ftl/>${s}', [s: 'fail']);
        assertEquals 'fail', result
    }
    
    void testParseFmTagsTemplate() {
        String result = parseFtlTemplate('[#ftl/][@g.form /]');
        assertTrue result, result.contains('<form')
        assertTrue result, result.contains('</form>')
        
        result = parseFtlTemplate('[#ftl/]<a href="${g.message({\'code\': \'abc\', \'default\': \'xyz\'})}">');
        assertEquals '<a href="xyz">', result
        
    }
    
    
    
    void testParseFmTagsTemplateWithoutRequestContext() {
        String result = null
        def x = {
            result = parseFtlTemplate('[#ftl/][@g.form /]');
            assertTrue result, result.contains('<form')
            assertTrue result, result.contains('</form>')

            result = parseFtlTemplate('[#ftl/]<a href="${g.message({\'code\': \'abc\', \'default\': \'xyz\'})}">');
            assertEquals '<a href="xyz">', result
        } as Runnable

        x = new Thread(myThreadGroup, x); x.daemon = true; x.start()
        while (x.isAlive()) {
            Thread.sleep 1000
        }

        if (threadException) throw threadException
    }
    
    private parseFtlTemplate = {String templateSourceCode, Map binding = [:] ->
        if (sWriter.buffer.length() > 0) {sWriter.buffer.delete 0, sWriter.buffer.length()}
        Configuration cfg = freemarkerConfig.configuration
        Template template = new Template('template', new StringReader(templateSourceCode), cfg)
        template.process (binding, sWriter)
        return sWriter.toString()
    }
    
}
