/**
 * Copyright (C) 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ninja.servlet;

import javax.servlet.ServletContextEvent;

import ninja.utils.NinjaProperties;
import ninja.utils.NinjaPropertiesImpl;

import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;
import ninja.utils.NinjaMode;
import ninja.utils.NinjaModeHelper;

/**
 * define in web.xml:
 * 
 * <listener>
 *   <listener-class>ninja.NinjaServletListener</listener-class>
 * </listener>
 *  
 * @author zoza
 * 
 */
public class NinjaServletListener extends GuiceServletContextListener {
    
    private volatile NinjaBootstrap ninjaBootstrap;

    NinjaPropertiesImpl ninjaProperties = null;
    
    String contextPath;

    public synchronized void setNinjaProperties(NinjaPropertiesImpl ninjaPropertiesImpl) {
        
        if (this.ninjaProperties != null) {
            
            throw new IllegalStateException("NinjaProperties already set.");
        
        } else {
        
            this.ninjaProperties = ninjaPropertiesImpl;
            
        }
        
    }

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) { 
        contextPath = servletContextEvent.getServletContext().getContextPath();
        super.contextInitialized(servletContextEvent);
    }
   
    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        ninjaBootstrap.shutdown();
        super.contextDestroyed(servletContextEvent);
    }
   
    /**
     * Getting the injector is done via double locking in conjuction
     * with volatile keyword for thread safety.
     * See also: http://en.wikipedia.org/wiki/Double-checked_locking
     * 
     * @return The injector for this application.
     */
    @Override
    public Injector getInjector() {
        
        // fetch instance variable into method, so that we access the volatile
        // global variable only once - that's better performance wise.
        NinjaBootstrap ninjaBootstapLocal = ninjaBootstrap;
        
        if (ninjaBootstapLocal == null) {
        
            synchronized(this) {
                
                ninjaBootstapLocal = ninjaBootstrap;
                
                if (ninjaBootstapLocal == null) {
                    
                    // if properties 
                    if (ninjaProperties == null) {
                        
                        ninjaProperties 
                                = new NinjaPropertiesImpl(
                                        NinjaModeHelper.determineModeFromSystemPropertiesOrProdIfNotSet());
                    
                    }
                
                    ninjaBootstrap 
                            = createNinjaBootstrap(ninjaProperties, contextPath);
                    ninjaBootstapLocal = ninjaBootstrap;

                }
            
            }
        
        }
        
        return ninjaBootstapLocal.getInjector();

    }
    
    
    
    private NinjaBootstrap createNinjaBootstrap(
        NinjaPropertiesImpl ninjaProperties,
        String contextPath) {
    
         // we set the contextpath.
        ninjaProperties.setContextPath(contextPath);
        
        ninjaBootstrap = new NinjaBootstrap(ninjaProperties);
        
        ninjaBootstrap.boot();
        
        return ninjaBootstrap;

    }

}
