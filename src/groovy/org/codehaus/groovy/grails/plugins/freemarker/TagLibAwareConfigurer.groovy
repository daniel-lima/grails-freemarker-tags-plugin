package org.codehaus.groovy.grails.plugins.freemarker

import groovy.lang.GroovyObject;

import org.springframework.context.ApplicationContext;

class TagLibAwareConfigurer extends AbstractTagLibAwareConfigurer {

    @Override
    protected GroovyObject getTagLibInstance(ApplicationContext springContext,
            String className) {
        return springContext."${className}_fm"
    }

}
