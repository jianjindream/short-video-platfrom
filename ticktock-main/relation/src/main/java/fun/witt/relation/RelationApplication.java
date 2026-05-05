package fun.witt.relation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;
import tk.mybatis.spring.annotation.MapperScan;

@MapperScan(basePackages = "fun.witt.mapper")
@EnableFeignClients(basePackages = {"fun.witt.api.feign"})
@EnableDiscoveryClient
@EnableKafka
@EnableScheduling
@SpringBootApplication(scanBasePackages = "fun.witt")
public class RelationApplication {

    public static void main(String[] args) {
        SpringApplication.run(RelationApplication.class, args);
    }

}
