package com.rkos.modules.story.repository;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.stereotype.Component;

/**
 * 测试配置：解决 {@code @DataMongoTest} 上下文中 {@code @MapperScan}
 * 注册 MyBatis-Plus Mapper Bean 导致缺少 DataSource 的问题。
 * <p>
 * 实现 {@link PriorityOrdered} 确保在所有 BeanDefinition 注册完成后
 * （包括 {@code @MapperScan} 注册的 MapperFactoryBean）才执行清理。
 */
@Component
class MongoTestMapperFixConfig implements BeanDefinitionRegistryPostProcessor, PriorityOrdered {

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        // 最高优先级，确保在 MapperScannerConfigurer 之后执行
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (beanFactory instanceof BeanDefinitionRegistry registry) {
            for (String name : registry.getBeanDefinitionNames()) {
                String beanClassName = registry.getBeanDefinition(name).getBeanClassName();
                if (beanClassName != null && beanClassName.contains("MapperFactoryBean")) {
                    registry.removeBeanDefinition(name);
                }
            }
        }
    }
}
