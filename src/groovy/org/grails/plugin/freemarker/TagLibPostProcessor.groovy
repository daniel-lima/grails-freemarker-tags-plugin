package org.grails.plugin.freemarker

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
            
            ThreadLocal<Deque> outStack = new ThreadLocal<Deque>()
            def _getX = {
                Deque<Object> d = outStack.get()
                if (d == null) {
                    d = new LinkedList<Object>()
                    outStack.set(d)
                }
                
                return d
            }
            
            
            MetaClass mc = bean.metaClass
            mc.getOut = {->
                Deque d = _getX()
                return (d.size() > 0)?d.last : null
            }
            
            mc.popOut = {->
                return _getX().removeLast()
            }
            
            mc.pushOut = {out ->
                _getX().addLast(out) 
            }
            
            
        }
                
        return true
    }

    

}
