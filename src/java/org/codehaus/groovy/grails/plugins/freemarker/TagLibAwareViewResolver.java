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
package org.codehaus.groovy.grails.plugins.freemarker;

import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.AbstractUrlBasedView;
import org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver;

public class TagLibAwareViewResolver extends FreeMarkerViewResolver {

    private final Log log = LogFactory.getLog(getClass());
    private final Log errorLog = LogFactory.getLog(getClass().getName()
            + ".ERROR");

    public TagLibAwareViewResolver() {
        log.debug("constructor()");
        setViewClass(DynamicTagLibView.class);
    }

    @Override
    protected View loadView(String viewName, Locale locale) {
        if (log.isDebugEnabled()) {
            log.debug("loadView(): viewName " + viewName + ", locale " + locale);
        }
        View view = null;
        try {
            view = super.loadView(viewName, locale);
        } catch (Exception e) {
            boolean hideException = false;
            try {
                // hideException = helper &&
                // helper.grailsConfig.viewResolver.legacyHideExceptions
            } catch (Exception ne) {
            }

            if (hideException) {
                // return null if an exception occurs so the rest of the view
                // resolver chain gets an opportunity to generate a View
                errorLog.debug("loadView()", e);
            } else {
                errorLog.error("loadView()", e);
                throw new RuntimeException(e);
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("loadView(): view " + view);
        }
        return view;
    }

    @Override
    protected AbstractUrlBasedView buildView(String viewName) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("buildView(): viewName " + viewName);
        }

        AbstractUrlBasedView view = super.buildView(viewName);

        return view;
    }

}
