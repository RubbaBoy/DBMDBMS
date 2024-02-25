package thon.hack.dbmdbms;

import it.corsinvest.proxmoxve.api.PveClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@SpringBootApplication
@EnableAsync
public class DbmdbmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(DbmdbmsApplication.class, args);
    }

    /**
     * Defines a singleton {@link Executor} for Spring threading.
     *
     * @return The created {@link Executor}
     */
    @Bean
    public Executor taskExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(7); // mike said this should be 7
        executor.setMaxPoolSize(Integer.MAX_VALUE);
        executor.setQueueCapacity(Integer.MAX_VALUE);
        executor.setThreadNamePrefix("Playback-");
        executor.initialize();
        return executor;
    }

}
