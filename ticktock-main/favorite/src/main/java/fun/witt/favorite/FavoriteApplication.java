package fun.witt.favorite;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;
import tk.mybatis.spring.annotation.MapperScan;

@MapperScan(basePackages = "fun.witt.mapper")
@EnableFeignClients(basePackages = {"fun.witt.api.feign"})
@EnableDiscoveryClient
@EnableScheduling
@SpringBootApplication(scanBasePackages = "fun.witt")
public class FavoriteApplication {

	public static void main(String[] args) {
		SpringApplication.run(FavoriteApplication.class, args);
	}

}
