/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.config.spring.extension;

import java.util.Set;
import org.apache.dubbo.common.extension.ExtensionFactory;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.context.ApplicationContext;

/**
 * SpringExtensionFactory
 *
 * 例子
 *public class DemoFilter implements Filter {
 *
 *     private DemoDAO demoDAO;
 *
 *     @Override
 *     public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
 *         return invoker.invoke(invocation);
 *     }
 *
 *     public DemoFilter setDemoDAO(DemoDAO demoDAO) {
 *         this.demoDAO = demoDAO;
 *         return this;
 *     }
 * }
 *
 * 在 「4.4.3 injectExtension」 中，会调用 #setDemoDAO(demo) 方法，将 DemoFilter 依赖的属性 demoDAO 注入
 *
 *
 *
 */
public class SpringExtensionFactory implements ExtensionFactory {
    private static final Logger logger = LoggerFactory.getLogger(SpringExtensionFactory.class);

    private static final Set<ApplicationContext> contexts = new ConcurrentHashSet<ApplicationContext>();

    public static void addApplicationContext(ApplicationContext context) {
        contexts.add(context);
    }

    public static void removeApplicationContext(ApplicationContext context) {
        contexts.remove(context);
    }

    // currently for test purpose
    public static void clearContexts() {
        contexts.clear();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getExtension(Class<T> type, String name) {

        //SPI should be get from SpiExtensionFactory
        if (type.isInterface() && type.isAnnotationPresent(SPI.class)) {
            return null;
        }

        // 从Spring容器中按照名字进行Bean查询
        for (ApplicationContext context : contexts) {
            if (context.containsBean(name)) {
                Object bean = context.getBean(name);
                if (type.isInstance(bean)) {
                    return (T) bean;
                }
            }
        }

        logger.warn("No spring extension(bean) named:" + name + ", try to find an extension(bean) of type " + type.getName());

        // 如果按照名字没有找到，则按照类型查找
        for (ApplicationContext context : contexts) {
            try {
                return context.getBean(type);
             // 多个Bean
            } catch (NoUniqueBeanDefinitionException multiBeanExe) {
                throw multiBeanExe;
             // 没有找到
            } catch (NoSuchBeanDefinitionException noBeanExe) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Error when get spring extension(bean) for type:" + type.getName(), noBeanExe);
                }
            }
        }

        logger.warn("No spring extension(bean) named:" + name + ", type:" + type.getName() + " found, stop get bean.");

        return null;
    }

}
