package org.bllose;

import javassist.*;
import org.apache.commons.lang3.StringUtils;
import org.bllose.content.Constants;
import org.bllose.tools.AnnotationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

public class MyClassTransformer implements ClassFileTransformer {
    private static final Logger log = LoggerFactory.getLogger(MyClassTransformer.class);
    MyClassTransformer() {
        super();
        log.warn("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        log.warn("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        log.warn("++++++++++++++++++Transformer has started!++++++++++++++++++++++++");
        log.warn("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        log.warn("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");

        String param = System.getenv(Constants.XXLJOB_ENABLED);
        if(!StringUtils.isBlank(param) && param.length() == 4) {
            XXLJOB_REMOVE = Boolean.valueOf(param);
        }

        param = System.getenv(Constants.RABBITMQ_ENABLED);
        if(!StringUtils.isBlank(param) && param.length() == 4) {
            RABBIT_LISTENER_REMOVE = Boolean.valueOf(param);
        }

        param = System.getenv(Constants.KAFKAMQ_ENABLED);
        if(!StringUtils.isBlank(param) && param.length() == 4) {
            KAFKA_REMOVE = Boolean.valueOf(param);
        }

        param = System.getenv(Constants.TARGET_PATH_ROOT);
        if(StringUtils.isBlank(param)) {
            PATH_ROOT = "com/dycjr";
        } else {
            PATH_ROOT = param;
        }
        log.info("加载工作根目录: {}", PATH_ROOT);
    }

    private static Boolean XXLJOB_REMOVE = true;
    private static Boolean RABBIT_LISTENER_REMOVE = true;
    private static Boolean KAFKA_REMOVE = true;
    private static String PATH_ROOT;

    @Override
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) throws IllegalClassFormatException {
        if(!className.startsWith(PATH_ROOT)) return new byte[0];
        boolean isChanged = false;

        try {
            ClassPool pool = ClassPool.getDefault();
            InputStream inputStream = new ByteArrayInputStream(classfileBuffer);
            CtClass cc = pool.makeClass(inputStream);
            CtMethod[] methods = cc.getMethods();
            for (CtMethod method : methods) {
                if(XXLJOB_REMOVE && AnnotationUtil.anyContains(method, Constants.ANNOTATION_XXLJOB)){
                    isChanged |= AnnotationUtil.removeAnnotations(method, Constants.ANNOTATION_XXLJOB);
                }
                if(RABBIT_LISTENER_REMOVE && AnnotationUtil.anyContains(method, Constants.ANNOTATION_RABBIT_LISTENER)){
                    isChanged |= AnnotationUtil.removeAnnotations(method, Constants.ANNOTATION_RABBIT_LISTENER);
                }
                if(KAFKA_REMOVE && AnnotationUtil.anyContains(method, Constants.ANNOTATION_KAFKA_LISTENER)) {
                    isChanged |= AnnotationUtil.removeAnnotations(method, Constants.ANNOTATION_KAFKA_LISTENER);
                }
            }

            if(isChanged) return cc.toBytecode();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (CannotCompileException e) {
            throw new RuntimeException(e);
        }

        return null;
    }
}
