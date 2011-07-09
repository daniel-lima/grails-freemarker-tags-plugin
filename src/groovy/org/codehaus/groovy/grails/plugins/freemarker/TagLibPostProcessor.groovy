package org.codehaus.groovy.grails.plugins.freemarker

import org.codehaus.groovy.grails.commons.ArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.TagLibArtefactHandler;
import org.springframework.beans.BeansException
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter

class TagLibPostProcessor extends InstantiationAwareBeanPostProcessorAdapter implements InstantiationAwareBeanPostProcessor {

    GrailsApplication grailsApplication
    
    @Override
    public boolean postProcessAfterInstantiation(Object bean, String beanName)
            throws BeansException {
        
        ArtefactHandler tagLibHandler = grailsApplication.getArtefactHandler(TagLibArtefactHandler.TYPE)
        if (tagLibHandler.isArtefact(bean.class) && beanName.endsWith('_fm')) {
            
            MetaClass mc = bean.metaClass
            mc.getOut = {->
                return System.out
            }
            
            
        }
                
        return true
    }

    

}
