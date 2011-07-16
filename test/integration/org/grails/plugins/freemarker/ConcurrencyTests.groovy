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
package org.grails.plugins.freemarker

import freemarker.template.Configuration
import freemarker.template.Template
import grails.test.*


/**
 * @author Daniel Henrique Alves Lima
 */
class ConcurrencyTests extends GroovyTestCase {

    def freemarkerConfig
    private Exception threadException
    private ThreadGroup myThreadGroup = new ThreadGroup('x') {public void uncaughtException(Thread t,Throwable e) {super.uncaughtException(t, e); threadException = e}}
    
    protected void setUp() {
        super.setUp(); threadException = null
    }

    protected void tearDown() {
        super.tearDown()
    }    
    
    void testConcurrency() {
        def text = new StringBuilder('[#ftl/]')
        for (int i = 0; i < 1000; i++) {
            text.append(i)
            text.append(': [@g.thread name=name /]\n')
        }
        Configuration cfg = freemarkerConfig.configuration
        Template template = new Template('template', new StringReader(text.toString()), cfg)
        text = null
        
        for (int counter = 0; counter < 20; counter++) {
            Thread.currentThread().yield()
            runInParallel {
                StringWriter result = new StringWriter()
                template.process([name: counter + 1], result)
                Thread.currentThread().yield()
                List lines = new StringReader(result.toString()).readLines(); result = null
                assertEquals "thread ${counter} ${lines.size()}", 1000, lines.size()
                lines.eachWithIndex {line, index ->
                    //if (Math.random() <= 0.1) {assertTrue 'random', false}
                    assertEquals "${index}: Thread-${counter+1}".toString(), line 
                }
            }
            Thread.currentThread().yield()
        }
        
    }
    
    private runInParallel = {Closure c ->
        if (threadException != null) {throw threadException}
        def thread = null; boolean executed = false 
        Closure c1 = {
            try {
                c()
            } finally {
                executed = true
            }
        }
        
        thread = new Thread(myThreadGroup, c1 as Runnable); thread.daemon = true; thread.start(); thread.yield()
        while (thread.isAlive() && !executed) {
            Thread.sleep 1000
        }
        if (threadException != null) {throw threadException}
    }
    
}
