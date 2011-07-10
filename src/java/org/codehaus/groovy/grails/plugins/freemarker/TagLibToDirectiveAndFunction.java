package org.codehaus.groovy.grails.plugins.freemarker;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.web.taglib.GroovyPageAttributes;

import freemarker.core.Environment;
import freemarker.template.ObjectWrapper;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;
import freemarker.template.utility.DeepUnwrap;
import groovy.lang.Closure;
import groovy.lang.GroovyObject;

public class TagLibToDirectiveAndFunction implements TemplateDirectiveModel,
        TemplateMethodModelEx {

    private static final Map<String, String> RESERVED_WORDS_TRANSLATION;
    private final Log log = LogFactory.getLog(getClass());

    static {
        Map<String, String> m = new LinkedHashMap<String, String>();
        m.put("as", "_as");
        RESERVED_WORDS_TRANSLATION = Collections.unmodifiableMap(m);
    }

    private String namespace;
    @SuppressWarnings("unused")
    private GroovyObject tagLibInstance;
    private String tagName;
    private Closure tagInstance;
    private boolean hasReturnValue;

    public TagLibToDirectiveAndFunction(String namespace,
            GroovyObject tagLibInstance, String tagName, Closure tagInstance,
            boolean hasReturnValue) {
        this.namespace = namespace;
        this.tagLibInstance = tagLibInstance;
        this.tagName = tagName;
        this.tagInstance = tagInstance;
        this.hasReturnValue = hasReturnValue;
    }

    @Override
    public Object exec(@SuppressWarnings("rawtypes") List arguments)
            throws TemplateModelException {
        if (log.isDebugEnabled()) {
            log.debug("exec(): @" + namespace + "." + tagName);
        }
        try {
            CharArrayWriter writer = new CharArrayWriter();
            tagInstance.invokeMethod("pushOut", writer);

            Object args = null;
            Object body = null;
            if (arguments != null) {
                if (arguments.size() > 0) {
                    args = arguments.get(0);
                }
                if (arguments.size() > 1) {
                    body = arguments.get(1);
                }
            }

            Object result = null;
            args = args != null ? unwrapParams((TemplateHashModelEx) args,
                    Boolean.TRUE) : unwrapParams(Collections.EMPTY_MAP,
                    Boolean.TRUE);
            if (log.isDebugEnabled()) {
                log.debug("exec(): args " + args);
                log.debug("exec(): body " + body);
            }

            if (tagInstance.getMaximumNumberOfParameters() == 1) {
                result = tagInstance.call(args);
            } else {
                final Object fBody = body;
                @SuppressWarnings("serial")
                Closure bodyClosure = new Closure(this) {

                    @SuppressWarnings("unused")
                    public Object doCall(Object it) throws IOException,
                            TemplateException {
                        return fBody;
                    }

                };

                result = tagInstance.call(new Object[] { args, bodyClosure });
                if (result == null) {
                    // writer.flush();
                    result = writer.toString();
                }
            }

            return result;
        } finally {
            tagInstance.invokeMethod("popOut", null);
        }
    }

    @Override
    public void execute(final Environment env,
            @SuppressWarnings("rawtypes") Map params, TemplateModel[] loopVars,
            final TemplateDirectiveBody body) throws TemplateException,
            IOException {
        if (log.isDebugEnabled()) {
            log.debug("execute(): @" + namespace + "." + tagName);
        }
        try {
            tagInstance.invokeMethod("pushOut", env.getOut());

            params = unwrapParams(params, Boolean.TRUE);
            if (log.isDebugEnabled()) {
                log.debug("execute(): params " + params);
                log.debug("exec(): body " + body);
            }
            Object result = null;
            if (tagInstance.getMaximumNumberOfParameters() == 1) {
                result = tagInstance.call(params);
            } else {
                @SuppressWarnings("serial")
                Closure bodyClosure = new Closure(this) {

                    @SuppressWarnings("unused")
                    public void doCall(Object it) throws IOException,
                            TemplateException {
                        if (body != null) {
                            ObjectWrapper objectWrapper = env
                                    .getObjectWrapper();
                            TemplateModel oldIt = env.getVariable("it");
                            try {
                                if (it != null) {
                                    env.setVariable("it",
                                            objectWrapper.wrap(it));
                                }
                                body.render((Writer) tagInstance
                                        .getProperty("out"));
                            } finally {
                                if (oldIt != null) {
                                    env.setVariable("it", oldIt);
                                }
                            }
                        }
                    }

                };

                result = tagInstance.call(new Object[] { params, bodyClosure });
            }

            if (result != null && hasReturnValue) {
                env.getOut().append(result.toString());
            }
        } finally {
            tagInstance.invokeMethod("popOut", null);
        }

    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected Map unwrapParams(TemplateHashModelEx params,
            Boolean translateReservedWords) throws TemplateModelException {

        if (translateReservedWords == null) {
            translateReservedWords = Boolean.TRUE;
        }

        Map unwrappedParams = new GroovyPageAttributes(new LinkedHashMap());

        TemplateModelIterator keys = params.keys().iterator();
        while (keys.hasNext()) {
            String oldKey = keys.next().toString();
            Object value = params.get(oldKey);
            if (value != null) {
                value = DeepUnwrap.permissiveUnwrap((TemplateModel) value);
            }

            String key = null;
            if (translateReservedWords) {
                key = RESERVED_WORDS_TRANSLATION.get(oldKey);
            }
            if (key == null) {
                key = oldKey;
            }

            unwrappedParams.put(key, value);
        }

        return unwrappedParams;

    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected Map unwrapParams(Map params, Boolean translateReservedWords)
            throws TemplateModelException {
        if (translateReservedWords == null) {
            translateReservedWords = Boolean.TRUE;
        }

        Map unwrappedParams = new GroovyPageAttributes(new LinkedHashMap());
        Iterator<Map.Entry> iterator = params.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry entry = iterator.next();
            Object value = entry.getValue();
            if (value != null) {
                value = DeepUnwrap.permissiveUnwrap((TemplateModel) value);
            }

            String key = null;
            if (translateReservedWords) {
                key = RESERVED_WORDS_TRANSLATION.get(entry.getKey());
            }
            if (key == null) {
                key = (String) entry.getKey();
            }

            unwrappedParams.put(key, value);
        }

        return unwrappedParams;
    }

}
