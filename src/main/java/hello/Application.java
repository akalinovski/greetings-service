package hello;

import com.uber.jaeger.Configuration;
import com.uber.jaeger.samplers.ConstSampler;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import java.util.Arrays;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
        return args -> {

            System.out.println("Let's inspect the beans provided by Spring Boot:");

            String[] beanNames = ctx.getBeanDefinitionNames();
            Arrays.sort(beanNames);
            for (String beanName : beanNames) {
                System.out.println(beanName);
            }

        };
    }

    @Bean
    public io.opentracing.Tracer jaegerTracer() {
        return new Configuration("spring-boot",
                new Configuration.SamplerConfiguration(ConstSampler.TYPE, 1),
                new Configuration.ReporterConfiguration(true,
                        "jaeger-agent.kube-system.svc.cluster.local", 5775, 1000, 1000))
                .getTracer();
    }

}
