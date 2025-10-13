package ru.test.the.best.chat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import ru.test.the.best.chat.core.gson.adapter.InstantTypeAdapter;

import java.time.Duration;
import java.time.Instant;

@SpringBootApplication
public class TheBestChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(TheBestChatApplication.class, args);
    }

    @Bean
    Gson gson() {
        return new GsonBuilder()
                .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .create();
    }

    @Bean
    JedisPool jedisPool(
            @Value("${redis.max.total}") final int redisMaxTotal,
            @Value("${redis.max.idle}") final int redisMaxIdle,
            @Value("${redis.min.idle}") final int redisMinIdle,
            @Value("${redis.block.when.exhausted}") final boolean redisBlockWhenExhausted,
            @Value("${redis.test.on.borrow}") final boolean redisTestOnBorrow,
            @Value("${redis.test.on.return}") final boolean redisTestOnReturn,
            @Value("${redis.test.while.idle}") final boolean redisTestWhileIdle,
            @Value("${redis.min.evictable.idle.time}") final int minEvictableIdleTime,
            @Value("${redis.time.between.eviction.runs}") final int timeBetweenEvictionRuns,
            @Value("${redis.num.tests.per.eviction.run}") final int numTestsPerEvictionRun,
            @Value("${redis.host}") final String host,
            @Value("${redis.port}") final int port,
            @Value("${redis.password}") final String password,
            @Value("${redis.username}") final String username

    ) {
        final var poolConfig = new JedisPoolConfig();

        poolConfig.setJmxEnabled(false);

        // === Основные параметры размера пула ===

        /**
         * Максимальное общее количество соединений (активных + простаивающих),
         * которые могут быть созданы пулом. -1 означает без ограничений.
         * Это важный параметр для контроля нагрузки на Redis.
         */
        poolConfig.setMaxTotal(redisMaxTotal);

        /**
         * Максимальное количество "простаивающих" (неиспользуемых) соединений,
         * которые могут находиться в пуле. Если активных соединений мало,
         * лишние простаивающие будут закрыты.
         */
        poolConfig.setMaxIdle(redisMaxIdle);

        /**
         * Минимальное количество "простаивающих" соединений, которые пул будет
         * стараться поддерживать. Это позволяет быстро обслуживать всплески нагрузки,
         * т.к. не нужно тратить время на создание новых соединений.
         */
        poolConfig.setMinIdle(redisMinIdle);


        // === Параметры поведения при исчерпании пула ===

        /**
         * Определяет, будет ли поток блокироваться и ждать, когда в пуле закончатся
         * свободные соединения.
         * true (рекомендуется): Поток ждет, пока соединение не освободится (время ожидания см. maxWaitMillis).
         * false: Пул немедленно бросит исключение JedisExhaustedPoolException.
         */
        poolConfig.setBlockWhenExhausted(redisBlockWhenExhausted);


        // === Параметры проверки работоспособности соединений ===

        /**
         * Проверять ли соединение командой PING перед тем, как выдать его из пула.
         * Помогает избежать получения "мертвых" соединений, которые были закрыты
         * со стороны сервера или из-за проблем с сетью. Слегка снижает производительность.
         */
        poolConfig.setTestOnBorrow(redisTestOnBorrow);

        /**
         * Проверять ли соединение командой PING перед тем, как вернуть его в пул.
         * Обычно избыточно, если включен testOnBorrow. По умолчанию false.
         */
        poolConfig.setTestOnReturn(redisTestOnReturn); // Изменено на false для оптимизации

        /**
         * Проверять ли "простаивающие" соединения в фоновом режиме специальным потоком (evictor).
         * Это основной способ поддержания пула в "здоровом" состоянии без влияния на производительность запросов.
         */
        poolConfig.setTestWhileIdle(redisTestWhileIdle);


        // === Параметры работы фонового потока очистки (Evictor) ===
        // Эти параметры имеют смысл только если testWhileIdle = true.

        /**
         * Минимальное время, которое соединение должно простаивать,
         * прежде чем оно может быть вытеснено (удалено) из пула сборщиком мусора (evictor).
         * Помогает закрывать лишние соединения, если нагрузка спала.
         */
        poolConfig.setMinEvictableIdleTime(Duration.ofSeconds(minEvictableIdleTime));

        /**
         * Интервал времени между запусками фонового потока (evictor),
         * который проверяет и удаляет "протухшие" или лишние простаивающие соединения.
         */
        poolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(timeBetweenEvictionRuns));

        /**
         * Количество соединений, проверяемых за один запуск потока-сборщика (evictor).
         * -1 означает, что проверяются все простаивающие соединения.
         * Установка небольшого положительного числа распределяет нагрузку от проверок во времени.
         */
        poolConfig.setNumTestsPerEvictionRun(numTestsPerEvictionRun);

        return new JedisPool(poolConfig, host, port, username, password);
    }
}
