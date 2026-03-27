package com.example.sftpwatcher.config;

import java.time.Clock;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

@Configuration
public class SftpFrameworkConfiguration {

    @Bean
    public SchedulerFactoryBeanCustomizer schedulerFactoryBeanCustomizer(ApplicationContext applicationContext) {
        return factory -> factory.setJobFactory(new AutowiringQuartzJobFactory(applicationContext.getAutowireCapableBeanFactory()));
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    private static final class AutowiringQuartzJobFactory extends SpringBeanJobFactory {
        private final AutowireCapableBeanFactory beanFactory;

        private AutowiringQuartzJobFactory(AutowireCapableBeanFactory beanFactory) {
            this.beanFactory = beanFactory;
        }

        @Override
        protected Object createJobInstance(org.quartz.spi.TriggerFiredBundle bundle) throws Exception {
            Object job = super.createJobInstance(bundle);
            beanFactory.autowireBean(job);
            return job;
        }
    }
}
